package com.sgitmanagement.expressoext.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.security.PrivilegesResource.PrivilegeResource;

@Path("/{privilege:(?i)privilege}")
public class PrivilegesResource extends BaseEntitiesResource<Privilege, PrivilegeService, PrivilegeResource> {
	public PrivilegesResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Privilege.class, request, response, new PrivilegeResource(request, response), PrivilegeService.class);
	}

	static public class PrivilegeResource extends BaseEntityResource<Privilege, PrivilegeService> {
		public PrivilegeResource(HttpServletRequest request, HttpServletResponse response) {
			super(Privilege.class, request, response);
		}
	}
}
