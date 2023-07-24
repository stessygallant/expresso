package com.sgitmanagement.expressoext.filter;

import com.sgitmanagement.expressoext.security.User;

public interface AutoCreatableUser {
	public User create(String userName) throws Exception;
}
