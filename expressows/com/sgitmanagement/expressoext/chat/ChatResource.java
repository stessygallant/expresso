package com.sgitmanagement.expressoext.chat;

import java.io.IOException;

import com.sgitmanagement.expressoext.base.BaseResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.ws.rs.core.Context;

// @ServerEndpoint(value = "/websocket/chat", decoders = ChatMessageDecoder.class, encoders = ChatMessageEncoder.class)
public class ChatResource extends BaseResource<ChatService> {

	public ChatResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(request, response, ChatService.class);
	}

	@OnMessage
	public void onMessage(Session session, ChatMessage message) throws IOException {
		getService().onMessage(session, message);
	}
}