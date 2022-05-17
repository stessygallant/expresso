package com.sgitmanagement.expressoext.base;

import java.util.Date;

import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;

public class BaseOptionService<E extends BaseOption> extends BaseDeactivableEntityService<E> {
	final public static int MAX_SEARCH_RESULTS = 50;

	@Override
	public E create(E e) throws Exception {
		// if there is no description, use the pgmkey
		if (e.getPgmKey() != null && e.getPgmKey().trim().length() == 0) {
			e.setPgmKey(null);
		}
		if (e.getDescription() == null) {
			e.setDescription(e.getPgmKey());
		}
		return super.create(e);
	}

	@Override
	public E update(E v) throws Exception {
		if (v.getPgmKey() != null && v.getPgmKey().trim().length() == 0) {
			v.setPgmKey(null);
		}
		if (v.getDescription() == null) {
			v.setDescription(v.getPgmKey());
		}
		return super.update(v);
	}

	/**
	 * Get the entity (option) from the pgmKey
	 *
	 * @param pgmKey
	 * @return
	 * @throws Exception
	 */
	public E get(String pgmKey) throws Exception {
		return get(new Filter("pgmKey", pgmKey));
	}

	@Override
	protected Query.Sort[] getDefaultQuerySort() {
		return new Query.Sort[] { new Query.Sort("sortOrder", Query.Sort.Direction.asc), new Query.Sort("description", Query.Sort.Direction.asc) };
	}

	@Override
	protected Filter getSearchFilter(String term) {
		Filter filter = new Filter(Logic.or);
		filter.addFilter(new Filter("description", Operator.contains, term));
		filter.addFilter(new Filter("pgmKey", Operator.contains, term));
		return filter;
	}

	@Override
	public void delete(Integer id) throws Exception {
		E e = get(id);
		e.setDeactivationDate(new Date());
	}
}
