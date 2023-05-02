package com.sgitmanagement.expressoext.security;

import java.util.List;

import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expressoext.base.BaseDeactivableEntityService;

public class BasePersonService<E extends Person> extends BaseDeactivableEntityService<E> {

	@Override
	protected Query.Sort[] getDefaultQuerySort() {
		return new Query.Sort[] { new Query.Sort("lastName", Query.Sort.Direction.asc), new Query.Sort("firstName", Query.Sort.Direction.asc) };
	}

	@Override
	protected Filter getRestrictionsFilter() {
		if (isUserInRole("UserManager.user") || isUserInRole("UserManager.viewAllUsers")) {
			// admin can see all
			return null;
		} else if (getUser().getCompanyId() != null) {
			// they are limited to see their own company
			return new Filter("companyId", getUser().getCompanyId());
		} else {
			// they are limited to see their own user
			return new Filter("id", getUser().getId());
		}
	}

	/**
	 * 
	 * @param managerPerson
	 * @return
	 * @throws Exception
	 */
	public List<E> getPersonsUnder(E managerPerson) throws Exception {
		// get all persons under
		Query query = new Query().setActiveOnly(true);
		query.setHierarchical(true);
		query.setAppendHierarchicalParents(false);
		query.addFilter(new Filter("managerPersonId", managerPerson.getId()));
		return list(query);
	}
}
