package com.sgitmanagement.expresso.security;

import java.util.List;

import com.sgitmanagement.expresso.base.IUser;

public interface Authorizable<U extends IUser> {
	public boolean isUserAdmin();

	public boolean isUserInRole(String role);

	public boolean isUserAllowed(String action, List<String> resources);

	public U getUser(String userName);

	public U getSystemUser();

	public U getPublicUser();
}
