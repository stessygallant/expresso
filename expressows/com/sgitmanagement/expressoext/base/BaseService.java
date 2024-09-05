package com.sgitmanagement.expressoext.base;

import java.util.List;

import com.sgitmanagement.expresso.base.AbstractBaseService;
import com.sgitmanagement.expressoext.security.AuthorizationHelper;
import com.sgitmanagement.expressoext.security.BasicUser;

public class BaseService extends AbstractBaseService<BasicUser> {

	@Override
	final public boolean isUserInRole(String rolePgmKey) {
		return AuthorizationHelper.isUserInRole(getUser(), rolePgmKey);
	}

	/**
	 * Verify if the user is in the role, but do not include admin user if they are not in role
	 * 
	 * @param rolePgmKey
	 * @return
	 */
	final public boolean isUserInRoleNoAdmin(String rolePgmKey) {
		return AuthorizationHelper.isUserInRole(getUser(), rolePgmKey, false);
	}

	@Override
	final public boolean isUserAdmin() {
		return AuthorizationHelper.isUserAdmin(getUser());
	}

	@Override
	final public boolean isUserAllowed(String action, List<String> resources) {
		return AuthorizationHelper.isUserAllowed(getUser(), action, resources);
	}

	@Override
	final public BasicUser getUser(String userName) {
		return AuthorizationHelper.getUser(userName);
	}

	@Override
	final public BasicUser getSystemUser() {
		return AuthorizationHelper.getSystemUser();
	}

	@Override
	final public BasicUser getPublicUser() {
		return AuthorizationHelper.getPublicUser();
	}
}
