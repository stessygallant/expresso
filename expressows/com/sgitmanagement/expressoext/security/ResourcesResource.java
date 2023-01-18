package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.security.ResourcesResource.ResourceResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/resource")
public class ResourcesResource extends BaseEntitiesResource<Resource, ResourceService, ResourceResource> {
	public ResourcesResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Resource.class, request, response, new ResourceResource(request, response), ResourceService.class);
	}

	static public class ResourceResource extends BaseEntityResource<Resource, ResourceService> {
		public ResourceResource(HttpServletRequest request, HttpServletResponse response) {
			super(Resource.class, request, response);
		}
	}
}