package com.sgitmanagement.expressoext.security;

import jakarta.persistence.EntityManager;

import com.sgitmanagement.expresso.base.PersistenceManager;
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
		EntityManager em = PersistenceManager.getInstance().getEntityManager();

		CompanyService service = newServiceStatic(CompanyService.class, Company.class);
		service.sync();

		PersistenceManager.getInstance().commitAndClose(em);

		System.out.println("Done");
	}
}
