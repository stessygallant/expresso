package com.sgitmanagement.expresso.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserManager implements AutoCloseable {
	final static protected Logger logger = LoggerFactory.getLogger(UserManager.class);
	private static UserManager instance;

	private static ThreadLocal<IUser> userThreadLocal = new ThreadLocal<>();

	static {
		instance = new UserManager();
	}

	static public UserManager getInstance() {
		return instance;
	}

	// make sure application cannot instantiate the PersistenceManager
	private UserManager() {
	}

	public IUser getUser() {
		return userThreadLocal.get();
	}

	public void setUser(IUser user) {
		userThreadLocal.set(user);
	}

	@Override
	public void close() {
		userThreadLocal.remove();
	}
}