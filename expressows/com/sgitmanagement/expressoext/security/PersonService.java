package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expressoext.util.MainUtil;

public class PersonService extends BasePersonService<Person> {

	public static void main(String[] args) throws Exception {
		PersonService service = newServiceStatic(PersonService.class, Person.class);
		System.out.println(service.getPersonsUnder(service.get(34)));

		MainUtil.close();
	}

	@Override
	protected Filter getSearchFilter(String term) {
		Filter filter = new Filter(Logic.or);
		filter.addFilter(new Filter("fullName", Operator.contains, term));
		return filter;
	}
}
