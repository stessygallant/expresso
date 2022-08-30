package com.sgitmanagement.expressoext.base;

import java.io.IOException;
import java.util.List;

import com.sgitmanagement.expresso.base.AbstractBaseService;
import com.sgitmanagement.expressoext.security.AuthorizationHelper;
import com.sgitmanagement.expressoext.security.User;

import jakarta.websocket.Session;

public class BaseWebSocketService extends AbstractBaseService<User> {

	public void onOpen(Session session) throws IOException {
		String message = "New session [" + session.getId() + "]";
		logger.info(message);
	}

	public void onClose(Session session) throws IOException {
		String message = "Disconnected session [" + session.getId() + "]";
		logger.info(message);
	}

	public void onError(Session session, Throwable throwable) {
		logger.error("Error on session [" + session.getId() + "]", throwable);
	}

	public void onMessage(Session session, Object message) throws IOException {
		// must be implemented by the subclass
	}

	@Override
	final public boolean isUserInRole(String rolePgmKey) {
		return AuthorizationHelper.isUserInRole(getUser(), rolePgmKey);
	}

	@Override
	final public boolean isUserAdmin() {
		return AuthorizationHelper.isUserAdmin(getUser());
	}

	@Override
	final public boolean isUserAllowed(String action, List<String> resources) {
		return AuthorizationHelper.isUserAllowed(getUser(), action, resources);
	}

	@Override
	final public User getUser(String userName) {
		return AuthorizationHelper.getUser(userName);
	}

	@Override
	final public User getSystemUser() {
		return AuthorizationHelper.getSystemUser();
	}

	@Override
	final public User getPublicUser() {
		return AuthorizationHelper.getPublicUser();
	}
}
