package com.sgitmanagement.expressoext.security;

import java.util.HashSet;
import java.util.Set;

import com.sgitmanagement.expressoext.base.BaseOptionResource;
import com.sgitmanagement.expressoext.base.BaseOptionsResource;
import com.sgitmanagement.expressoext.security.RolesResource.RoleResource;

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
import jakarta.ws.rs.core.MultivaluedMap;

@Path("/role")
public class RolesResource extends BaseOptionsResource<Role, RoleService, RoleResource> {

	public RolesResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Role.class, request, response, new RoleResource(request, response), RoleService.class);
	}

	public void reset(MultivaluedMap<String, String> formParams) throws Exception {
		AuthorizationHelper.clearCache();
	}

	static public class RoleResource extends BaseOptionResource<Role, RoleService> {
		public RoleResource(HttpServletRequest request, HttpServletResponse response) {
			super(Role.class, request, response);
		}

		@Path("info")
		public RoleInfosResource getInfos() {
			return new RoleInfosResource(request, response, getId());
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
			getService().startTransaction();
			getService().addPrivilege(getId(), privilegeId);
		}

		@DELETE
		@Path("privilege/{privilegeId}")
		public void removePrivilege(@PathParam("privilegeId") int privilegeId) throws Exception {
			getService().startTransaction();
			getService().removePrivilege(getId(), privilegeId);
		}
	}
}