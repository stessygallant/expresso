package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.security.CompaniesResource.CompanyResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/company")
public class CompaniesResource extends BaseEntitiesResource<Company, CompanyService, CompanyResource> {

	public CompaniesResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Company.class, request, response, new CompanyResource(request, response), CompanyService.class);
	}

	static public class CompanyResource extends BaseEntityResource<Company, CompanyService> {

		public CompanyResource(HttpServletRequest request, HttpServletResponse response) {
			super(Company.class, request, response);
		}
	}
}