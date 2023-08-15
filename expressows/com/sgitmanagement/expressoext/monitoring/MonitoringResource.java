package com.sgitmanagement.expressoext.monitoring;

import com.sgitmanagement.expressoext.base.BaseResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/monitoring")
public class MonitoringResource extends BaseResource<MonitoringService> {

	public MonitoringResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(request, response, MonitoringService.class);
	}
}