package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Sort;
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

	@Override
	protected Sort[] getDefaultQuerySort() {
		return new Query.Sort[] { new Query.Sort("systemRole", Query.Sort.Direction.asc), new Query.Sort("pgmKey", Query.Sort.Direction.asc) };

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

	public static void main(String[] args) throws Exception {
		RoleService service = newServiceStatic(RoleService.class, Role.class);
		service.list().forEach(r -> System.out.println(r.getPgmKey()));
		System.out.println("Done");
	}
}
