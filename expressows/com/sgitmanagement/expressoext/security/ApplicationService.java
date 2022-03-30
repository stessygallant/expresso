package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expressoext.base.BaseEntityService;

public class ApplicationService extends BaseEntityService<Application> {

	public void addRole(int id, int roleId) {
		Application application = get(id);
		Role role = getEntityManager().find(Role.class, roleId);
		application.getRoles().add(role);
	}

	public void removeRole(int id, int roleId) {
		Application application = get(id);
		Role role = getEntityManager().find(Role.class, roleId);
		application.getRoles().remove(role);
	}

	@Override
	protected Filter getSearchFilter(String term) {
		Filter filter = new Filter(Logic.or);
		filter.addFilter(new Filter("description", Operator.contains, term));
		filter.addFilter(new Filter("pgmKey", Operator.contains, term));
		return filter;
	}
}
