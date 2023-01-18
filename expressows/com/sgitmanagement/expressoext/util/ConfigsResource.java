package com.sgitmanagement.expressoext.util;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.util.ConfigsResource.ConfigResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/config")
public class ConfigsResource extends BaseEntitiesResource<Config, ConfigService, ConfigResource> {

	public ConfigsResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Config.class, request, response, new ConfigResource(request, response), ConfigService.class);
	}

	static public class ConfigResource extends BaseEntityResource<Config, ConfigService> {
		public ConfigResource(HttpServletRequest request, HttpServletResponse response) {
			super(Config.class, request, response);
		}
	}
}