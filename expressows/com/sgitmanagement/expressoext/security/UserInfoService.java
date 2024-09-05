package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseEntityService;

public class UserInfoService extends BaseEntityService<UserInfo> {

	@Override
	public UserInfo update(UserInfo v) throws Exception {
		if (v.getPassword() != null) {
			// special case: only set the password to the user if it is the same user
			if (v.getUserId() != null && v.getUserId().equals(getUser().getId())) {
				getUser().getExtended().setPassword(Util.hashPassword(v.getPassword()));
			}

			return v;
		} else {
			return super.update(v);
		}
	}

	@Override
	protected Filter getRestrictionsFilter() {
		if (isUserAdmin()) {
			// ok can see all
			return null;
		} else {
			// A user can only access his own config
			return new Filter("userId", getUser().getId());
		}
	}

	@Override
	public void verifyActionRestrictions(String action, UserInfo userInfo) {
		if (!AuthorizationHelper.isUserInRole(getUser(), "UserManager.user")) {
			if (!getUser().getId().equals(userInfo.getUserId())) {
				throw new ForbiddenException();
			}
		}
	}
}
