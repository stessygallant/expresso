package com.sgitmanagement.expressoext.approval;

import com.sgitmanagement.expressoext.approval.ApprovalsResource.ApprovalResource;
import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/approval")
public class ApprovalsResource extends BaseEntitiesResource<Approval, ApprovalService, ApprovalResource> {
	public ApprovalsResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Approval.class, request, response, new ApprovalResource(request, response), ApprovalService.class);
	}

	static public class ApprovalResource extends BaseEntityResource<Approval, ApprovalService> {
		public ApprovalResource(HttpServletRequest request, HttpServletResponse response) {
			super(Approval.class, request, response);
		}
	}
}
