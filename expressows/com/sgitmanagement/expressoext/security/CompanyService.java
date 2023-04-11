package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expressoext.base.BaseEntityService;

public class CompanyService extends BaseEntityService<Company> {

	@Override
	protected Filter getSearchFilter(String term) {
		Filter filter = new Filter(Logic.or);
		filter.addFilter(new Filter("name", Operator.contains, term));
		filter.addFilter(new Filter("billingCode", Operator.contains, term));
		return filter;
	}

	public static void main(String[] args) throws Exception {
		CompanyService service = newServiceStatic(CompanyService.class, Company.class);
		service.sync();
		service.closeServices();

		System.out.println("Done");
	}
}
