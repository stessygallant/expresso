package com.sgitmanagement.expressoext.security;

import java.util.ArrayList;
import java.util.Collection;
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
import com.sgitmanagement.expressoext.security.RolesResource.RoleResource;

@Path("/role")
public class RolesResource extends BaseOptionsResource<Role, RoleService, RoleResource> {

	public RolesResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Role.class, request, response, new RoleResource(request, response), RoleService.class);
	}

	static public class RoleResource extends BaseOptionResource<Role, RoleService> {
		public RoleResource(HttpServletRequest request, HttpServletResponse response) {
			super(Role.class, request, response);
		}

		@GET
		@Path("privilege")
		@Produces(MediaType.APPLICATION_JSON)
		public Set<Privilege> getPrivileges() {
			if (getId() != -1) {
				Role role = getService().getRef(getId());
				return role.getPrivileges();

			} else {
				return new HashSet<>();
			}
		}

		@POST
		@Path("privilege/{privilegeId}")
		public void addPrivilege(@PathParam("privilegeId") int privilegeId) throws Exception {
			try {
				getPersistenceManager().startTransaction(getEntityManager());
				getService().addPrivilege(getId(), privilegeId);
			} catch (Exception ex) {
				getPersistenceManager().rollback(getEntityManager());
				throw ex;
			} finally {
				getPersistenceManager().commit(getEntityManager());
			}
		}

		@DELETE
		@Path("privilege/{privilegeId}")
		public void removePrivilege(@PathParam("privilegeId") int privilegeId) throws Exception {
			try {
				getPersistenceManager().startTransaction(getEntityManager());
				getService().removePrivilege(getId(), privilegeId);
			} catch (Exception ex) {
				getPersistenceManager().rollback(getEntityManager());
				throw ex;
			} finally {
				getPersistenceManager().commit(getEntityManager());
			}
		}

		@GET
		@Path("application")
		@Produces(MediaType.APPLICATION_JSON)
		public Collection<Application> getApplications() {
			if (getId() != -1) {
				Role role = getService().getRef(getId());
				return role.getApplications();
			} else {
				return new ArrayList<>();
			}
		}

		@POST
		@Path("application/{applicationId}")
		public void addApplication(@PathParam("applicationId") int applicationId) throws Exception {
			try {
				getPersistenceManager().startTransaction(getEntityManager());

				getService().addApplication(getId(), applicationId);
			} catch (Exception ex) {
				getPersistenceManager().rollback(getEntityManager());
				throw ex;
			} finally {
				getPersistenceManager().commit(getEntityManager());
			}

		}

		@DELETE
		@Path("application/{applicationId}")
		public void removeApplication(@PathParam("applicationId") int applicationId) throws Exception {
			try {
				getPersistenceManager().startTransaction(getEntityManager());

				getService().removeApplication(getId(), applicationId);
			} catch (Exception ex) {
				getPersistenceManager().rollback(getEntityManager());
				throw ex;
			} finally {
				getPersistenceManager().commit(getEntityManager());
			}
		}

		@Path("info")
		public RoleInfosResource getInfos() {
			return new RoleInfosResource(request, response, getId());
		}
	}
}