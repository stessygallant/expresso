package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expressoext.base.BaseEntityService;
import com.sgitmanagement.expressoext.util.MainUtil;

public class BlockedIPAddressService extends BaseEntityService<BlockedIPAddress> {

	@Override
	protected Filter getSearchFilter(String term) {
		Filter filter = new Filter(Logic.or);
		filter.addFilter(new Filter("ip", Operator.contains, term));
		return filter;
	}

	public static void main(String[] args) throws Exception {
		BlockedIPAddressService service = newServiceStatic(BlockedIPAddressService.class, BlockedIPAddress.class);
		service.list();
		MainUtil.close();
	}
}
