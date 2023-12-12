package com.sgitmanagement.expresso.websocket;

import java.io.IOException;

import com.sgitmanagement.expressoext.base.BaseResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
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
		logger.debug("New session [" + session.getId() + "]");
		getService().addWebSocketSession(session, resourceSecurityPath);
	}

	@OnClose
	public void onClose(Session session, @PathParam("resourceSecurityPath") String resourceSecurityPath) throws IOException {
		logger.debug("Disconnected session [" + session.getId() + "]");
		getService().removeWebSocketSession(session, resourceSecurityPath);
	}

	@OnError
	public void onError(Session session, Throwable throwable, @PathParam("resourceSecurityPath") String resourceSecurityPath) {
		logger.error("Error on session [" + session.getId() + "]", throwable);
		getService().removeWebSocketSession(session, resourceSecurityPath);
	}
}