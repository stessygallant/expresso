package com.sgitmanagement.expressoext.security;

import java.util.HashSet;
import java.util.Set;

import com.sgitmanagement.expressoext.base.BaseOptionResource;
import com.sgitmanagement.expressoext.base.BaseOptionsResource;
import com.sgitmanagement.expressoext.security.JobTitlesResource.JobTitleResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@Path("jobTitle")
public class JobTitlesResource extends BaseOptionsResource<JobTitle, JobTitleService, JobTitleResource> {

	public JobTitlesResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(JobTitle.class, request, response, new JobTitleResource(request, response), JobTitleService.class);
	}

	static public class JobTitleResource extends BaseOptionResource<JobTitle, JobTitleService> {
		public JobTitleResource(HttpServletRequest request, HttpServletResponse response) {
			super(JobTitle.class, request, response);
		}

		@GET
		@Path("role")
		@Produces(MediaType.APPLICATION_JSON)
		public Set<Role> getRoles() {
			if (getId() != -1) {
				return getService().getRef(getId()).getRoles();
			} else {
				return new HashSet<>();
			}
		}

		@POST
		@Path("role/{roleId}")
		public void addRole(@PathParam("roleId") int roleId) throws Exception {
			getService().startTransaction();
			getService().addRole(getId(), roleId);
		}

		@DELETE
		@Path("role/{roleId}")
		public void removeRole(@PathParam("roleId") int roleId) throws Exception {
			getService().startTransaction();
			getService().removeRole(getId(), roleId);
		}

		@Path("approbationAmount")
		public JobTitleApprobationAmountsResource getJobTitleApprobationAmounts() {
			return new JobTitleApprobationAmountsResource(request, response, getId());
		}

		@Path("info")
		public JobTitleInfosResource getJobTitleInfosResource() {
			return new JobTitleInfosResource(request, response, getId());
		}
	}
}
