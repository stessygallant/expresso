package com.sgitmanagement.expressoext.base;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sgitmanagement.expresso.base.AbstractBaseEntityResource;
import com.sgitmanagement.expressoext.security.User;

/**
 * Methods allowed on an entity<br>
 * GET - get an entity of the resource<br>
 * DELETE - delete an entity of the resource<br>
 * PUT - update the entire entity or only some fields<br>
 * POST - POST method on an entity is only for executing an action, never to create an entity<br>
 */
public abstract class BaseEntityResource<E extends BaseEntity, S extends BaseEntityService<E>> extends AbstractBaseEntityResource<E, S, User, Integer> {
	public BaseEntityResource(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response) {
		super(typeOfE, request, response);

	}
}
