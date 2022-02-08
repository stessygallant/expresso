package com.sgitmanagement.expresso.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.WinHttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class HttpRESTUtil {
	static public HttpRESTUtil INSTANCE;

	static {
		// create the default singleton
		INSTANCE = new HttpRESTUtil();
	}

	private HttpRESTUtil() {

	}

	public JsonObject performRESTRequest(String urlString) throws Exception {
		return performRESTRequest(urlString, "GET", null, null, null, null, null);
	}

	public JsonObject performRESTRequest(String urlString, String method, String body) throws Exception {
		return performRESTRequest(urlString, method, null, body, null, null, null);
	}

	public JsonObject performRESTRequest(String urlString, String method, String body, String username, String password) throws Exception {
		return performRESTRequest(urlString, method, null, body, null, username, password);
	}

	public JsonObject performRESTRequest(String urlString, String method, String username, String password) throws Exception {
		return performRESTRequest(urlString, method, null, null, null, username, password);
	}

	public JsonObject performRESTRequest(String urlString, String method, String contentType, String body, String encoding, String username, String password) throws Exception {
		return performRESTRequest(urlString, method, contentType, body, encoding, username, password, null);
	}

	public JsonObject performRESTRequest(String urlString, String method, String contentType, String body, String encoding, String username, String password, Map<String, String> headers)
			throws Exception {

		// Timeout after 60 seconds
		int timeout = 60 * 1000; // ms

		return performRESTRequest(urlString, method, contentType, body, encoding, username, password, headers, timeout);
	}

	/**
	 * Helper method for REST connector to actually send the request
	 * 
	 * @param urlString
	 * @param method
	 * @param contentType
	 * @param body
	 * @param encoding
	 * @param username
	 * @param password
	 * @param headers
	 * @return
	 * @throws Exception
	 */
	public JsonObject performRESTRequest(String urlString, String method, String contentType, String body, String encoding, String username, String password, Map<String, String> headers,
			int timeoutInMs) throws Exception {

		if (encoding == null) {
			encoding = "UTF-8";
		}

		if (contentType == null) {
			contentType = "application/json; charset=" + encoding;
		}

		CloseableHttpClient httpClient;
		RequestConfig config = RequestConfig.custom().setConnectTimeout(timeoutInMs).setConnectionRequestTimeout(timeoutInMs).setSocketTimeout(timeoutInMs).build();
		if (WinHttpClients.isWinAuthAvailable()) {
			// There is no need to provide user credentials
			// HttpClient will attempt to access current user security context through
			// Windows platform specific methods via JNI.
			httpClient = WinHttpClients.custom().setDefaultRequestConfig(config).build();
		} else {
			httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
		}

		// System.out.println(method + " " + urlString);
		try {
			HttpUriRequest httpRequest;
			switch (method) {
			case "PUT":
				httpRequest = new HttpPut(urlString);
				break;
			case "DELETE":
				httpRequest = new HttpDelete(urlString);
				break;
			case "GET":
				httpRequest = new HttpGet(urlString);
				break;
			case "POST":
			default:
				httpRequest = new HttpPost(urlString);
				break;
			}

			// set Basic Authentication if needed
			if (username != null) {
				String userpassword = username + ":" + password;
				String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
				httpRequest.addHeader("Authorization", "Basic " + encodedAuthorization);
			}

			// add custom header
			if (headers != null) {
				for (String key : headers.keySet()) {
					httpRequest.addHeader(key, headers.get(key));
				}
			}

			// add the body if needed
			if (body != null && body.trim().length() > 0) {
				StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
				entity.setContentType(new BasicHeader("Content-Type", contentType));
				entity.setContentEncoding(new BasicHeader("Charset", encoding));
				((HttpEntityEnclosingRequestBase) httpRequest).setEntity(entity);
			}

			CloseableHttpResponse response = httpClient.execute(httpRequest);
			try {
				HttpEntity entity = response.getEntity();
				if (response.getStatusLine().getStatusCode() == 200) {
					Gson gson = new GsonBuilder().serializeNulls().create();
					String jsonResponse = EntityUtils.toString(entity);
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
		} finally {
			httpClient.close();
		}
	}
}
