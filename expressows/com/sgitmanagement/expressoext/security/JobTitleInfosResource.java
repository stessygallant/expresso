package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.security.JobTitleInfosResource.JobTitleInfoResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;

public class JobTitleInfosResource extends BaseEntitiesResource<JobTitleInfo, JobTitleInfoService, JobTitleInfoResource> {
	public JobTitleInfosResource(@Context HttpServletRequest request, @Context HttpServletResponse response, Integer parentId) {
		super(JobTitleInfo.class, request, response, new JobTitleInfoResource(request, response), JobTitleInfoService.class, parentId);
	}

	static public class JobTitleInfoResource extends BaseEntityResource<JobTitleInfo, JobTitleInfoService> {
		public JobTitleInfoResource(HttpServletRequest request, HttpServletResponse response) {
			super(JobTitleInfo.class, request, response);
		}
	}
}