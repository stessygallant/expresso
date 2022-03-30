package com.sgitmanagement.expressoext.base;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;

public abstract class BaseOptionsResource<E extends BaseOption, S extends BaseOptionService<E>, R extends BaseOptionResource<E, S>> extends BaseEntitiesResource<E, S, R> {
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BaseOptionsResource(Class<E> typeOfE, @Context HttpServletRequest request, @Context HttpServletResponse response, R baseOptionResource) {
		super(typeOfE, request, response, baseOptionResource, (Class) BaseOptionService.class);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BaseOptionsResource(Class<E> typeOfE, @Context HttpServletRequest request, @Context HttpServletResponse response, R baseOptionResource, Integer parentId) {
		super(typeOfE, request, response, baseOptionResource, (Class) BaseOptionService.class, parentId);
	}

	public BaseOptionsResource(Class<E> typeOfE, @Context HttpServletRequest request, @Context HttpServletResponse response, R baseOptionResource, Class<S> serviceClass) {
		super(typeOfE, request, response, baseOptionResource, serviceClass);
	}

	public BaseOptionsResource(Class<E> typeOfE, @Context HttpServletRequest request, @Context HttpServletResponse response, R baseOptionResource, Class<S> serviceClass, Integer parentId) {
		super(typeOfE, request, response, baseOptionResource, serviceClass, parentId);
	}
}