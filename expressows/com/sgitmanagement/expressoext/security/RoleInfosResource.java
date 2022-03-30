package com.sgitmanagement.expressoext.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.security.RoleInfosResource.RoleInfoResource;

public class RoleInfosResource extends BaseEntitiesResource<RoleInfo, RoleInfoService, RoleInfoResource> {
	public RoleInfosResource(@Context HttpServletRequest request, @Context HttpServletResponse response, Integer parentId) {
		super(RoleInfo.class, request, response, new RoleInfoResource(request, response), RoleInfoService.class, parentId);
	}

	static public class RoleInfoResource extends BaseEntityResource<RoleInfo, RoleInfoService> {
		public RoleInfoResource(HttpServletRequest request, HttpServletResponse response) {
			super(RoleInfo.class, request, response);
		}
	}
}