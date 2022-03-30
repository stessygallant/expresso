package com.sgitmanagement.expressoext.security;

import java.util.List;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expressoext.base.BaseOptionService;

public class RoleService extends BaseOptionService<Role> {

	@Override
	public void delete(Integer id) throws Exception {
		if (id == Role.R.USER.getId()) {
			// we cannot delete this role
			throw new RuntimeException("Cannot delete the User role");
		} else {
			super.delete(id);
		}
	}

	public void addPrivilege(int id, int privilegeId) {
		Role role = get(id);
		PrivilegeService privilegeService = newService(PrivilegeService.class, Privilege.class);
		Privilege privilege = privilegeService.getRef(privilegeId);
		role.getPrivileges().add(privilege);
	}

	public void removePrivilege(int id, int privilegeId) {
		Role role = get(id);
		PrivilegeService privilegeService = newService(PrivilegeService.class, Privilege.class);
		Privilege privilege = privilegeService.getRef(privilegeId);
		role.getPrivileges().remove(privilege);
	}

	public void addApplication(int id, int applicationId) throws Exception {
		ApplicationService applicationService = newService(ApplicationService.class, Application.class);
		Application application = applicationService.get(applicationId);
		Role role = get(id);
		role.getApplications().add(application);

		// when a role get access to an application, it gets access to all READ action for all resources
		// that are linked to the application
		PrivilegeService privilegeService = newService(PrivilegeService.class, Privilege.class);

		@SuppressWarnings("unchecked")
		List<Resource> resources = newService(BaseOptionService.class, Resource.class).list(new Filter("applicationId", applicationId));
		for (Resource res : resources) {
			Privilege p = privilegeService.get(Action.R.READ.getId(), res.getId());
			if (p != null) {
				logger.debug("Adding READ to " + role.getPgmKey() + " on " + res.getLabel());
				role.getPrivileges().add(p);
			}
		}
	}

	public void removeApplication(int id, int applicationId) {
		Role role = get(id);
		Application application = getEntityManager().getReference(Application.class, applicationId);
		role.getApplications().remove(application);
	}

	@Override
	protected Filter getRestrictionsFilter() {
		if (isUserInRole("admin")) {
			// admin can see all roles
			return null;
		} else {
			// otherwise they cannot see reserved role
			Filter filter = new Filter();
			filter.addFilter(new Filter("systemRole", false));
			return filter;
		}
	}
}
