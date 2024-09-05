package com.sgitmanagement.expressoext.base;

import java.util.List;

import com.sgitmanagement.expresso.base.AbstractBaseEntityService;
import com.sgitmanagement.expressoext.security.AuthorizationHelper;
import com.sgitmanagement.expressoext.security.BasicUser;

abstract public class AbstractOuterEntityService<E extends AbstractOuterEntity<I>, I> extends AbstractBaseEntityService<E, BasicUser, I> {
	@Override
	final public boolean isUserInRole(String rolePgmKey) {
		return AuthorizationHelper.isUserInRole(getUser(), rolePgmKey);
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
