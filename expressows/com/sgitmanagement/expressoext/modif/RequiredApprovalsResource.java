package com.sgitmanagement.expressoext.modif;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.modif.RequiredApprovalsResource.RequiredApprovalResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;

@Path("/requiredApproval")
public class RequiredApprovalsResource extends BaseEntitiesResource<RequiredApproval, RequiredApprovalService, RequiredApprovalResource> {

	public RequiredApprovalsResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(RequiredApproval.class, request, response, new RequiredApprovalResource(request, response), RequiredApprovalService.class);
	}

	static public class RequiredApprovalResource extends BaseEntityResource<RequiredApproval, RequiredApprovalService> {

		public RequiredApprovalResource(HttpServletRequest request, HttpServletResponse response) {
			super(RequiredApproval.class, request, response);
		}

		public RequiredApproval approve(MultivaluedMap<String, String> formParams) throws Exception {
			RequiredApproval requiredApproval = get(getId());
			return getService().approve(requiredApproval);
		}

		public RequiredApproval reject(MultivaluedMap<String, String> formParams) throws Exception {
			String comment = formParams.getFirst("comment");
			RequiredApproval requiredApproval = get(getId());
			return getService().reject(requiredApproval, comment);
		}
	}
}
