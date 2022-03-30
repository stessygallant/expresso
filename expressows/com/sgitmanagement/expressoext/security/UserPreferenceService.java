package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expressoext.base.BaseEntityService;

public class UserPreferenceService extends BaseEntityService<UserPreference> {
	@Override
	protected Filter getRestrictionsFilter() {
		if (isUserAdmin()) {
			// ok can see all
			return null;
		} else {
			// A user can only access his own preferences
			return new Filter("userId", getUser().getId());
		}
	}

	@Override
	public void verifyActionRestrictions(String action, UserPreference userPreference) {
		if (!AuthorizationHelper.isUserInRole(getUser(), "UserManager.user")) {
			if (!getUser().getId().equals(userPreference.getUserId())) {
				throw new ForbiddenException();
			}
		}
	}
}
