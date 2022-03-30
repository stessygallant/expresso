package com.sgitmanagement.expressoext.base;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;

public abstract class BaseDeactivavleEntitiesResource<E extends BaseDeactivableEntity, S extends BaseDeactivableEntityService<E>, R extends BaseDeactivableEntityResource<E, S>>
		extends BaseEntitiesResource<E, S, R> {
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BaseDeactivavleEntitiesResource(Class<E> typeOfE, @Context HttpServletRequest request, @Context HttpServletResponse response, R baseDeactivableEntityResource) {
		super(typeOfE, request, response, baseDeactivableEntityResource, (Class) BaseDeactivableEntityService.class);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BaseDeactivavleEntitiesResource(Class<E> typeOfE, @Context HttpServletRequest request, @Context HttpServletResponse response, R baseDeactivableEntityResource, String parentName,
			Integer parentId) {
		super(typeOfE, request, response, baseDeactivableEntityResource, (Class) BaseDeactivableEntityService.class, parentId);
	}

	public BaseDeactivavleEntitiesResource(Class<E> typeOfE, @Context HttpServletRequest request, @Context HttpServletResponse response, R baseDeactivableEntityResource, Class<S> serviceClass) {
		super(typeOfE, request, response, baseDeactivableEntityResource, serviceClass);
	}

	public BaseDeactivavleEntitiesResource(Class<E> typeOfE, @Context HttpServletRequest request, @Context HttpServletResponse response, R baseDeactivableEntityResource, Class<S> serviceClass,
			Integer parentId) {
		super(typeOfE, request, response, baseDeactivableEntityResource, serviceClass, parentId);
	}
}