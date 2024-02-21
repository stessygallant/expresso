package com.sgitmanagement.expressoext.util;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expressoext.base.BaseEntityService;

public class ConfigService extends BaseEntityService<Config> {

	public Config get(String key) throws Exception {
		return get(new Filter("key", key));
	}
}
