package com.sgitmanagement.expressoext.security;

import java.util.List;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expressoext.base.BaseEntityService;

public class RoleInfoService extends BaseEntityService<RoleInfo> {

	@Override
	public RoleInfo create(RoleInfo e) throws Exception {
		RoleInfo roleInfo = super.create(e);

		// when a role info is created, we need to add the user info to each user with this role
		UserService userService = newService(UserService.class, User.class);
		List<User> users = userService.getUsersInRole(roleInfo.getRoleId());
		for (User user : users) {
			userService.addRoleInfo(user, roleInfo);
		}
		return roleInfo;
	}

	@Override
	protected Filter getRestrictionsFilter() {
		return null;
	}
}
