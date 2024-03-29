package com.sgitmanagement.expresso.base;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expresso.dto.SearchResult;
import com.sgitmanagement.expresso.dto.VirtualList;
import com.sgitmanagement.expresso.exception.BaseException;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expresso.exception.ValidationException;
import com.sgitmanagement.expresso.util.ProgressSender;
import com.sgitmanagement.expresso.util.Util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

/**
 * Methods allowed on a group of entities<br>
 * Ex: /workOrder<br>
 * GET - get the list of entities for the resource<br>
 * POST - create a new entity OR perform an action on this resource type (ex: process, sync, etc)<br>
 * PUT - not allowed<br>
 * DELETE - not allowed<br>
 */
public abstract class AbstractBaseEntitiesResource<E extends IEntity<I>, S extends AbstractBaseEntityService<E, U, I>, R extends AbstractBaseEntityResource<E, S, U, I>, U extends IUser, I>
		extends AbstractBaseResource<S, U> {
	public final static int MAX_LIST_RESULTS = 10000;
	public final static int MAX_HIERARCHICAL_RESULTS = 500;
	private AbstractBaseEntityResource<E, S, U, I> baseEntityResource;
	private Integer parentId;
	private Class<E> typeOfE;

	/**
	 * Must be used by primary resources
	 *
	 * @param typeOfE
	 * @param request
	 * @param response
	 * @param baseEntityResource
	 * @param serviceClass
	 */
	public AbstractBaseEntitiesResource(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response, R baseEntityResource, Class<S> serviceClass) {
		super(request, response, serviceClass);
		this.typeOfE = typeOfE;

		// keep a reference to the base entity and assign the same getService()
		this.baseEntityResource = baseEntityResource;
		this.baseEntityResource.setService(this.getService());
	}

	/**
	 * Must be used by sub resources
	 *
	 * @param typeOfE
	 * @param request
	 * @param response
	 * @param baseEntityResource
	 * @param serviceClass
	 * @param parentId
	 */
	public AbstractBaseEntitiesResource(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response, R baseEntityResource, Class<S> serviceClass, Integer parentId) {
		this(typeOfE, request, response, baseEntityResource, serviceClass);

		// remove this and use it from the calling base resource
		this.parentId = parentId;

		// if there is a parent, set it to the service
		getService().setParentId(getParentId());
	}

	@Deprecated
	public AbstractBaseEntitiesResource(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response, R baseEntityResource, Class<S> serviceClass, String parentName, Integer parentId) {
		this(typeOfE, request, response, baseEntityResource, serviceClass, parentId);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public VirtualList<E> list(@Context UriInfo uriInfo, @QueryParam("query") String queryJSONString) throws Exception {
		Query query = Query.getQuery(queryJSONString);
		return list(uriInfo, query);
	}

	@SuppressWarnings("rawtypes")
	protected VirtualList<E> list(UriInfo uriInfo, Query query) throws Exception {

		if (getParentId() != null && getParentId() == -1) {
			// this is only to clear the ui grid
			return new VirtualList<>(Collections.emptyList());
		} else {
			// recursively: if the parent is defined and not 0, add a filter on the parent
			String parentName = null;
			for (Object resource : uriInfo.getMatchedResources()) {
				if (resource instanceof AbstractBaseEntitiesResource) {
					AbstractBaseEntitiesResource abstractBaseEntitiesResource = (AbstractBaseEntitiesResource) resource;

					if (parentName != null) {
						parentName += "." + abstractBaseEntitiesResource.getParentName();
					} else {
						parentName = abstractBaseEntitiesResource.getParentName();
					}
					Integer parentId = abstractBaseEntitiesResource.getParentId();

					if (parentId != null && parentId != 0) {
						query.addFilter(new Filter(parentName + "Id", parentId));

						// no need to go upper. We already have a more constraining filter than if we
						// go up to the parent
						break;
					}
				}
			}

			// by default, from the UI, we want active only
			if (!query.isActiveOnlySet()) {
				query.setActiveOnly(true);
			}

			// if the request is for an id or a key, do not add any other filter
			Set<String> keyFields = new HashSet<>(Arrays.asList(getService().getKeyFields()));
			keyFields.add("id"); // id is always a key field
			for (String keyField : keyFields) {
				List<Filter> keyFilters = query.getFilters(keyField);

				// if 0 or more than 1, it is not a simple case by id
				if (keyFilters != null && keyFilters.size() == 1) {
					Filter keyFilter = keyFilters.get(0);
					if (keyFilter != null && keyFilter.getValue() != null && keyFilter.getOperator().equals(Operator.eq)) {

						// logger.debug("Searching by key field [" + keyFilter.getField() + "] value [" + keyFilter.getValue() + "]");

						// create a new query with only the key field
						Query keyQuery = new Query();
						keyQuery.setKeySearch(true);
						keyQuery.setActiveOnly(query.activeOnly());
						keyQuery.setSort(query.getSort());
						keyQuery.setHierarchical(query.getHierarchical());

						// add support for multiple keys
						Object[] keys;
						if (keyFilter.getValue() instanceof Array) {
							keys = (Object[]) keyFilter.getValue();
						} else {
							String s = "" + keyFilter.getValue();
							if (s.startsWith("[")) {
								s = s.substring(1, s.length() - 1);
							}
							keys = s.split(",");
						}

						if (keys.length == 1) {
							if (!keyFilter.getField().equals("id") && keyFilter.getOperator().equals(Operator.eq)) {
								keyQuery.addFilter(new Filter(keyFilter.getField(), getService().formatKeyField(keyFilter.getField(), keys[0])));
							} else {
								keyQuery.addFilter(keyFilter);
							}
						} else {
							Filter filter = new Filter(Logic.or);
							for (Object key : keys) {
								if (!keyFilter.getField().equals("id") && keyFilter.getOperator().equals(Operator.eq)) {
									filter.addFilter(new Filter(keyFilter.getField(), getService().formatKeyField(keyFilter.getField(), key)));
								} else {
									filter.addFilter(new Filter(keyFilter.getField(), keyFilter.getOperator(), key));
								}
							}
							keyQuery.addFilter(filter);
						}
						query = keyQuery;
						break;
					}
				}
			}

			// if it is not a search by key
			if (!query.keySearch()) {

				// for each field, verify if the field is a reference
				try {
					getService().verifyKeyFieldReference(query.getFilter());
				} catch (Exception ex) {
					logger.error("verifyKeyFieldReference - Cannot verify field reference [" + getService().getResourceName() + "]: " + query, ex);
				}

				// If the query contains a field in the activeOnly filter, do not use the active only filter
				getService().verifyActiveOnlyFieldInQuery(query);

				// if we need to add the search filter for overall search
				String searchFilterTerm = query.getSearchFilterTerm();
				if (searchFilterTerm != null && searchFilterTerm.length() > 0) {
					logger.debug("Searching overall for [" + searchFilterTerm + "]");

					// if multiple words, each word must be present
					String[] words = searchFilterTerm.trim().split(" ");
					Filter searchOverallFilter;
					if (words.length == 1) {
						searchOverallFilter = getService().getSearchOverallFilter(searchFilterTerm);
					} else {
						searchOverallFilter = new Filter(Logic.and);
						for (String s : words) {
							searchOverallFilter.addFilter(getService().getSearchOverallFilter(s));
						}
					}
					query.addFilter(searchOverallFilter);
				}
			}

			// if there is no page size, set the maximum by default
			// to avoid Excel to download all rows, restrict the limit
			if (query.getPageSize() != null && query.getPageSize() > MAX_LIST_RESULTS) {
				// logger.warn("Got a request for [" + query.getPageSize() + "] " + getTypeOfE().getSimpleName()
				// + " from [" + getUser().getUserName() + "]");
				if (!getService().isUserAdmin()) {
					query.setPageSize(MAX_LIST_RESULTS);
				}
			}
			// System.out.println(query);

			// we cannot have a limit for hierarchical: to built the tree, we need all records
			if (query.hierarchical()) {
				boolean retrieveAll = false;
				if (retrieveAll) {
					query.setPageSize(null);

					// but we must verify the number of records
					long count = getService().count(query);
					if (count > MAX_HIERARCHICAL_RESULTS) {
						throw new ValidationException("tooManyResultsForHierarchical");
					}
				} else {
					query.setPageSize(MAX_HIERARCHICAL_RESULTS);
				}
			}

			long count;
			List<E> data;
			if (query.countOnly()) {
				data = null;
				count = getService().count(query);
			} else {
				data = getService().list(query);
				if (query.getPageSize() != null && !query.hierarchical()) {
					count = getService().count(query);
					if (count == 0) {
						count = data.size();
					}
				} else {
					count = data.size();
				}
			}
			return new VirtualList<>(data, count);
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public E create(E e) throws Exception {
		// verify creation restrictions
		verifyCreationRestrictions();

		// startTransaction
		getService().startTransaction();
		return getService().create(e);
	}

	@GET
	@Path("search")
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResult<E> search() throws Exception {
		String queryJSONString = Util.nullifyIfNeeded(getRequest().getParameter("query"));
		String idString = Util.nullifyIfNeeded(getRequest().getParameter("id"));

		// for multiselect
		boolean retrieveIdOnly = (getRequest().getParameter("retrieveIdOnly") != null ? Boolean.parseBoolean(getRequest().getParameter("retrieveIdOnly")) : true);

		// get the searchText from the request parameter
		String searchText = Util.nullifyIfNeeded(getRequest().getParameter("filter[filters][0][value]"));
		if (searchText == null) {
			searchText = Util.nullifyIfNeeded(getRequest().getParameter("term")); // backward compatibility
			if (searchText == null) {
				searchText = Util.nullifyIfNeeded(getRequest().getParameter("searchText"));
			}
		}

		if (idString != null && idString.equals("-1")) {
			// special case
			return new SearchResult<>("id=-1");
		}

		try {
			Query query;
			if (queryJSONString == null) {
				query = new Query();
			} else {
				// make sure the term does not contains " (double quote)
				if (searchText != null && searchText.indexOf('"') != -1) {
					searchText = searchText.replaceAll("\"", "\\\\\"");
				}

				if (queryJSONString.indexOf("{term}") != -1) { // backward compatibility
					// replace {term} with the searchText
					queryJSONString = queryJSONString.replace("{term}", (searchText != null ? searchText : ""));
				}
				if (queryJSONString.indexOf("{searchText}") != -1) {
					// replace {searchText} with the searchText
					queryJSONString = queryJSONString.replace("{searchText}", (searchText != null ? searchText : ""));
				}
				query = Query.getQuery(queryJSONString);
			}
			// logger.debug("Perform a search term[" + term + "] idString[" + idString + "] Query[" + new Gson().toJson(query) + "]");

			// we need to have distinct result
			Set<E> list = new LinkedHashSet<>();

			if (idString != null) {
				// on initialization, the search is called with the id
				// this is used by Combobox and MultiSelect
				Filter idFilter = new Filter(Logic.or);
				for (String id : idString.split(",")) {
					idFilter.addFilter(new Filter("id", getService().convertId(id)));
				}

				list.addAll(getService().list(idFilter));
			}

			// for combobox, we only need the current ID
			// for multiselect, we would need also to get the 50 first ones otherwise the list will be empty
			if (idString == null || !retrieveIdOnly) {
				// use the standard list (limit to 50)
				query.setSkip(0);
				if (query.getPageSize() == null) {
					query.setPageSize(AbstractBaseEntityService.MAX_SEARCH_RESULTS);
				}

				// by default, from the UI, we want active only
				if (!query.isActiveOnlySet()) {
					query.setActiveOnly(true);
				}

				// logger.debug("Search query: " + new Gson().toJson(query));

				// use the search query
				list.addAll(getService().search(query, searchText));
			}
			return new SearchResult<>((idString != null ? "id=" + idString : searchText), list, query.getPageSize() != null && query.getPageSize().intValue() <= list.size());
		} catch (Exception e) {
			logger.error("Cannot perform a search term[" + (idString != null ? "id=" + idString : searchText) + "]", e);
			throw e;
		}
	}

	@Path("{id}")
	public AbstractBaseEntityResource<E, S, U, I> getBaseEntityResourceById(@PathParam("id") String idString) throws Exception {
		idString = URLDecoder.decode(idString, "UTF-8");
		baseEntityResource.setId(getService().convertId(idString));
		return baseEntityResource;
	}

	@Path("key")
	public AbstractBaseEntityResource<E, S, U, I> getBaseEntityResourceByKey() throws Exception {
		// update a resource by providing a key instead of an ID
		Map<String, String> params = Util.getParameters(getRequest());
		if (params.size() != 1) {
			throw new ValidationException("requestNotValid");
		}
		String key = null;
		String value = null;
		for (Map.Entry<String, String> entry : params.entrySet()) {
			key = entry.getKey();
			value = entry.getValue();
		}
		if (key == null || value == null) {
			throw new ValidationException("requestNotValid");
		}

		try {
			E e = getService().get(new Filter(key, value));
			baseEntityResource.setId(e.getId());
			return baseEntityResource;
		} catch (Exception ex) {
			throw new ValidationException("invalidKey [" + key + "=" + value + "]");
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean isParentUpdateAllowed(String uri, LinkedList<Object> ancestorResources) {
		boolean parentAllowed = false;
		try {
			// ex: /appws/rest/project/verifyActionsRestrictions
			// ex: /appwws/rest/project/35/state/verifyActionsRestrictions
			String[] uriParts = uri.split("/");
			if (uriParts.length > 5 && ancestorResources.size() > 2) {
				// 3 is the start index of the resource
				// this means that we have a parent
				// we need to validate the parent
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < (uriParts.length - 3); i++) {
					sb.append(uriParts[i] + "/");
				}
				sb.append("verifyActionsRestrictions");

				String parentURI = sb.toString();
				String parentId = uriParts[uriParts.length - 3];

				// if parentId is 0, we cannot validate it
				if (!parentId.equals("0")) {

					// get the parent resource
					ancestorResources.poll(); // remove the current resource
					ancestorResources.poll(); // remove the parent id resource
					AbstractBaseEntitiesResource parentResource = (AbstractBaseEntitiesResource) ancestorResources.poll();

					// call the parent to verify if the resource can be modified
					String updateAction = "update";
					Map<String, Boolean> parentActionMap = parentResource.verifyActionsRestrictions(parentURI, ancestorResources, parentId, updateAction);
					parentAllowed = parentActionMap.get(updateAction).booleanValue();
				}
			} else {
				// if there is no parent, it is ok
				parentAllowed = true;
			}
		} catch (Exception ex) {
			parentAllowed = false;
		}
		return parentAllowed;
	}

	/**
	 *
	 * @param uriInfo
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void verifyCreationRestrictions() throws Exception {
		// verification could be done on the parent or/and the current entity

		// id is use for hierarchical grid
		String idString = getRequest().getParameter("id");
		E e = null;
		if (idString != null && idString.equals("-1")) {
			// this is a fake record
			// always false
			throw new ForbiddenException();
		} else if (idString != null && idString.length() > 0) {
			e = baseEntityResource.get(getService().convertId(idString));
		}

		// get the parent from the path
		IEntity parentEntity = null;
		if (getParentId() != null && getService().getParentEntityField() != null) {
			parentEntity = getService().getParentEntityService().get(getParentId());
		}
		getService().verifyCreationRestrictions(e, parentEntity);
	}

	/**
	 * This method is called to verify if the sub resource is allowed to be created based on the status of the master resource
	 *
	 * @param uriInfo
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("verifyCreationRestrictions")
	@Produces(MediaType.APPLICATION_JSON)
	public String verifyCreationRestrictionsGET() throws Exception {
		boolean allowed;
		try {
			verifyCreationRestrictions();
			allowed = true;
		} catch (ForbiddenException e) {
			allowed = false;
		}

		return "{\"allowed\":" + allowed + "}";
	}

	/**
	 * This method is used to verify if the action is allowed by the user on the resource. It does not verify if the user can perform the action (this is done by the role). It validates if the
	 * resource is protected or may not be modified
	 */
	@GET
	@Path("verifyActionsRestrictions")
	@Produces(MediaType.APPLICATION_JSON)
	public String verifyActionsRestrictions(@Context UriInfo uriInfo) throws Exception {
		String idString = getRequest().getParameter("id");
		String actions = getRequest().getParameter("actions");

		Map<String, Boolean> actionMap = verifyActionsRestrictions(getRequest().getRequestURI(), new LinkedList<>(uriInfo.getMatchedResources()), idString, actions);

		// JAXB cannot map Map<String, Boolean>
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (Map.Entry<String, Boolean> entry : actionMap.entrySet()) {
			sb.append("\"" + entry.getKey() + "\": " + entry.getValue() + ",");
		}

		// remove the last comma
		if (sb.length() > 1) {
			sb.deleteCharAt(sb.length() - 1);
		}

		sb.append("}");
		return sb.toString();
	}

	/**
	 * This method will verify the parent (if any) then it will verify the current resource
	 *
	 * @param idString
	 * @param actions
	 * @return
	 * @throws Exception
	 */
	protected Map<String, Boolean> verifyActionsRestrictions(String uri, LinkedList<Object> ancestorResources, String idString, String actions) throws Exception {
		// logger.debug("verifyActionsRestrictions uri[" + uri + "] id[" + idString + "] actions[" + actions + "]");

		// get the entity (if the user cannot see the entity, it will throw an exception)
		E e = null;
		Boolean isParentUpdateAllowed = null;

		if (idString != null && idString.equals("-1")) {
			// this is a fake record
			// always false
			isParentUpdateAllowed = false;
		} else if (idString != null && idString.length() > 0) {
			e = baseEntityResource.get(getService().convertId(idString));
		} else {
			// there is no ID, this mean a new resource
			// we need to validate if we can at least update the parent
			isParentUpdateAllowed = isParentUpdateAllowed(uri, ancestorResources);
		}

		Map<String, Boolean> actionMap = new HashMap<>();
		for (String action : actions.split(",")) {
			boolean allowed;
			if (e == null && isParentUpdateAllowed != null && isParentUpdateAllowed == false) {
				allowed = false;
			} else if (e == null && (action.equals("update") || action.equals("delete") || action.equals("duplicate"))) {
				// to update the resource must already exists
				allowed = false;
			} else {
				try {
					getService().verifyActionRestrictions(action, e);
					allowed = true;
				} catch (ForbiddenException ex) {
					allowed = false;
				} catch (Exception ex) {
					logger.error("Error in verifyActionRestrictions", ex);
					allowed = false;
				}
			}
			actionMap.put(action, allowed);
		}
		return actionMap;
	}

	/**
	 * Get a link (URL) to a resource
	 *
	 * @param formParams
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("link")
	@Produces(MediaType.APPLICATION_JSON)
	public String getLink() throws Exception {
		String idString = getRequest().getParameter("id");
		String link = getService().getLink(baseEntityResource.get(getService().convertId(idString)));
		return "{\"link\":\"" + link + "\"}";
	}

	@GET
	@Path("appClass")
	@Produces(MediaType.APPLICATION_JSON)
	public String getAppClassFields() throws Exception {
		String jsonCompliance = getRequest().getParameter("jsonCompliance");
		return getService().getAppClassFields(jsonCompliance != null ? Boolean.parseBoolean(jsonCompliance) : false);
	}

	@GET
	@Path("resourceManager")
	public void generateResourceManager(@DefaultValue("") @QueryParam(value = "namespace") String namespace) throws Exception {
		getService().generateResourceManager(namespace);
	}

	/**
	 * This method is applied on a collection, but it may return an entity
	 * 
	 * @param formParams
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public E performCollectionAction(MultivaluedMap<String, String> formParams) throws Exception {
		String action = Util.getParameterValue(getRequest(), "action");

		logger.debug("PerformING entity collection action [" + action + "] on [" + this.getClass().getSimpleName() + "]");
		try {
			getService().startTransaction();
			Method method = this.getClass().getMethod(action, MultivaluedMap.class);
			return (E) method.invoke(this, formParams);
		} catch (NoSuchMethodException e) {
			logger.error("Error performing entity collection action [" + action + "] on [" + this.getClass().getSimpleName() + "]: No such method exists." + formParams);
			throw new BaseException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "No method defined for the action [" + action + "]");
		} catch (BaseException e) {
			throw e;
		} catch (Exception e) {
			if (e instanceof InvocationTargetException && e.getCause() != null && e.getCause() instanceof BaseException) {
				throw (BaseException) e.getCause();
			} else {
				throw e;
			}
		}
		// logger.info("PerformED action [" + action + "] on [" + this.getClass().getSimpleName() + "]");
	}

	/**
	 * By default, call the sync method from the getService()
	 *
	 * @param formParams
	 * @throws Exception
	 */
	public void sync(MultivaluedMap<String, String> formParams) throws Exception {
		// only 1 sync at the same time per class
		synchronized (this.getClass()) {
			String section = Util.getParameterValue(getRequest(), "section");
			String useProgressSender = Util.getParameterValue(getRequest(), "useProgressSender");
			if (formParams != null && section == null) {
				section = formParams.getFirst("section");
			}
			if (section != null) {
				// backward compatible when section is on query string
				this.getService().sync(section, new ProgressSender(useProgressSender != null ? getResponse() : null));
			} else {
				this.getService().sync(formParams, new ProgressSender(useProgressSender != null ? getResponse() : null));
			}

			getService().commit(); // we need to commit before the end of the synchronized to let the other session get our modifications
		}
	}

	final protected AbstractBaseEntityResource<E, S, U, I> getBaseEntityResource() {
		return baseEntityResource;
	}

	final protected String getParentName() {
		return getService().getParentEntityField() != null ? getService().getParentEntityField().getName() : null;
	}

	final protected Integer getParentId() {
		return this.parentId;
	}

	public Class<E> getTypeOfE() {
		return typeOfE;
	}

	@Override
	final protected S newService(Class<S> serviceClass) {
		S service = super.newService(serviceClass);
		service.setTypeOfE(typeOfE);
		return service;
	}
}