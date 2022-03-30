package com.sgitmanagement.expressoext.base;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sgitmanagement.expresso.base.AbstractBaseResource;
import com.sgitmanagement.expresso.base.AbstractBaseService;
import com.sgitmanagement.expressoext.security.User;

public abstract class BaseResource<S extends AbstractBaseService<User>> extends AbstractBaseResource<S, User> {

	protected BaseResource(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}

	public BaseResource(HttpServletRequest request, HttpServletResponse response, Class<S> serviceClass) {
		super(request, response, serviceClass);
	}
}
