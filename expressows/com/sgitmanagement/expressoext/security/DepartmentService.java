package com.sgitmanagement.expressoext.security;

import java.util.List;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expressoext.base.BaseOptionService;

public class DepartmentService extends BaseOptionService<Department> {

	public void addRole(int id, int roleId) throws Exception {
		Department department = get(id);
		Role role = getEntityManager().find(Role.class, roleId);

		// only admin can add reserved role
		if (role.isSystemRole() && !isUserAdmin()) {
			throw new ForbiddenException();
		}

		department.getRoles().add(role);

		// for each user with this department, add the userInfo
		UserService userService = newService(UserService.class, User.class);
		List<User> users = userService.list(new Filter("departmentId", id));
		for (User user : users) {
			// add the role info to the user
			userService.addRoleInfo(user, role);
		}

	}

	public void removeRole(int id, int roleId) throws Exception {
		Department department = get(id);
		Role role = getEntityManager().find(Role.class, roleId);
		department.getRoles().remove(role);

		// for each user with this department, add the userInfo
		UserService userService = newService(UserService.class, User.class);
		List<User> users = userService.list(new Filter("departmentId", id));
		for (User user : users) {
			// remove the role info to the user
			userService.removeRoleInfo(user, role);
		}
	}
}
