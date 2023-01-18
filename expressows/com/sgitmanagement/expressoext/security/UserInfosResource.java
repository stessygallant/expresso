package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.security.UserInfosResource.UserInfoResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;

public class UserInfosResource extends BaseEntitiesResource<UserInfo, UserInfoService, UserInfoResource> {
	public UserInfosResource(@Context HttpServletRequest request, @Context HttpServletResponse response, Integer parentId) {
		super(UserInfo.class, request, response, new UserInfoResource(request, response), UserInfoService.class, parentId);
	}

	static public class UserInfoResource extends BaseEntityResource<UserInfo, UserInfoService> {
		public UserInfoResource(HttpServletRequest request, HttpServletResponse response) {
			super(UserInfo.class, request, response);
		}
	}
}