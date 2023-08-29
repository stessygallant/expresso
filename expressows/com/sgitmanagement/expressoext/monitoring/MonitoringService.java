package com.sgitmanagement.expressoext.monitoring;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sgitmanagement.expresso.base.AbstractBaseEntityService;
import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.exception.ValidationException;
import com.sgitmanagement.expressoext.base.BaseService;
import com.sgitmanagement.expressoext.security.Resource;
import com.sgitmanagement.expressoext.security.ResourceService;
import com.sgitmanagement.expressoext.util.MainUtil;

public class MonitoringService extends BaseService {

	private CloseableHttpClient httpClient;
	private String serverUrl;
	private String path;
	private String userName;
	private String password;

	/**
	 * Create the HTTP client
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private void connect() throws Exception {

		// Timeout after 60 seconds
		int timeoutInMs = 60 * 1000; // ms
		RequestConfig config = RequestConfig.custom().setConnectTimeout(timeoutInMs).setConnectionRequestTimeout(timeoutInMs).setSocketTimeout(timeoutInMs).build();

		// ignore SSL certificate (SSL certificate for Monitoring is not valid)
		SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(new TrustStrategy() {
			@Override
			public boolean isTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
				return true;
			}
		}).build();
		HostnameVerifier hnv = new NoopHostnameVerifier();
		SSLConnectionSocketFactory sslcf = new SSLConnectionSocketFactory(sslContext, hnv);

		Properties properties = getApplicationConfigProperties("monitoring");
		this.userName = properties.getProperty("monitoring_username");
		this.password = properties.getProperty("monitoring_password");
		this.serverUrl = properties.getProperty("monitoring_url");
		this.path = properties.getProperty("monitoring_query_path");

		// build the HTTP client
		this.httpClient = HttpClientBuilder.create().setSSLSocketFactory(sslcf).setDefaultRequestConfig(config).build();
	}

	/**
	 * Close the HTTP client
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private void disconnect() throws Exception {
		this.httpClient.close();
	}

	/**
	 * 
	 * @param query
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private JsonObject get(String query) throws Exception {
		String url = this.serverUrl + this.path + URLEncoder.encode(query, "UTF-8");
		HttpUriRequest httpRequest = new HttpGet(url);
		httpRequest.addHeader("Accept", "application/json; odata=verbose");

		if (this.userName != null) {
			String userpassword = this.userName + ":" + this.password;
			String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
			httpRequest.addHeader("Authorization", "Basic " + encodedAuthorization);
		}

		CloseableHttpResponse response = httpClient.execute(httpRequest);
		try {
			HttpEntity entity = response.getEntity();
			if (response.getStatusLine().getStatusCode() == 200) {
				Gson gson = new GsonBuilder().serializeNulls().create();
				String jsonResponse = EntityUtils.toString(entity, StandardCharsets.UTF_8);
				// System.out.println(jsonResponse.length());
				// System.out.println(jsonResponse.substring(0, 1000));
				JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
				return jsonObject;
			} else if (response.getStatusLine().getStatusCode() == 204) {
				// no content
				EntityUtils.consume(entity);
				return null;
			} else {
				String errorMessage = EntityUtils.toString(entity);
				throw new Exception("HTTP Code " + response.getStatusLine().getStatusCode() + (errorMessage != null ? ": " + errorMessage : ""));
			}
		} finally {
			response.close();
		}
	}

	@Override
	public void process(String operation) throws Exception {
		// close the current transaction
		commit(false);

		Set<String> persistenceUnits = new HashSet<>();

		// get the list of resources
		ResourceService resourceService = newService(ResourceService.class, Resource.class);
		List<Resource> resources = resourceService.list(true);
		logger.info("Monitoring " + resources.size() + " resources");
		for (Resource resource : resources) {
			try {
				AbstractBaseEntityService<?, ?, ?> service = resourceService.newService(resource.getName());
				if (service == null) {
					if (resource.getName().endsWith("Document")) {
						// ok, do not log
					} else {
						logger.debug("Skipping resource [" + resource.getName() + "]: no service");
					}
				} else {

					// build the query
					Query query = new Query().setActiveOnly(true).setPageSize(50);
					switch (resource.getName()) {
					// case "employeeSchedule":
					// break;
					//
					default:
						break;
					}

					// the first call to a new persistence unit is slow, do it twice
					String persistenceUnit = service.getPersistenceUnit() == null ? "default" : service.getPersistenceUnit();
					if (!persistenceUnits.contains(persistenceUnit)) {
						logger.debug("New persistence unit [" + persistenceUnit + "]");
						service.list(query);
						persistenceUnits.add(persistenceUnit);
					}

					// call the resource
					Date startDate = new Date();
					service.list(query);
					Date endDate = new Date();
					long methodExecutionTime = endDate.getTime() - startDate.getTime();
					logger.debug("Monitoring resource [" + resource.getName() + "]: " + methodExecutionTime + " ms");

					// TO DO call the web service resource
					// String path = resource.getResourceSecurityPath();
					// // for sub resource, use /0 as parent
					// path = path.replace("/", "/0/");

					// TO DO compare with the executionTime

					// close the service
					service.close();
					clearCache();
				}
			} catch (ValidationException ex) {
				logger.warn("Validation exception monitoring resource [" + resource.getName() + "]: " + ex);
			} catch (Exception ex) {
				logger.warn("Error monitoring resource [" + resource.getName() + "]: " + ex, ex);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		MonitoringService monitoringService = MonitoringService.newServiceStatic(MonitoringService.class);
		monitoringService.process();
		MainUtil.close();
	}
}
