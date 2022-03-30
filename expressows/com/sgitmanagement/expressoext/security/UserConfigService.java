package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expressoext.base.BaseEntityService;

public class UserConfigService extends BaseEntityService<UserConfig> {

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
}
