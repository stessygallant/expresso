package com.sgitmanagement.expressoext.approval;

import com.sgitmanagement.expressoext.approval.ApprovalFlowsResource.ApprovalFlowResource;
import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/approvalFlow")
public class ApprovalFlowsResource extends BaseEntitiesResource<ApprovalFlow, ApprovalFlowService, ApprovalFlowResource> {
	public ApprovalFlowsResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(ApprovalFlow.class, request, response, new ApprovalFlowResource(request, response), ApprovalFlowService.class);
	}

	static public class ApprovalFlowResource extends BaseEntityResource<ApprovalFlow, ApprovalFlowService> {
		public ApprovalFlowResource(HttpServletRequest request, HttpServletResponse response) {
			super(ApprovalFlow.class, request, response);
		}
	}
}
