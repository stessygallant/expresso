package com.sgitmanagement.expressoext.modif;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.modif.AuditTrailsResource.AuditTrailResource;

@Path("/{audittrail:(?i)audittrail}")
public class AuditTrailsResource extends BaseEntitiesResource<AuditTrail, AuditTrailService, AuditTrailResource> {

	public AuditTrailsResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(AuditTrail.class, request, response, new AuditTrailResource(request, response), AuditTrailService.class);
	}

	static public class AuditTrailResource extends BaseEntityResource<AuditTrail, AuditTrailService> {

		public AuditTrailResource(HttpServletRequest request, HttpServletResponse response) {
			super(AuditTrail.class, request, response);
		}

	}

}
