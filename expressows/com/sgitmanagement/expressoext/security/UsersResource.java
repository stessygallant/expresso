package com.sgitmanagement.expressoext.security;

import java.util.List;
import java.util.Set;

import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expressoext.base.BaseDeactivableEntityResource;
import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.security.UsersResource.UserResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

@Path("/user")
public class UsersResource extends BaseEntitiesResource<User, UserService, UserResource> {
	public UsersResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(User.class, request, response, new UserResource(request, response), UserService.class);
	}

	@GET
	@Path("me")
	@Produces(MediaType.APPLICATION_JSON)
	public User getMyUser() {
		return getUser();
	}

	@GET
	@Path("inrole")
	@Produces(MediaType.APPLICATION_JSON)
	public List<User> getUsersInRole(@QueryParam("roleId") int roleId) throws Exception {
		return getService().getUsersInRole(roleId);
	}

	static public class UserResource extends BaseDeactivableEntityResource<User, UserService> {
		public UserResource(HttpServletRequest request, HttpServletResponse response) {
			super(User.class, request, response);
		}

		@GET
		@Path("role")
		@Produces(MediaType.APPLICATION_JSON)
		public Set<Role> getRoles() {
			if (getUser().getId().equals(getId()) || getService().isUserAdmin() || getService().isUserInRole("UserManager.user")) {
				return getService().getRoles(getId());
			} else {
				throw new ForbiddenException();
			}
		}

		@GET
		@Path("allroles")
		@Produces(MediaType.APPLICATION_JSON)
		public Set<Role> getAllRoles() {
			if (getUser().getId().equals(getId()) || getService().isUserAdmin() || getService().isUserInRole("UserManager.user")) {
				return getService().getAllRoles(getId());
			} else {
				throw new ForbiddenException();
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

		@GET
		@Path("privilege")
		@Produces(MediaType.APPLICATION_JSON)
		public List<Privilege> getPrivileges() throws Exception {
			if (getUser().getId().equals(getId()) || getService().isUserAdmin() || getService().isUserInRole("UserManager.user")) {
				return getService().getPrivileges(getId());
			} else {
				throw new ForbiddenException();
			}
		}

		@GET
		@Path("application")
		@Produces(MediaType.APPLICATION_JSON)
		public List<Application> getAllApplications() throws Exception {
			if (getUser().getId().equals(getId()) || getService().isUserAdmin() || getService().isUserInRole("UserManager.user")) {
				return getService().getAllApplications(getId());
			} else {
				throw new ForbiddenException();
			}
		}

		@Path("info")
		public UserInfosResource getInfos() {
			return new UserInfosResource(request, response, getId());
		}

		@Path("config")
		public UserConfigsResource getConfigs() {
			return new UserConfigsResource(request, response, getId());
		}

		@Path("preference")
		public UserPreferencesResource getPreferences() {
			return new UserPreferencesResource(request, response, getId());
		}

		public void send(MultivaluedMap<String, String> formParams) throws Exception {
			User user = get(getId());
			getService().sendWelcomeEmail(user);
		}

		public User unlock(MultivaluedMap<String, String> formParams) throws Exception {
			User user = get(getId());
			return getService().unlock(user);
		}
	}
}
