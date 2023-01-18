package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.security.PersonsResource.PersonResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/person")
public class PersonsResource extends BaseEntitiesResource<Person, PersonService, PersonResource> {
	public PersonsResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Person.class, request, response, new PersonResource(request, response), PersonService.class);
	}

	static public class PersonResource extends BaseEntityResource<Person, PersonService> {
		public PersonResource(HttpServletRequest request, HttpServletResponse response) {
			super(Person.class, request, response);
		}
	}
}
