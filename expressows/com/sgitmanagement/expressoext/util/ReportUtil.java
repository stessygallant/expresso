package com.sgitmanagement.expressoext.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.exception.BaseException;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expressoext.security.User;

import jakarta.servlet.http.HttpServletResponse;

public enum ReportUtil {
	INSTANCE;

	final private Logger logger = LoggerFactory.getLogger(ReportUtil.class);
	private final String BIRT_URL;
	private final String BIRT_REPORT_FOLDER;

	private ReportUtil() {
		Properties props = SystemEnv.INSTANCE.getProperties("config");
		BIRT_URL = props.getProperty("birt_url");
		BIRT_REPORT_FOLDER = props.getProperty("birt_report_folder");
	}

	/**
	 * This method is used to execute a report and save it to a file
	 *
	 * @param user
	 * @param reportName
	 * @param formParams
	 * @return
	 * @throws Exception
	 */
	public File executeReport(User user, String reportName, String fileName, Map<String, String> formParams) throws Exception {
		// create a temp file
		File file = File.createTempFile(fileName + ".", "." + getFormat(formParams));
		// System.out.println("Temp file [" + file.getAbsolutePath() + "]");
		OutputStream os = new FileOutputStream(file);
		executeReport(user, reportName, fileName, formParams, null, os);
		return file;
	}

	public void executeReport(User user, String reportName, String fileName, Map<String, String> formParams, HttpServletResponse httpServletResponse, OutputStream os) {
		try {
			if (reportName == null) {
				reportName = formParams.get("reportName");
			}

			// add standard params
			if (formParams == null) {
				formParams = new HashMap<>();
			}
			String format = getFormat(formParams);
			formParams.put("__format", format);
			formParams.put("__report", "report/" + BIRT_REPORT_FOLDER + "/" + reportName + ".rptdesign");
			formParams.put("user", user.getFullName());
			formParams.put("userId", "" + user.getId());

			// execute the report
			URIBuilder uriBuilder = new URIBuilder(BIRT_URL);
			for (Map.Entry<String, String> entry : formParams.entrySet()) {
				uriBuilder.addParameter(entry.getKey(), entry.getValue());
			}

			String url = uriBuilder.build().toString();
			logger.info("URL=[" + url + "]");
			Request.Get(url).execute().handleResponse(new ResponseHandler<Void>() {

				@Override
				public Void handleResponse(final HttpResponse response) throws IOException {
					StatusLine statusLine = response.getStatusLine();
					HttpEntity entity = response.getEntity();
					if (statusLine.getStatusCode() >= 300) {
						throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
					}
					if (entity == null) {
						throw new ClientProtocolException("BIRT Response contains no content");
					}

					if (httpServletResponse != null) {
						ContentType contentType = ContentType.getOrDefault(entity);
						httpServletResponse.setContentType(contentType.getMimeType());
						httpServletResponse.setHeader("Content-disposition", "inline; filename=" + fileName + "." + format);
					}

					InputStream is = entity.getContent();
					byte[] buffer = new byte[1024];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}
					os.flush();
					is.close();
					os.close();
					return null;
				}

			});
		} catch (IOException e) {
			// Caused by: java.io.IOException: An established connection was aborted by the software in your host
			// machine
			// ignore
		} catch (Exception e) {
			throw new BaseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error executing report", e);
		}
	}

	private String getFormat(Map<String, String> formParams) {
		// add the format
		String format = null;
		if (formParams != null) {
			format = formParams.get("format");
		}
		if (format == null) {
			format = "pdf";
		}
		return format;
	}
}
