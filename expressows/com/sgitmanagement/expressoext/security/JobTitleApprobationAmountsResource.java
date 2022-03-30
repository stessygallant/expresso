package com.sgitmanagement.expressoext.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.security.JobTitleApprobationAmountsResource.JobTitleApprobationAmountResource;

public class JobTitleApprobationAmountsResource extends BaseEntitiesResource<JobTitleApprobationAmount, JobTitleApprobationAmountService, JobTitleApprobationAmountResource> {

	public JobTitleApprobationAmountsResource(@Context HttpServletRequest request, @Context HttpServletResponse response, Integer parentId) {
		super(JobTitleApprobationAmount.class, request, response, new JobTitleApprobationAmountResource(request, response), JobTitleApprobationAmountService.class, parentId);
	}

	static public class JobTitleApprobationAmountResource extends BaseEntityResource<JobTitleApprobationAmount, JobTitleApprobationAmountService> {
		public JobTitleApprobationAmountResource(HttpServletRequest request, HttpServletResponse response) {
			super(JobTitleApprobationAmount.class, request, response);
		}

	}
}
