package com.sgitmanagement.expressoext.base;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class BaseOptionResource<E extends BaseOption, S extends BaseOptionService<E>> extends BaseDeactivableEntityResource<E, S> {

	public BaseOptionResource(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response) {
		super(typeOfE, request, response);
	}
}
