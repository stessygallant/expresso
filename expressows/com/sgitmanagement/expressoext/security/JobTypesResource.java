package com.sgitmanagement.expressoext.security;

import java.util.HashSet;
import java.util.Set;

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

import com.sgitmanagement.expressoext.base.BaseOptionResource;
import com.sgitmanagement.expressoext.base.BaseOptionsResource;
import com.sgitmanagement.expressoext.security.JobTypesResource.JobTypeResource;

@Path("{jobtype:(?i)jobtype}")
public class JobTypesResource extends BaseOptionsResource<JobType, JobTypeService, JobTypeResource> {

	public JobTypesResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(JobType.class, request, response, new JobTypeResource(request, response), JobTypeService.class);
	}

	static public class JobTypeResource extends BaseOptionResource<JobType, JobTypeService> {

		public JobTypeResource(HttpServletRequest request, HttpServletResponse response) {
			super(JobType.class, request, response);
		}

		@GET
		@Path("{role:(?i)role}")
		@Produces(MediaType.APPLICATION_JSON)
		public Set<Role> getRoles() {
			if (getId() != -1) {
				JobType jobType = getService().getRef(getId());
				return jobType.getRoles();
			} else {
				return new HashSet<>();
			}
		}

		@POST
		@Path("role/{roleId}")
		public void addRole(@PathParam("roleId") int roleId) throws Exception {
			try {
				getPersistenceManager().startTransaction(getEntityManager());
				getService().addRole(getId(), roleId);
			} catch (Exception ex) {
				getPersistenceManager().rollback(getEntityManager());
				throw ex;
			} finally {
				getPersistenceManager().commit(getEntityManager());
			}

		}

		@DELETE
		@Path("role/{roleId}")
		public void removeRole(@PathParam("roleId") int roleId) throws Exception {
			try {
				getPersistenceManager().startTransaction(getEntityManager());
				getService().removeRole(getId(), roleId);
			} catch (Exception ex) {
				getPersistenceManager().rollback(getEntityManager());
				throw ex;
			} finally {
				getPersistenceManager().commit(getEntityManager());
			}

		}
	}
}