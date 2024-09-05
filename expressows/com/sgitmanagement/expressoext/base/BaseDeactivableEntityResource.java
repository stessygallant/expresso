package com.sgitmanagement.expressoext.base;

import com.sgitmanagement.expresso.base.AbstractBaseEntityResource;
import com.sgitmanagement.expresso.base.Deactivable;
import com.sgitmanagement.expressoext.security.BasicUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MultivaluedMap;

public abstract class BaseDeactivableEntityResource<E extends BaseEntity & Deactivable, S extends BaseDeactivableEntityService<E>> extends AbstractBaseEntityResource<E, S, BasicUser, Integer> {

	public BaseDeactivableEntityResource(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response) {
		super(typeOfE, request, response);
	}

	/**
	 *
	 * @param formParams
	 * @return
	 * @throws Exception
	 */
	public E activate(MultivaluedMap<String, String> formParams) throws Exception {
		E e = get(getId());
		return getService().activate(e);
	}

	/**
	 *
	 * @param formParams
	 * @return
	 * @throws Exception
	 */
	@Override
	public E deactivate(MultivaluedMap<String, String> formParams) throws Exception {
		E e = get(getId());
		return getService().deactivate(e);
	}

}