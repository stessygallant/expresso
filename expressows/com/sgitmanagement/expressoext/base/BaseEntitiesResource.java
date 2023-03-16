package com.sgitmanagement.expressoext.base;

import com.sgitmanagement.expresso.base.AbstractBaseEntitiesResource;
import com.sgitmanagement.expresso.base.AbstractBaseEntityResource;
import com.sgitmanagement.expressoext.security.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Methods allowed on a resource<br>
 * GET - get the list of entities for the resource<br>
 * POST - create a new entity OR perform an action a the resource (ex: process, sync, etc)<br>
 * PUT - not allowed<br>
 * DELETE - not allowed<br>
 */
public abstract class BaseEntitiesResource<E extends BaseEntity, S extends BaseEntityService<E>, R extends AbstractBaseEntityResource<E, S, User, Integer>>
		extends AbstractBaseEntitiesResource<E, S, R, User, Integer> {
	public BaseEntitiesResource(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response, R baseEntityResource, Class<S> serviceClass) {
		super(typeOfE, request, response, baseEntityResource, serviceClass);
	}

	public BaseEntitiesResource(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response, R baseEntityResource, Class<S> serviceClass, Integer parentId) {
		super(typeOfE, request, response, baseEntityResource, serviceClass, parentId);
	}
}