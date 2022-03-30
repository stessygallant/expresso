package com.sgitmanagement.expressoext.base;

import java.util.Date;

import com.sgitmanagement.expresso.base.Deactivable;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;

public class BaseDeactivableEntityService<E extends BaseEntity & Deactivable> extends BaseEntityService<E> {
	/**
	 *
	 * @param e
	 * @return
	 * @throws Exception
	 */
	public E activate(E e) throws Exception {
		e.setDeactivationDate(null);
		return update(e);
	}

	/**
	 *
	 * @param e
	 * @return
	 * @throws Exception
	 */
	@Override
	public E deactivate(E e) throws Exception {
		e.setDeactivationDate(new Date());
		return update(e);
	}

	@Override
	protected Filter getActiveOnlyFilter() throws Exception {
		// and remove deactivated option (deactivationDate)
		Filter filter = new Filter(Logic.or);
		filter.addFilter(new Filter("deactivationDate", null));
		filter.addFilter(new Filter("deactivationDate", Operator.gt, new Date()));
		return filter;
	}
}
