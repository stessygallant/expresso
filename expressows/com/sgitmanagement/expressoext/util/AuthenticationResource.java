package com.sgitmanagement.expressoext.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;

import com.sgitmanagement.expressoext.base.BaseResource;

@Path("/authentication")
public class AuthenticationResource extends BaseResource<AuthenticationService> {

	public AuthenticationResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(request, response, AuthenticationService.class);
	}

	public void login(MultivaluedMap<String, String> formParams) throws Exception {
		getService().login(getUser());
	}

	public void logout(MultivaluedMap<String, String> formParams) throws Exception {
		getService().logout();
	}

	public void reset(MultivaluedMap<String, String> formParams) throws Exception {
		String userName = formParams.getFirst("userName");
		getService().resetPassword(userName);
	}

	public void validate(MultivaluedMap<String, String> formParams) throws Exception {
		String userName = formParams.getFirst("userName");
		String newPassword = formParams.getFirst("newPassword");
		String securityTokenNo = formParams.getFirst("securityToken");
		getService().setNewPassword(userName, newPassword, securityTokenNo);
	}
}
