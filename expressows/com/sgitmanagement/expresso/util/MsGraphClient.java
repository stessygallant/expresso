package com.sgitmanagement.expresso.util;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.apache.http.Consts;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;

public class MsGraphClient {
	private static final String MSGRAPH_URL;
	private String msGraphAccessToken;

	static {
		MSGRAPH_URL = SystemEnv.INSTANCE.getDefaultProperties().getProperty("msgraph_url");
	}

	public String connect(String scope) throws Exception {
		this.msGraphAccessToken = getMicrosoftGraphAccessToken(scope);
		return this.msGraphAccessToken;
	}

	public String connect() throws Exception {
		return connect(null);
	}

	public void disconnect() throws Exception {
		this.msGraphAccessToken = null;
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	private String getMicrosoftGraphAccessToken(String scope) throws Exception {
		Properties props = SystemEnv.INSTANCE.getProperties("config");
		String authority = props.getProperty("msgraph_authority");
		String clientId = props.getProperty("msgraph_client_id");
		String secret = props.getProperty("msgraph_client_secret");
		if (scope == null) {
			scope = props.getProperty("msgraph_scope");
		}

		ConfidentialClientApplication app = ConfidentialClientApplication.builder(clientId, ClientCredentialFactory.createFromSecret(secret)).authority(authority).build();

		// With client credentials flows the scope is ALWAYS of the shape "resource/.default", as the
		// application permissions need to be set statically (in the portal), and then granted by a tenant administrator
		ClientCredentialParameters clientCredentialParam = ClientCredentialParameters.builder(Collections.singleton(scope)).build();

		CompletableFuture<IAuthenticationResult> future = app.acquireToken(clientCredentialParam);
		return future.get().accessToken();
	}

	public JsonObject callMicrosoftGraph(String path) throws Exception {
		return callMicrosoftGraph(path, "GET", null, ContentType.APPLICATION_JSON, null);
	}

	public JsonObject callMicrosoftGraph(String path, Object body) throws Exception {
		return callMicrosoftGraph(path, "POST", body, ContentType.APPLICATION_JSON, null);
	}

	public JsonObject callMicrosoftGraph(String path, String method, Object body, ContentType contentType, Map<String, String> headers) throws Exception {

		if (MSGRAPH_URL != null && MSGRAPH_URL.trim().length() > 0) {
			Request request;
			if (body == null && (method == null || method.equals("GET"))) {
				request = Request.Get(MSGRAPH_URL + path);
			} else if (method.equals("POST")) {
				request = Request.Post(MSGRAPH_URL + path);
			} else if (method.equals("DELETE")) {
				request = Request.Delete(MSGRAPH_URL + path);
			} else if (method.equals("PUT")) {
				request = Request.Put(MSGRAPH_URL + path);
			} else {
				throw new Exception("Method [" + method + "] not supported");
			}

			if (body != null) {
				if (contentType.equals(ContentType.DEFAULT_TEXT)) {
					request.bodyString((String) body, contentType);
				} else if (contentType.equals(ContentType.DEFAULT_BINARY)) {
					request.bodyByteArray((byte[]) body, contentType);
				} else {
					request.bodyString((String) body, contentType);
				}
			}

			request.addHeader("Authorization", "Bearer " + msGraphAccessToken).addHeader("Accept", "application/json");

			if (headers != null) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					request.addHeader(entry.getKey(), entry.getValue());
				}
			}

			Response response = request.execute();
			Content returnedContent = response.returnContent();
			if (returnedContent != null) {
				String content = returnedContent.asString(Consts.UTF_8);
				// System.out.println(content);
				return new Gson().fromJson(content, JsonObject.class);
			}
		}
		return null;
	}

	public static void main(String args[]) throws Exception {
		MsGraphClient msGraphClient = new MsGraphClient();
		try {
			String token = msGraphClient.connect("https://outlook.office365.com/.default");
			System.out.println(token);
		} finally {
			msGraphClient.disconnect();
		}
	}
}
