package com.sgitmanagement.expressoext.websocket;

import java.io.IOException;

import com.google.gson.JsonElement;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.core.Context;

@ServerEndpoint(value = "/websocket/{resourceSecurityPath}", decoders = WebSocketMessageDecoder.class, encoders = WebSocketMessageEncoder.class)
public class WebSocketResource extends BaseResource<WebSocketService> {

	public WebSocketResource() {
		super(null, null, WebSocketService.class);
	}

	public WebSocketResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(request, response, WebSocketService.class);
	}

	@OnOpen
	public void onOpen(Session session, @PathParam("resourceSecurityPath") String resourceSecurityPath) throws IOException {
		logger.info("New session id[" + session.getId() + "] user[" + (getUser() != null ? getUser().getUserName() : "n/a") + "] ip[" + Util.getIpAddress(getRequest()) + "] rs[" + resourceSecurityPath
				+ "]");
		getService().addWebSocketSession(session, resourceSecurityPath);
	}

	@OnClose
	public void onClose(Session session, @PathParam("resourceSecurityPath") String resourceSecurityPath) throws IOException {
		logger.info("Disconnected session id[" + session.getId() + "] user[" + (getUser() != null ? getUser().getUserName() : "n/a") + "] ip[" + Util.getIpAddress(getRequest()) + "] rs["
				+ resourceSecurityPath + "]");
		getService().removeWebSocketSession(session, resourceSecurityPath);
	}

	@OnError
	public void onError(Session session, Throwable throwable, @PathParam("resourceSecurityPath") String resourceSecurityPath) {
		logger.warn("Error on session id[" + session.getId() + "] user[" + (getUser() != null ? getUser().getUserName() : "n/a") + "] ip[" + Util.getIpAddress(getRequest()) + "] rs["
				+ resourceSecurityPath + "]: " + throwable);
		getService().removeWebSocketSession(session, resourceSecurityPath);
	}

	@OnMessage
	public void onMessage(Session session, JsonElement message) throws IOException {
		// this is a keepalive packet
		// getService().onMessage(session, message);
	}
}