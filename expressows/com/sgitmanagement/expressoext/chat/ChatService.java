package com.sgitmanagement.expressoext.chat;

import java.io.IOException;

import com.sgitmanagement.expressoext.base.BaseWebSocketService;

import jakarta.websocket.Session;

public class ChatService extends BaseWebSocketService {

	/**
	 * 
	 * @param session
	 * @param chatMessage
	 * @throws IOException
	 */
	public void broadcast(Session broadcastingSession, ChatMessage chatMessage) throws IOException {
		for (Session session : broadcastingSession.getOpenSessions()) {
			// do not send the message to the incoming session
			if (session.isOpen() && !broadcastingSession.getId().equals(session.getId())) {
				synchronized (session) {
					try {
						logger.debug("Sending from[" + chatMessage.getFrom() + " message[" + chatMessage.getContent() + "]");
						session.getBasicRemote().sendObject(chatMessage);
					} catch (Exception ex) {
						logger.error("Cannot send message to session[" + session.getId() + "]: " + ex);
					}
				}
			}
		}
	}

	@Override
	public void onMessage(Session session, Object chatMessage) throws IOException {
		broadcast(session, (ChatMessage) chatMessage);
	}
}
