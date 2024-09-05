package com.sgitmanagement.expressoext.base;

import com.sgitmanagement.expresso.base.AbstractBaseResource;
import com.sgitmanagement.expresso.base.AbstractBaseService;
import com.sgitmanagement.expressoext.security.BasicUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class BaseResource<S extends AbstractBaseService<BasicUser>> extends AbstractBaseResource<S, BasicUser> {

	protected BaseResource(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}

	public BaseResource(HttpServletRequest request, HttpServletResponse response, Class<S> serviceClass) {
		super(request, response, serviceClass);
	}
}
