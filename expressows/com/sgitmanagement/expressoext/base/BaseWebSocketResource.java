package com.sgitmanagement.expressoext.base;

import com.sgitmanagement.expresso.base.AbstractBaseWebSocketResource;
import com.sgitmanagement.expressoext.security.User;

public abstract class BaseWebSocketResource<S extends BaseWebSocketService> extends AbstractBaseWebSocketResource<S, User> {
	public BaseWebSocketResource(Class<S> serviceClass) {
		super(serviceClass);
	}

}
