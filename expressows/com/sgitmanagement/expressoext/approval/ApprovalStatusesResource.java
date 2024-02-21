package com.sgitmanagement.expressoext.approval;

import com.sgitmanagement.expressoext.approval.ApprovalStatusesResource.ApprovalStatusResource;
import com.sgitmanagement.expressoext.base.BaseOptionResource;
import com.sgitmanagement.expressoext.base.BaseOptionsResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/approvalStatus")
public class ApprovalStatusesResource extends BaseOptionsResource<ApprovalStatus, ApprovalStatusService, ApprovalStatusResource> {
	public ApprovalStatusesResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(ApprovalStatus.class, request, response, new ApprovalStatusResource(request, response));
	}

	static public class ApprovalStatusResource extends BaseOptionResource<ApprovalStatus, ApprovalStatusService> {
		public ApprovalStatusResource(HttpServletRequest request, HttpServletResponse response) {
			super(ApprovalStatus.class, request, response);
		}
	}
}
