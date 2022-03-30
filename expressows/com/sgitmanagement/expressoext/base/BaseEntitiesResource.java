package com.sgitmanagement.expressoext.base;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import com.sgitmanagement.expresso.base.AbstractBaseEntitiesResource;
import com.sgitmanagement.expresso.base.AbstractBaseEntityResource;
import com.sgitmanagement.expressoext.security.User;

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

	// public BaseEntitiesResource(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response,
	// R baseEntityResource, Class<S> serviceClass, Integer parentId) {
	// super(typeOfE, request, response, baseEntityResource, serviceClass, parentId);
	// }

	public BaseEntitiesResource(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response, R baseEntityResource, Class<S> serviceClass, Integer parentId) {
		super(typeOfE, request, response, baseEntityResource, serviceClass, parentId);
	}

	/**
	 * Print the list of resources (from ids)
	 *
	 * @param ui
	 * @throws Exception
	 */
	@GET
	@Path("print")
	public void print(@Context UriInfo ui) throws Exception {
		MultivaluedMap<String, String> params = ui.getQueryParameters();
		String ids = params.getFirst("ids");
		String id = null;
		if (ids != null) {
			// get the first id
			if (ids.contains(",")) {
				id = ids.split(",")[0];
			} else {
				id = ids;
			}
		}
		String resourceName = getTypeOfE().getSimpleName().substring(0, 1).toLowerCase() + getTypeOfE().getSimpleName().substring(1);
		Map<String, String> paramMap = new HashMap<>();
		paramMap.put("id", "" + id);
		paramMap.put(resourceName + "Id", "" + id);
		paramMap.put(resourceName + "Ids", "" + ids);
		paramMap.put(resourceName.toLowerCase() + "Id", "" + id);
		paramMap.put(resourceName.toLowerCase() + "Ids", "" + ids);
		getService().print(paramMap, getResponse());
	}
}