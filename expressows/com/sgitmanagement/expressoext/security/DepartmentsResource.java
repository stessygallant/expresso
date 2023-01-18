package com.sgitmanagement.expressoext.security;

import java.util.HashSet;
import java.util.Set;

import com.sgitmanagement.expressoext.base.BaseOptionResource;
import com.sgitmanagement.expressoext.base.BaseOptionsResource;
import com.sgitmanagement.expressoext.security.DepartmentsResource.DepartmentResource;

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

@Path("/{department:(?i)department}")
public class DepartmentsResource extends BaseOptionsResource<Department, DepartmentService, DepartmentResource> {
	public DepartmentsResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Department.class, request, response, new DepartmentResource(request, response), DepartmentService.class);
	}

	static public class DepartmentResource extends BaseOptionResource<Department, DepartmentService> {
		public DepartmentResource(HttpServletRequest request, HttpServletResponse response) {
			super(Department.class, request, response);
		}

		@GET
		@Path("{role:(?i)role}")
		@Produces(MediaType.APPLICATION_JSON)
		public Set<Role> getRoles() {
			if (getId() != -1) {
				Department department = getService().getRef(getId());
				return department.getRoles();
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