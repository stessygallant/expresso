package com.sgitmanagement.expressoext.ldap;

import com.sgitmanagement.expressoext.base.BaseResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/activeDirectoryUser")
public class ActiveDirectoryUsersResource extends BaseResource<ActiveDirectoryUserService> {
	public ActiveDirectoryUsersResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(request, response, ActiveDirectoryUserService.class);
	}
}
