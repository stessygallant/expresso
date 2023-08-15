package com.sgitmanagement.expressoext.security;

import org.apache.commons.lang3.StringUtils;

import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expresso.dto.Query.Sort;
import com.sgitmanagement.expressoext.base.BaseEntityService;
import com.sgitmanagement.expressoext.util.MainUtil;

public class ResourceService extends BaseEntityService<Resource> {

	public static void main(String[] args) throws Exception {
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

		MainUtil.close();
	}

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

	@Override
	protected Sort[] getDefaultQuerySort() {
		return new Query.Sort[] { new Query.Sort("name", Query.Sort.Direction.asc) };
	}
}
