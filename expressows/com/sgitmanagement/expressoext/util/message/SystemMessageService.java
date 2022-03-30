package com.sgitmanagement.expressoext.util.message;

import java.util.Date;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expressoext.base.BaseEntityService;

public class SystemMessageService extends BaseEntityService<SystemMessage> {

	@Override
	protected Filter getActiveOnlyFilter() throws Exception {
		Filter filter = new Filter(Logic.or);
		filter.addFilter(new Filter("endDate", Operator.isNull));
		filter.addFilter(new Filter("endDate", Operator.gt, new Date()));
		return filter;
	}
}
