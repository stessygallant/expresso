package com.sgitmanagement.expressoext.security;

import java.util.List;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expressoext.base.BaseOptionService;

public class JobTypeService extends BaseOptionService<JobType> {

	public void addRole(int id, int roleId) throws Exception {
		JobType jobType = get(id);
		Role role = getEntityManager().find(Role.class, roleId);

		// only admin can add reserved role
		if (role.isSystemRole() && !isUserAdmin()) {
			throw new ForbiddenException();
		}

		jobType.getRoles().add(role);

		// for each user with this job type, add the userInfo
		UserService userService = newService(UserService.class, User.class);
		List<User> users = userService.list(new Filter("jobTitle.jobTypeId", id));
		for (User user : users) {
			// add the role info to the user
			userService.addRoleInfo(user, role);
		}
	}

	public void removeRole(int id, int roleId) throws Exception {
		JobType jobType = get(id);
		Role role = getEntityManager().find(Role.class, roleId);
		jobType.getRoles().remove(role);

		// for each user with this job type, add the userInfo
		UserService userService = newService(UserService.class, User.class);
		List<User> users = userService.list(new Filter("jobTitle.jobTypeId", id));
		for (User user : users) {
			// remove the role info to the user
			userService.removeRoleInfo(user, role);
		}
	}
}
