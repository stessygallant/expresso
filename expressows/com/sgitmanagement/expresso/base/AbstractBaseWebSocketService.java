package com.sgitmanagement.expresso.base;

import java.io.IOException;

import jakarta.websocket.Session;

public abstract class AbstractBaseWebSocketService<U extends IUser> extends AbstractBaseService<U> {
	public void onOpen(Session session) throws IOException {
		logger.debug("New session [" + session.getId() + "]");
	}

	public void onClose(Session session) throws IOException {
		logger.debug("Disconnected session [" + session.getId() + "]");
	}

	public void onError(Session session, Throwable throwable) {
		logger.error("Error on session [" + session.getId() + "]", throwable);
	}

	public void onMessage(Session session, Object message) throws IOException {
		// must be implemented by the subclass
	}
}