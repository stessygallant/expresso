package com.sgitmanagement.expressoext.modif;

import com.sgitmanagement.expressoext.base.BaseOptionResource;
import com.sgitmanagement.expressoext.base.BaseOptionsResource;
import com.sgitmanagement.expressoext.modif.RequiredApprovalStatusesResource.RequiredApprovalStatusResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/requiredApprovalStatus")
public class RequiredApprovalStatusesResource extends BaseOptionsResource<RequiredApprovalStatus, RequiredApprovalStatusService, RequiredApprovalStatusResource> {
	public RequiredApprovalStatusesResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(RequiredApprovalStatus.class, request, response, new RequiredApprovalStatusResource(request, response), RequiredApprovalStatusService.class);
	}

	static public class RequiredApprovalStatusResource extends BaseOptionResource<RequiredApprovalStatus, RequiredApprovalStatusService> {
		public RequiredApprovalStatusResource(HttpServletRequest request, HttpServletResponse response) {
			super(RequiredApprovalStatus.class, request, response);
		}
	}
}