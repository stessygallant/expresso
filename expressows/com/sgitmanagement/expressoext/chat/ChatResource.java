package com.sgitmanagement.expressoext.chat;

import java.io.IOException;

import com.sgitmanagement.expressoext.base.BaseWebSocketResource;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/websocket/chat", decoders = ChatMessageDecoder.class, encoders = ChatMessageEncoder.class)
public class ChatResource extends BaseWebSocketResource<ChatService> {
	public ChatResource() {
		super(ChatService.class);
	}

	@OnOpen
	public void onOpen(Session session) throws IOException {
		getService().onOpen(session);
	}

	@OnClose
	public void onClose(Session session) throws IOException {
		getService().onClose(session);
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		getService().onError(session, throwable);
	}

	@OnMessage
	public void onMessage(Session session, ChatMessage message) throws IOException {
		getService().onMessage(session, message);
	}
}