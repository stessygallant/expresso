package com.sgitmanagement.expresso.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;

public class Query {
	private Integer skip;
	private Integer pageSize;
	private List<Sort> sort;
	private Filter filter;

	private Boolean activeOnly;
	private Boolean countOnly;
	private boolean createIfNotFound;

	private String searchFilterTerm;
	private boolean keySearch;
	private boolean verified;

	public Query() {
		super();
	}

	public Query(Filter filter) {
		this();
		this.filter = filter;
	}

	public Integer getSkip() {
		return skip;
	}

	public Query setSkip(Integer skip) {
		this.skip = skip;
		return this;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public Query setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	public List<Sort> getSort() {
		return sort;
	}

	public Query setSort(List<Sort> sort) {
		this.sort = sort;
		return this;
	}

	public Query addSort(String field) {
		return addSort(new Sort(field));
	}

	public Query addSort(Sort s) {
		if (this.sort == null) {
			this.sort = new ArrayList<>();
		}
		this.sort.add(s);
		return this;
	}

	public Filter getFilter() {
		return this.filter;
	}

	public Query setFilter(Filter filter) {
		this.filter = filter;
		return this;
	}

	public Query addFilter(Filter filter) {
		if (filter != null) {
			if (this.filter == null) {
				this.filter = filter;
			} else {
				// if there is already a filter, make sure that the logic is AND
				// or otherwise move it to a sub filter
				if (this.filter.getField() != null || (this.filter.filters != null && this.filter.getLogic().equals(Logic.or))) {
					Filter previous = this.filter;
					this.filter = new Filter();
					this.filter.addFilter(previous);
					this.filter.addFilter(filter);
				} else {
					this.filter.addFilter(filter);
				}
			}
		}
		return this;
	}

	public boolean hasFilters() {
		return filter != null && ((filter.filters != null && filter.filters.size() > 0) || filter.field != null);
	}

	public boolean hasSort() {
		return sort != null && !sort.isEmpty();
	}

	public boolean isKeySearch() {
		return keySearch;
	}

	public Query setKeySearch(boolean keySearch) {
		this.keySearch = keySearch;
		return this;
	}

	public boolean isVerified() {
		return verified;
	}

	public Query setVerified(boolean verified) {
		this.verified = verified;
		return this;
	}

	public boolean isCreateIfNotFound() {
		return createIfNotFound;
	}

	public Query setCreateIfNotFound(boolean createIfNotFound) {
		this.createIfNotFound = createIfNotFound;
		return this;
	}

	/**
	 * Get the first filter that matches the field
	 *
	 * @param field
	 * @return
	 */
	public Filter getFilter(String field) {
		List<Filter> list = getFilters(field);
		if (list == null || list.isEmpty()) {
			return null;
		} else {
			return list.get(0);
		}
	}

	/**
	 * Get all filters that match the field
	 *
	 * @param field
	 * @return
	 */
	public List<Filter> getFilters(String field) {
		return getFilters(field, false);
	}

	/**
	 *
	 * @param field
	 * @param startsWith
	 * @return
	 */
	public List<Filter> getFilters(String field, boolean startsWith) {
		List<Filter> list = null;
		if (filter != null) {
			list = new ArrayList<>();
			filter.getFilters(list, field, startsWith);
		}
		return list;
	}

	/**
	 * Remove any filters with the field
	 *
	 * @param field
	 */
	public void removeFilter(String field) {
		if (this.filter != null) {
			if (removeFilter(this.filter, field)) {
				this.filter = null;
			}
		}
	}

	/**
	 * Remove any filters with the field
	 *
	 * @param field
	 */
	private boolean removeFilter(Filter filter, String field) {
		if (filter.getField() != null && filter.getField().equals(field)) {
			return true;
		} else if (filter.filters != null) {
			for (Filter p : new ArrayList<>(filter.filters)) {
				if (removeFilter(p, field)) {
					filter.filters.remove(p);
				}
			}
		}
		return false;
	}

	/**
	 *
	 * @return true if we want active only resources, false otherwise
	 */
	public boolean activeOnly() {
		return this.activeOnly == null ? false : this.activeOnly;
	}

	public boolean isActiveOnlySet() {
		return this.activeOnly != null;
	}

	public Query setActiveOnly(boolean activeOnly) {
		this.activeOnly = activeOnly;
		return this;
	}

	/**
	 * Because in the UI there is sometimes a problem to set the activeOnly flag on the query, we need to set it at the filter level. Then this method will verify it and fix the query accordingly
	 */
	public void verifyFlagsOnFilter() {
		Filter createIfNotFoundFilter = getFilter("createIfNotFound");
		if (createIfNotFoundFilter != null) {
			this.createIfNotFound = (Boolean) createIfNotFoundFilter.getValue();

			// remove the createIfNotFound filter
			removeFilter(this.filter, "createIfNotFound");
		}

		Filter activeOnlyFilter = getFilter("activeOnly");
		if (activeOnlyFilter != null) {
			this.activeOnly = (Boolean) activeOnlyFilter.getValue();

			// remove the activeOnly filter
			removeFilter(this.filter, "activeOnly");
		}
	}

	/**
	 * by default, we want data and total.
	 *
	 * @return true if we want only total, false otherwise
	 */
	public boolean countOnly() {
		return this.countOnly == null ? false : this.countOnly;
	}

	public Query setCountOnly(boolean countOnly) {
		this.countOnly = countOnly;
		return this;
	}

	public String getSearchFilterTerm() {
		if (this.searchFilterTerm == null) {
			// search filter could be at several places
			// bring it back to the query attribute
			if (filter != null && filter.searchFilterTerm != null) {
				this.searchFilterTerm = filter.searchFilterTerm;
			} else if (filter != null && getFilter("searchFilterTerm") != null) {
				this.searchFilterTerm = (String) getFilter("searchFilterTerm").getValue();
				removeFilter("searchFilterTerm");
			}
		}

		return this.searchFilterTerm;
	}

	public void setSearchFilterTerm(String searchFilterTerm) {
		this.searchFilterTerm = searchFilterTerm;
	}

	@Override
	public String toString() {
		return "Query [activeOnly=" + activeOnly + ", countOnly=" + countOnly + ", keySearch=" + keySearch + ", searchFilterTerm=" + searchFilterTerm + ", skip=" + skip + ", pageSize=" + pageSize
				+ ", sort=" + sort + ", filter=" + filter + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(filter);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Query other = (Query) obj;
		return Objects.equals(filter, other.filter);
	}

	/**
	 * Sort
	 */
	static public class Sort {
		private String field;

		public enum Direction {
			asc, desc
		}

		private Direction dir = Query.Sort.Direction.asc;

		public Sort() {
			super();
		}

		public Sort(String field, Direction dir) {
			this();
			this.field = field;
			this.dir = dir;
		}

		public Sort(String field) {
			this(field, Query.Sort.Direction.asc);
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}

		public Direction getDir() {
			return dir;
		}

		public void setDir(String dir) {
			this.setDir(Direction.valueOf(dir));
		}

		public void setDir(Direction dir) {
			this.dir = dir;
		}

		@Override
		public String toString() {
			return "Sort [field=" + field + ", dir=" + dir + "]";
		}

	}

	/*
	 * Filter
	 */
	static public class Filter {
		// this can be used only by the UI to set the flag on the Filter
		Boolean activeOnly;
		Boolean createIfNotFound;

		String searchFilterTerm;

		public enum Logic {
			and, or
		}

		// a filter could be a list of filters
		private Logic logic;
		private List<Filter> filters;

		public enum Operator {
			// string only
			startsWith, doesNotStartWith, endsWith, doesNotEndWith, contains, doesNotContain, isEmpty, isNotEmpty, isNotNullOrEmpty, isNullOrEmpty, trimCompare, neqTrimCompare, equalsIgnoreCase,

			// number and date only
			gt, lt, lte, gte,

			// date only
			truncGt, truncLt, truncLte, truncGte, // this will truncate the value in database before comparing
			timestampEquals, // this will compare with milliseconds. eq will compare same day only
			sameDayEquals, // use this instead of eq for date

			// commons
			eq, neq, isNotNull, isNull,

			// number and string only
			in,

			// reserved to framework
			@Deprecated
			equalsNoKey, @Deprecated
			containsNoKey,

			// for backward compatibility only (lower case)
			@Deprecated
			startswith, @Deprecated
			endswith, @Deprecated
			doesnotcontain, @Deprecated
			isempty, @Deprecated
			isnotempty, @Deprecated
			isnotnullorempty, @Deprecated
			isnullorempty, @Deprecated
			isnotnull, @Deprecated
			isnull, @Deprecated
			equalsnokey, @Deprecated
			containsnokey
		}

		private Operator operator;
		private JsonElement field;
		private Object value;

		public Filter() {
			super();
		}

		public Filter(Filter[] filters) {
			super();
			if (this.logic == null) {
				this.logic = Logic.and;
			}
			for (Filter f : filters) {
				addFilter(f);
			}
		}

		@Deprecated
		public Filter(String logic) {
			this();
			this.logic = Logic.valueOf(logic);
		}

		public Filter(Logic logic) {
			this();
			this.logic = logic;
		}

		public Filter(String field, Object value) {
			this();
			if (value instanceof Operator) {
				this.field = new JsonPrimitive(field);
				this.value = null;
				this.operator = (Operator) value;
			} else {
				this.field = new JsonPrimitive(field);
				this.value = value;
				this.operator = Operator.eq;
			}
		}

		@Deprecated
		public Filter(String field, String operator, Object value) {
			this(field, Operator.valueOf(operator), value);
		}

		public Filter(String field, Operator operator, Object value) {
			this();
			this.field = new JsonPrimitive(field);
			this.value = value;
			this.operator = operator;
		}

		public Logic getLogic() {
			// default to AND if not defined
			if (logic == null && filters != null) {
				logic = Logic.and;
			}
			return logic;
		}

		public void setLogic(String logic) {
			this.setLogic(Logic.valueOf(logic));
		}

		public void setLogic(Logic logic) {
			this.logic = logic;
		}

		public List<Filter> getFilters() {
			return filters;
		}

		public void setFilters(List<Filter> filters) {
			this.filters = filters;
		}

		public Filter addFilter(Filter filter) {
			if (filters == null) {
				filters = new ArrayList<>();
			}
			filters.add(filter);
			return this;
		}

		public Object getValue() {
			if (value == null && field != null && field instanceof JsonObject) {
				// get the value from the jsonobject (assume only 1 entry)
				for (Entry<String, JsonElement> entry : ((JsonObject) field).entrySet()) {
					return entry.getValue().getAsString();
				}
				return null;
			} else {
				return value;
			}
		}

		/**
		 * Get the value of the filter as a int. It will throw an exception if the value is null
		 *
		 * @return
		 */
		public int getIntValue() {
			Object value = getValue();
			String valueType = value.getClass().getSimpleName();

			Integer integerValue;
			if (valueType.equals("Integer")) {
				integerValue = (Integer) value;
			} else if (valueType.equals("Double")) {
				integerValue = ((Double) value).intValue();
			} else {
				String v = (String) value;
				if (v.indexOf('.') == -1) {
					integerValue = Integer.parseInt(v);
				} else {
					integerValue = (int) Float.parseFloat(v);
				}
			}
			return integerValue;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public String getField() {
			if (field == null) {
				return null;
			} else if (field instanceof JsonObject) {
				// get the value from the jsonobject (assume only 1 entry)
				for (Entry<String, JsonElement> entry : ((JsonObject) field).entrySet()) {
					return entry.getKey();
				}
				return null;
			} else {
				return field.getAsString();
			}
		}

		public void setField(String field) {
			setField(new JsonPrimitive(field));
		}

		public void setField(JsonElement field) {
			this.field = field;
		}

		public Operator getOperator() {
			return operator;
		}

		public void setOperator(String operator) {
			this.setOperator(Operator.valueOf(operator));
		}

		public void setOperator(Operator operator) {
			this.operator = operator;
		}

		/**
		 * Get the list of filters that match the field
		 *
		 * @param list
		 * @param field
		 */
		void getFilters(List<Filter> list, String field, boolean startsWith) {
			if (this.getField() != null && (this.getField().equals(field) || (startsWith && this.getField().startsWith(field)))) {
				list.add(this);
			} else if (this.filters != null) {
				for (Filter p : this.filters) {
					if (p != null) {
						p.getFilters(list, field, startsWith);
					}
				}
			}
		}

		@Override
		public String toString() {
			return "Filter [logic=" + logic + ", filters=" + filters + ", operator=" + operator + ", field=" + field + ", value=" + value + "]";
		}

		@Override
		public int hashCode() {
			return Objects.hash(field, filters, logic, operator, value);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Filter other = (Filter) obj;
			return Objects.equals(field, other.field) && Objects.equals(filters, other.filters) && logic == other.logic && operator == other.operator && Objects.equals(value, other.value);
		}
	}

	/**
	 * Parse the query string and return the Query object
	 *
	 * @param queryJSONString
	 * @return
	 */
	static public Query getQuery(String queryJSONString) {
		Query query = new Gson().fromJson(queryJSONString, Query.class);

		if (query == null) {
			query = new Query();
		}

		// patch: if the UI set the activeOnly flag on the Filter, move it to the Query
		query.verifyFlagsOnFilter();

		return query;
	}

	static public void main(String[] args) throws Exception {
		Query q = new Query();

		Filter f1 = new Filter("aaa", 1);
		q.addFilter(f1);

		Filter f2 = new Filter("bbb", 2);
		q.addFilter(f2);

		System.out.println(new Gson().toJson(q));
	}

}