package com.sgitmanagement.expressoext.security;

import java.util.HashSet;
import java.util.Set;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.security.ApplicationsResource.ApplicationResource;

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

@Path("/application")
public class ApplicationsResource extends BaseEntitiesResource<Application, ApplicationService, ApplicationResource> {
	public ApplicationsResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Application.class, request, response, new ApplicationResource(request, response), ApplicationService.class);
	}

	static public class ApplicationResource extends BaseEntityResource<Application, ApplicationService> {
		public ApplicationResource(HttpServletRequest request, HttpServletResponse response) {
			super(Application.class, request, response);
		}

		@GET
		@Path("role")
		@Produces(MediaType.APPLICATION_JSON)
		public Set<Role> getRoles() {
			if (getId() != -1) {
				Application application = getService().getRef(getId());
				return application.getRoles();
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