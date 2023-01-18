package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.security.UserConfigsResource.UserConfigResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;

public class UserConfigsResource extends BaseEntitiesResource<UserConfig, UserConfigService, UserConfigResource> {
	public UserConfigsResource(@Context HttpServletRequest request, @Context HttpServletResponse response, Integer parentId) {
		super(UserConfig.class, request, response, new UserConfigResource(request, response), UserConfigService.class, parentId);
	}

	static public class UserConfigResource extends BaseEntityResource<UserConfig, UserConfigService> {
		public UserConfigResource(HttpServletRequest request, HttpServletResponse response) {
			super(UserConfig.class, request, response);
		}
	}
}