package com.sgitmanagement.expressoext.security;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;

import com.sgitmanagement.expresso.base.PersistenceManager;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expressoext.base.BaseEntityService;

public class ResourceService extends BaseEntityService<Resource> {
	@Override
	protected Filter getSearchFilter(String term) {
		Filter filter = new Filter(Logic.or);
		filter.addFilter(new Filter("name", Operator.contains, term));
		filter.addFilter(new Filter("path", Operator.contains, term));
		return filter;
	}

	/**
	 * Get the resource for the name
	 *
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public Resource get(String name) throws Exception {
		return get(new Filter("name", name));
	}

	public static void main(String[] args) throws Exception {
		EntityManager em = PersistenceManager.getInstance().getEntityManager();

		ResourceService service = newServiceStatic(ResourceService.class, Resource.class);

		// System.out.println(service.list());

		// set the name to the resourceName (class)
		for (Resource resource : service.list()) {
			String name = resource.getPath();
			if (!name.equals("document")) {
				Resource mr = resource.getMasterResource();
				while (mr != null) {
					name = mr.getPath() + StringUtils.capitalize(name);
					mr = mr.getMasterResource();
				}
			}

			resource.setName(name);
		}

		PersistenceManager.getInstance().commitAndClose(em);
		System.out.println("Done");
	}
}