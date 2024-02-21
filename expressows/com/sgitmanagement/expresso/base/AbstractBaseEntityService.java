package com.sgitmanagement.expresso.base;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Formula;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sgitmanagement.expresso.base.AppClassField.Reference;
import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expresso.dto.Query.Sort;
import com.sgitmanagement.expresso.dto.Query.Sort.Direction;
import com.sgitmanagement.expresso.event.ResourceEventCentral;
import com.sgitmanagement.expresso.exception.AttributeNotFoundException;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expresso.exception.WrongVersionException;
import com.sgitmanagement.expresso.util.DateUtil;
import com.sgitmanagement.expresso.util.DeserializeOnlyStringAdapter;
import com.sgitmanagement.expresso.util.FieldRestrictionUtil;
import com.sgitmanagement.expresso.util.ProgressSender;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expresso.util.ZipUtil;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Subgraph;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/* 
resourceName: activityLogRequestChange
resourcePath: activityLogRequest/0/change
resourceSecurityPath: activityLogRequest/change
 */
abstract public class AbstractBaseEntityService<E extends IEntity<I>, U extends IUser, I> extends AbstractBaseService<U> {
	final public static int MAX_SEARCH_RESULTS = 50;

	// this is only for optimization (do not use distinct is not needed)
	private boolean distinctNeeded = false;

	private boolean refreshAfterMerge = true;

	private boolean logLongRequest = true;

	private boolean updateLastModified = true;

	private Set<String> activeOnlyFields = null;

	private boolean parentEntityLookup = false;
	private Field parentEntityField = null;
	private Field hierarchicalParentEntityField = null;

	// for performance optimization
	private String[] keyFields = null;
	private Boolean parentUpdatable = null;
	private static final Map<Query, Object> creationQueryMap = Collections.synchronizedMap(new HashMap<>());

	private Class<E> typeOfE;

	protected AbstractBaseEntityService() {

	}

	protected AbstractBaseEntityService(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
		this.typeOfE = typeOfE;
	}

	public Class<E> getTypeOfE() {
		return typeOfE;
	}

	final public void setTypeOfE(Class<E> typeOfE) {
		this.typeOfE = typeOfE;
	}

	/**
	 * Utility method to create or update the entity as needed
	 *
	 * @param e
	 * @return
	 * @throws Exception
	 */
	public E merge(E e) throws Exception {
		if (e.getId() == null) {
			return create(e);
		} else {
			return update(e);
		}
	}

	/**
	 *
	 * @param e
	 * @return
	 * @throws Exception
	 */
	public E create(E e) throws Exception {
		if (e == null) {
			throw new Exception("Problem with JAXB: Cannot create a null entity. Please verify your JSON object (maybe the type attribute is missing?)");
		}

		// validate before merge
		validateEntity(e);

		// set the creation date and user if Creatable
		if (Creatable.class.isAssignableFrom(getTypeOfE())) {
			if (((Creatable) e).getCreationDate() == null) {
				((Creatable) e).setCreationDate(new Date());
			}
			if (((Creatable) e).getCreationUserId() == null) {
				((Creatable) e).setCreationUserId(getUser().getId());
			}
		} else {
			// if the entity is Updatable but not Creatable, set the update date and user
			if (Updatable.class.isAssignableFrom(getTypeOfE())) {
				if (((Updatable) e).getLastModifiedDate() == null) {
					((Updatable) e).setLastModifiedDate(new Date());
				}
				if (((Updatable) e).getLastModifiedUserId() == null) {
					((Updatable) e).setLastModifiedUserId(getUser().getId());
				}
			}
		}

		try {
			// logger.debug("Persisting [" + getTypeOfE().getSimpleName() + "]: " + e);
			// MySQL: if many thread tries to persist an object on the same table, all but one thread will persist and the others one will wait
			getEntityManager().persist(e);
			// logger.debug("Persisted [" + getTypeOfE().getSimpleName() + "]: " + e.getId());

			// if there is a keyField and the keyField is null, set it to the ID
			String keyField = getKeyField();
			if (!keyField.equals("id")) {
				Object keyValue = BeanUtils.getProperty(e, keyField);
				if (keyValue == null) {
					setProperty(e, keyField, formatKeyField(keyField, e.getId()));
				}
			}

			if (refreshAfterMerge) {
				flushAndRefresh(e);
			}
			onPostMerge(e);

			// publish event
			ResourceEventCentral.INSTANCE.publishResourceEvent(getResourceName(), ResourceEventCentral.Event.Create, e);

		} catch (PersistenceException ex) {
			// from now, the session is rollback only. Anything done previously will be rollbacked
			// throw ExceptionUtils.getRootCause(ex);
			throw ex;
		}
		return e;
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	final public E getRef(I id) {
		return getEntityManager(false).getReference(getTypeOfE(), id);
	}

	/**
	 * This method cannot be called by an external class because if does not validate the security on the resources
	 *
	 * @param id
	 * @return
	 */
	public E get(I id) {
		if (id != null) {
			return getEntityManager(false).find(getTypeOfE(), id);
		} else {
			return null;
		}
	}

	final public E get(I id, boolean forUpdate) {
		return getEntityManager(false).find(getTypeOfE(), id, LockModeType.PESSIMISTIC_WRITE);
	}

	/**
	 * The subclass must provide an implementation to convert the String id to the actual type
	 *
	 * @param id
	 * @return
	 */
	abstract public I convertId(String id) throws Exception;

	/**
	 * This method is used to validate constraint upon create/update methods
	 *
	 * @param e
	 * @return
	 * @throws Exception
	 */
	public void onPostMerge(E e) throws Exception {
		// by default, do nothing
	}

	/**
	 * Throw ValidationException on error
	 * 
	 * @param e
	 * @throws Exception
	 */
	public void validateEntity(E e) throws Exception {
		// by default, do nothing
	}

	/**
	 * Use when calling updateField from the UI
	 * 
	 * @param e
	 * @param fieldName
	 * @param stringValue
	 */
	public void updateField(E e, String fieldName, String stringValue) throws Exception {
		// because many call to update a field from the same resource may be called,
		// we need to get a lock for the resource
		lock(e);

		Field field = Util.getField(e, fieldName);
		if (field != null) {
			// need to convert the type
			String fieldTypeClassName = field.getType().getName();
			Object value = Util.convertValue(stringValue, fieldTypeClassName);
			field.set(e, value);
		}
		// flushAndRefresh(e);
	}

	/**
	 *
	 * @param v
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public E update(E e) throws Exception {
		if (e == null) {
			throw new Exception("Problem with JAXB: Cannot create a null entity. Please verify your JSON object (maybe the 'type' attribute is missing?)");
		}

		// validate before merge
		validateEntity(e);

		if (getEntityManager().contains(e)) {
			// ok, already in the session
			if (Updatable.class.isAssignableFrom(getTypeOfE())) {
				if (updateLastModified) {
					((Updatable) e).setLastModifiedDate(new Date());
					((Updatable) e).setLastModifiedUserId(getUser().getId());
				}
			}
		} else {
			// get the previous version of the entity
			E p = get(e.getId());

			if (p == null) {
				throw new Exception("Cannot update a resource. Resource " + getTypeOfE().getSimpleName() + "[" + e.getId() + "] does not exist");
			}

			// if the lastModifiedDate is not the same, do not allow the update
			if (Updatable.class.isAssignableFrom(getTypeOfE())) {
				Updatable updatable = (Updatable) p;
				Updatable updatableNew = (Updatable) e;

				// update last modified date
				if (updateLastModified) {
					if (!NotVersionable.class.isAssignableFrom(getTypeOfE())) {
						if (!Util.equals(updatable.getLastModifiedDate(), updatableNew.getLastModifiedDate())) {
							logger.info("WrongVersionException: " + getTypeOfE().getSimpleName() + ":" + e.getId() + ":" + updatable.getLastModifiedDate() + ":" + updatableNew.getLastModifiedDate()
									+ ":" + updatable.getLastModifiedUserId());
							throw new WrongVersionException();
						}
					}

					updatableNew.setLastModifiedDate(new Date());
					updatableNew.setLastModifiedUserId(getUser().getId());
				}
			}

			// for backward compatibility, if the creation is null, put it back to the
			// previous creation date
			if (Creatable.class.isAssignableFrom(getTypeOfE())) {
				if (((Creatable) e).getCreationDate() == null) {
					((Creatable) e).setCreationDate(((Creatable) p).getCreationDate());
				}
				if (((Creatable) e).getCreationUserId() == null) {
					((Creatable) e).setCreationUserId(((Creatable) p).getCreationUserId());
				}
			}

			// copy all properties from e to p
			if (p instanceof HibernateProxy) {
				p = (E) Hibernate.unproxy(p);
			}
			if (e instanceof HibernateProxy) {
				e = (E) Hibernate.unproxy(e);
			}

			if (p == null || e == null) {
				throw new Exception("Cannot update a resource. p: " + p + " e:" + e);
			}

			// set the properties on the previous entity (already in the Hibernate context)
			setProperties(getTypeOfE(), p, e);

			// then we will return the updated previous entity
			e = p;
		}

		if (refreshAfterMerge) {
			flushAndRefresh(e);
		}

		onPostMerge(e);

		// publish event
		ResourceEventCentral.INSTANCE.publishResourceEvent(getResourceName(), ResourceEventCentral.Event.Update, e);

		return e;
	}

	/**
	 *
	 * @param typeOf
	 * @param dest
	 * @param source
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	final private boolean setProperties(Class<?> typeOf, E dest, E source) throws Exception {
		// from the base, set the properties on the entity
		boolean updated = false;

		Map<String, String> restrictedFields = FieldRestrictionUtil.INSTANCE.getFieldRestrictionMap(getResourceName());

		Class<?> c = typeOf;
		do {
			Field[] allFields = c.getDeclaredFields();
			for (Field field : allFields) {
				field.setAccessible(true);
				Object oldValue = field.get(dest);
				Object newValue = field.get(source);

				// logger.debug("Updating [" + field.getName() + "]");

				// if the entity has FieldRestriction, make sure that the user has to role
				if (restrictedFields != null) {
					String restrictedRole = restrictedFields.get(field.getName());
					// logger.debug("RestrictedField[" + field.getName() + "] Role[" + restrictedRole + "]");
					if (restrictedRole != null && !isUserInRole(restrictedRole)) {
						// do not assign them. Keep the current value
						logger.debug("Skipping assigning [" + field.getName() + "] keeping oldValue[" + field.get(dest) + "]");
						continue;
					}
				}

				// if the entity has some UpdateApprobationRequired field, create an UpdateApprobationRequired entry and do not change the value
				if (typeOf.getAnnotation(RequireApproval.class) != null && field.getAnnotation(RequireApproval.class) != null && !Util.equals(newValue, oldValue)) {
					String requireApprovalRole = field.getAnnotation(RequireApproval.class).role();
					if (requireApprovalRole == null || requireApprovalRole.length() == 0) {
						// get it from the resource
						requireApprovalRole = typeOf.getAnnotation(RequireApproval.class).role();
					}
					if (!isUserInRole(requireApprovalRole)) {
						createUpdateApprobationRequired(dest, field, oldValue, newValue);
						continue;
					}
				}

				// if DeserializeOnlyStringAdapter, do not update the field
				if (field.getAnnotation(XmlJavaTypeAdapter.class) != null && field.getAnnotation(XmlJavaTypeAdapter.class).value().equals(DeserializeOnlyStringAdapter.class)) {
					logger.debug("Skipping assigning [" + field.getName() + "] because DeserializeOnlyStringAdapter");
					continue;
				}

				if (oldValue != null && (IEntity.class.isInstance(oldValue) || EntityDerived.class.isInstance(oldValue))) {
					// do not assign them
				} else if (oldValue != null && Set.class.isInstance(oldValue) && field.getAnnotation(CollectionTable.class) != null) {
					// logger.debug("Got a ManyToMany [" + field.getName() + "] for the Entity [" + typeOf.getSimpleName() + "]");

					// this is a collection of ID (Integer).
					Set newIdSet = (Set) newValue;
					Set previousIdSet = (Set) oldValue;

					if (newIdSet == null) {
						newIdSet = new HashSet();
					}

					// add new ids
					for (Object id : newIdSet) {
						if (!previousIdSet.contains(id)) {
							updated = true;
							previousIdSet.add(id);
							// logger.debug("Adding " + id);
						}
					}

					// remove deleted ids
					for (Object id : new ArrayList<>(previousIdSet)) {
						if (!newIdSet.contains(id)) {
							updated = true;
							previousIdSet.remove(id);
							// logger.debug("Removing " + id);
						}
					}

				} else if (oldValue != null && Collection.class.isInstance(oldValue)) {
					// do not assign them (OneToMany)
					if (newValue != null) {
						// ignore
						// logger.debug("Got a Collection [" + field.getName() + "] for the Entity ["
						// + typeOf.getSimpleName() + "]");
					}
				} else {

					if (!Util.equals(newValue, oldValue)) {
						// logger.debug("Setting [" + field.getName() + "]: [" + oldValue + " -> "
						// + newValue +
						// "]");
						updated = true;
						setProperty(dest, field.getName(), newValue);
					}
				}
			}
		} while ((c = c.getSuperclass()) != null);

		return updated;
	}

	/**
	 *
	 * @param object
	 * @param fieldName
	 * @param fieldValue
	 * @throws Exception
	 */
	final private void setProperty(E e, String fieldName, Object fieldValue) throws Exception {
		// set the new value
		Field field = Util.getField(e, fieldName);
		field.set(e, fieldValue);

		// set the new value using the setter
		// BeanUtils.setProperty(e, fieldName, fieldValue);
	}

	/**
	 * 
	 * @param source
	 * @param dest
	 * @param field
	 * @throws Exception
	 */
	protected void createUpdateApprobationRequired(E e, Field field, Object currentValue, Object newValue) throws Exception {
		// to be implemented by the subclass
		throw new NotImplementedException();
	}

	/**
	 * Duplicate a resource
	 *
	 * @param e
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public E duplicate(E e) throws Exception {
		if (getEntityManager().contains(e)) {
			getEntityManager().detach(e);
		}

		e.setId(null);

		if (Creatable.class.isAssignableFrom(getTypeOfE())) {
			((Creatable) e).setCreationDate(null);
			((Creatable) e).setCreationUserId(null);
		}

		if (Updatable.class.isAssignableFrom(getTypeOfE())) {
			((Updatable) e).setLastModifiedDate(null);
			((Updatable) e).setLastModifiedUserId(null);
		}

		if (ExternalEntity.class.isAssignableFrom(getTypeOfE())) {
			((ExternalEntity<I>) e).setExtKey(null);
		}

		return create(e);
	}

	/**
	 * Deactivate a resource
	 *
	 * @param e
	 * @return
	 */
	public E deactivate(E e) throws Exception {
		if (Deactivable.class.isAssignableFrom(getTypeOfE())) {
			if (((Deactivable) e).getDeactivationDate() == null) {
				((Deactivable) e).setDeactivationDate(new Date());
			}
			return update(e);
		}
		return e;
	}

	/**
	 * Use with care: refresh the state of the instance from the database, overwriting changes made to the entity, if any.
	 *
	 * @param e
	 */
	final public E flushAndRefresh(E e) {
		getEntityManager().flush();
		getEntityManager().refresh(e);
		return e;
	}

	/**
	 * Try to lock the entity (wait if needed)
	 *
	 * @param e
	 */
	final public E lock(E e) {
		// logger.debug("Waiting lock [" + e.getClass().getSimpleName() + ":" + e.getId() + "]");

		// do NOT perform this locking refresh directly because it will lock all resources (with MySQL at least)
		// getEntityManager().refresh(e, LockModeType.PESSIMISTIC_WRITE);
		getEntityManager().lock(e, LockModeType.PESSIMISTIC_WRITE);

		// logger.debug("Got lock [" + e.getClass().getSimpleName() + ":" + e.getId() + "]");

		// we must get the latest committed version of the entity (while waiting on the lock, the entity may have been changed by the owner of the lock
		return flushAndRefresh(e);
	}

	/**
	 * Try to lock the entity but do not wait
	 *
	 * @param e
	 */
	final public E lockNoWait(E e) {
		Map<String, Object> properties = new HashMap<>();
		properties.put("jakarta.persistence.lock.timeout", 0);
		getEntityManager().lock(e, LockModeType.PESSIMISTIC_WRITE, properties);

		// we must get the latest committed version of the entity (while waiting on the lock, the entity may have been changed by the owner of the lock
		return flushAndRefresh(e);
	}

	/**
	 * Search the entity using a predefined query (usually called by ComboBox, Multiselect, etc)
	 *
	 * @param searchText
	 * @return
	 */
	public List<E> search(Query query, String searchText) throws Exception {
		if (searchText != null) {
			// if activeOnly and there is a keyField for the entity
			// add a filter to search the keyField only
			// if active only is requested, get the active only filter
			if (query.activeOnly() && getKeyFields().length > 1 /* ignore ID */) {

				// make sure there is a filter and the logic is And
				Filter originalFilter = new Filter(Logic.and);

				// add the original query filter
				originalFilter.addFilter(query.getFilter());

				// add active only flag
				Filter activeOnlyFilter = getActiveOnlyFilter();
				if (activeOnlyFilter != null) {
					originalFilter.addFilter(activeOnlyFilter);
				} else if (Deactivable.class.isAssignableFrom(getTypeOfE())) {
					originalFilter.addFilter(getDeactivableFilter());
				}

				// add the search Filter to the original filter
				originalFilter.addFilter(getSearchFilter(searchText));

				// create a new top filter
				Filter newQueryFilter = new Filter(Logic.or);
				query.setFilter(newQueryFilter);

				// add previous filter
				newQueryFilter.addFilter(originalFilter);

				// now allow all (activeOnly for the original filter)
				query.setActiveOnly(false);

				// add search by keyField
				Filter keyFieldFilter = new Filter(Logic.or);
				newQueryFilter.addFilter(keyFieldFilter);
				for (String keyField : getKeyFields()) {
					if (!keyField.equals("id")) {
						keyFieldFilter.addFilter(new Filter(keyField, formatKeyField(keyField, searchText)));
					}
				}
			} else {
				// only add the search filter
				query.addFilter(getSearchFilter(searchText));
			}
		}
		// logger.debug("Search query: " + query);
		return list(query);
	}

	/**
	 * By default, use the same search as the Combo Box
	 *
	 * @param query
	 * @param searchText
	 * @return
	 * @throws Exception
	 */
	public List<E> searchOverall(Query query, String searchText) throws Exception {
		Filter filter;
		if (searchText == null) {
			filter = new Filter();
		} else {
			// if multiple words, each word must be present
			String[] words = searchText.trim().split(" ");
			if (words.length == 1) {
				filter = getSearchOverallFilter(searchText);
			} else {
				filter = new Filter(Logic.and);
				for (String s : words) {
					filter.addFilter(getSearchOverallFilter(s));
				}
			}
		}
		if (filter != null) {
			query.addFilter(filter);
			return list(query);
		} else {
			logger.error("getSearchFilter method not implemented for the resource [" + getResourceName() + "]");
			return new ArrayList<>();
		}
	}

	public void delete(I id) throws Exception {
		E e = get(id);

		if (e != null) {
			ResourceEventCentral.INSTANCE.publishResourceEvent(getResourceName(), ResourceEventCentral.Event.Delete, e);
			getEntityManager().remove(e);
			getEntityManager().flush();
		}
	}

	final public boolean exists(I id) {
		return (get(id) != null);
	}

	public E getByKeyField(String keyFieldNo) throws Exception {
		String keyField = getKeyField();
		return get(new Filter(keyField, formatKeyField(keyField, keyFieldNo)));
	}

	final public E get(Filter filter) throws Exception {
		return get(new Query(filter));
	}

	public E getLatest(Filter filter, boolean activeOnly) throws Exception {
		String sortFieldName;
		if (Creatable.class.isAssignableFrom(getTypeOfE())) {
			sortFieldName = "creationDate";
		} else {
			sortFieldName = "id";
		}

		Query query = new Query().setActiveOnly(activeOnly);
		query.setPageSize(1);
		query.addSort(new Sort(sortFieldName, Sort.Direction.desc));
		query.addFilter(filter);
		try {
			return get(query);
		} catch (NoResultException ex) {
			return null;
		}
	}

	public E get(Query query) throws Exception {
		List<E> list = list(query);
		if (list.size() == 1) {
			return list.get(0);
		} else if (list.size() == 0) {
			// no result
			throw new NoResultException(getTypeOfE() + " Query: " + query);
		} else {
			// too many results
			throw new NonUniqueResultException(getTypeOfE() + " Query: " + query);
		}
	}

	final public List<E> list() throws Exception {
		return list(new Query());
	}

	final public List<E> list(Filter filter) throws Exception {
		return list(new Query(filter));
	}

	final public List<E> list(boolean activeOnly) throws Exception {
		return list(new Query().setActiveOnly(activeOnly));
	}

	/**
	 * YOU MUST OVERRIDE THE COUNT METHOD IF YOU OVERRIDE THIS LIST METHOD
	 *
	 * @param query
	 * @return
	 * @throws Exception
	 */

	public List<E> list(Query query) throws Exception {
		try {
			query = verifyQuery(query);
			// logger.debug("List query: " + new Gson().toJson(query));

			EntityManager entityManager = getEntityManager(false);

			// use the CriteriaBuilder to create the query
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<E> q = cb.createQuery(getTypeOfE());
			Root<E> root = q.from(getTypeOfE());
			q.select(root);

			if (!query.hasFilters() && !query.hasSort() && query.getPageSize() == null) {
				// use a simple query
				// by default, sort by the id
				q.orderBy(cb.desc(root.get(getKeyField())));
				TypedQuery<E> typedQuery = entityManager.createQuery(q);
				return typedQuery.getResultList();
			} else {
				// create the query using filters, paging and sort

				// this must be called first. It will set distinctNeeded
				Map<String, Join<?, ?>> joinMap = new HashMap<>();
				Predicate predicate = buildPredicates(cb, root, query.getFilter(), joinMap, true);

				// sorts may also trigger a distinct select
				List<Order> orders = new ArrayList<>();
				if (query.getSort() != null && query.getSort().size() > 0 && !query.countOnly()) {
					for (Sort sort : query.getSort()) {
						if (sort.getDir() != null && sort.getDir().equals(Direction.asc)) {
							orders.add(cb.asc(retrieveProperty(root, sort.getField(), joinMap, true)));
						} else {
							orders.add(cb.desc(retrieveProperty(root, sort.getField(), joinMap, true)));
						}
					}
				}

				// always sort by the id at the end to make sure the result set is stable
				for (Sort sort : getUniqueQuerySort()) {
					// make sure it is not already in the sort
					boolean alreadyInSort = false;
					if (query.getSort() != null) {
						for (Sort querySort : query.getSort()) {
							if (querySort.getField().equals(sort.getField())) {
								alreadyInSort = true;
								break;
							}
						}
					}

					if (!alreadyInSort) {
						if (sort.getDir() != null && sort.getDir().equals(Direction.asc)) {
							orders.add(cb.asc(root.get(sort.getField())));
						} else {
							orders.add(cb.desc(root.get(sort.getField())));
						}
					}
				}

				if (distinctNeeded) {
					q.distinct(true);
				}

				if (predicate != null) {
					q.where(predicate);
				}

				// add the sort to the query
				q.orderBy(orders);

				TypedQuery<E> typedQuery = entityManager.createQuery(q);

				if (query.getSkip() != null) {
					typedQuery.setFirstResult(query.getSkip());
				}

				if (query.getPageSize() != null) {
					typedQuery.setMaxResults(query.getPageSize());
				}

				// This is mandatory to eagerly fetch the data using CriteriaBuilder
				// jakarta.persistence.fetchgraph: all relationships are considered to be lazy regardless of annotation, and only the elements of the provided graph are loaded.
				// jakarta.persistence.loadgraph: add lazy entities (eager entities are loaded as defined)
				EntityGraph<E> entityGraph = entityManager.createEntityGraph(getTypeOfE());
				buildEntityGraph(entityGraph, getTypeOfE(), query, null);
				typedQuery.setHint("jakarta.persistence.loadgraph", entityGraph);

				// then issue the SQL query
				Date startDate = new Date();
				List<E> data;
				try {
					data = typedQuery.getResultList();
				} catch (IllegalArgumentException ex) {
					// this could happen when the grid tries to get a next page but that the
					// data has changed since the first load
					// ex: grid tries to display last page: from 150 to 152
					// but there is only 144 records now
					// java.lang.IllegalArgumentException: fromIndex(150) > toIndex(144)
					logger.warn("Problem loading the data: dataset is less than than previously: " + ex + "\n" + new Gson().toJson(query));
					data = new ArrayList<>();
				}

				// if the query required the entity to be created if is does not exist, create it
				if (data.size() == 0 && (query.getCreateIfNotFound() != null && query.getCreateIfNotFound().booleanValue())) {
					E e = createEntityFromUniqueConstraints(query);
					if (e != null) {
						data.add(e);
					}
				}

				// if the query is for an hierarchical list, we need to include all parents and children
				if (query.hierarchical()) {
					int beforeDataSize = data.size();
					int sqlQueryCount = appendHierarchicalEntities(data, query) + 1;
					logger.debug("Number of SQL Queries for hierarchical: " + sqlQueryCount + " Total entities: " + data.size() + " (before: " + beforeDataSize + ")");
				}

				if (data.size() > 5000) {
					logger.warn("Got a request that returns " + data.size() + " resources [" + getTypeOfE().getSimpleName() + "] from[" + getUser().getUserName() + "]. Query: "
							+ new Gson().toJson(query));
				}

				Date endDate = new Date();
				long delay = (endDate.getTime() - startDate.getTime());
				if (delay > 500) {
					// do not print if the request is known to be long
					if (logLongRequest) {
						String delayMessage = "Execution " + getTypeOfE().getSimpleName() + " SQL time: " + delay + " ms (" + data.size() + ") " + getTypeOfE().getSimpleName() + ": "
								+ new Gson().toJson(query);
						if (delay > 5000) {
							logger.warn(delayMessage);
						} else {
							logger.info(delayMessage);
						}
					}
				}

				return data;
			}
		} catch (Exception ex) {
			logger.error("Error executing query [" + getResourceName() + "]: " + ex + " - " + new Gson().toJson(query), ex);
			throw ex;
		}
	}

	/**
	 * 
	 * @param data
	 */
	private int appendHierarchicalEntities(List<E> data, Query query) throws Exception {
		// build a map with the key
		Set<I> ids = new HashSet<>();
		data.forEach(e -> ids.add(e.getId()));

		int sqlQueryCount = 0;

		if (query.appendHierarchicalChildren()) {
			// append children
			sqlQueryCount += appendHierarchicalChildEntities(data, data, ids, query.activeOnly());
		}

		if (query.appendHierarchicalParents()) {
			// append parents
			sqlQueryCount += appendHierarchicalParentEntities(data, data, ids);
		}
		return sqlQueryCount;
	}

	@SuppressWarnings({ "unchecked" })
	protected E getHierarchicalParentEntity(E e) throws Exception {
		Field hierarchicalParentEntityField = getHierarchicalParentEntityField();
		if (hierarchicalParentEntityField != null) {

			// this does not work as it does not use the getter
			// IEntity parentEntityInstance = (IEntity) getParentEntityField().get(e);
			E hierarchicalParentEntity = (E) PropertyUtils.getProperty(e, hierarchicalParentEntityField.getName());

			// if the parent is LAZY, then we get a proxy
			// we need to initialize the proxy
			if (hierarchicalParentEntity != null && hierarchicalParentEntity instanceof HibernateProxy) {
				hierarchicalParentEntity = (E) Hibernate.unproxy(hierarchicalParentEntity);
			}

			return hierarchicalParentEntity;
		} else {
			return null;
		}
	}

	/**
	 * get all the parents in 1 query (for performance reason)
	 * 
	 * @param hierarchicalParentEntities only the list of the entities to get the parent
	 * @param data                       complete list of data
	 * @param ids
	 * @return
	 * @throws Exception
	 */
	protected int appendHierarchicalParentEntities(List<E> hierarchicalParentEntities, List<E> data, Set<I> ids) throws Exception {
		if (ids == null) {
			// build a map with the key
			ids = new HashSet<>();
			for (E e : data) {
				ids.add(e.getId());
			}
		}
		int sqlQueryCount = 0;
		if (!hierarchicalParentEntities.isEmpty()) {
			Field hierarchicalParentEntityField = getHierarchicalParentEntityField();
			if (hierarchicalParentEntityField != null) {
				String hierarchicalParentIdFieldName = hierarchicalParentEntityField.getName() + "Id";
				Set<Integer> hierarchicalParentIds = new HashSet<>();
				for (E e : hierarchicalParentEntities) {
					Integer hierarchicalParentId = (Integer) PropertyUtils.getProperty(e, hierarchicalParentIdFieldName);
					if (hierarchicalParentId != null && !ids.contains(hierarchicalParentId)) {
						hierarchicalParentIds.add(hierarchicalParentId);
					}
				}

				if (!hierarchicalParentIds.isEmpty()) {
					sqlQueryCount++;
					logger.debug("Getting " + hierarchicalParentIds.size() + " new HierarchicalParentEntities");
					List<E> newHierarchicalParentEntities = new ArrayList<>();
					Filter hierarchicalParentIdsFilter = new Filter("id", Operator.inIds, hierarchicalParentIds);
					for (E e : list(new Query().setVerified(true).addFilter(hierarchicalParentIdsFilter))) {
						if (!ids.contains(e.getId())) {
							ids.add(e.getId());
							data.add(e);
							newHierarchicalParentEntities.add(e);
						}
					}

					// now get the upper level
					sqlQueryCount += appendHierarchicalParentEntities(newHierarchicalParentEntities, data, ids);
				}
			}
		}
		return sqlQueryCount;
	}

	/**
	 * 
	 * @param e
	 * @param data
	 * @param ids
	 * @param activeOnly
	 * @throws Exception
	 */
	private int appendHierarchicalChildEntities(List<E> hierarchicalChildEntities, List<E> data, Set<I> ids, boolean activeOnly) throws Exception {
		int sqlQueryCount = 0;
		// if we already reaches the maximum limit, do not include children
		if (data.size() < AbstractBaseEntitiesResource.MAX_HIERARCHICAL_RESULTS) {
			if (!hierarchicalChildEntities.isEmpty()) {
				Set<Integer> hierarchicalChildIds = new HashSet<>();
				for (E e : hierarchicalChildEntities) {
					hierarchicalChildIds.add((Integer) e.getId());
				}
				sqlQueryCount++;
				logger.debug("Getting " + hierarchicalChildIds.size() + " new HierarchicalChildEntities activeOnly: " + activeOnly);
				List<E> newHierarchicalChildEntities = new ArrayList<>();
				Field hierarchicalParentEntityField = getHierarchicalParentEntityField();
				String hierarchicalParentIdFieldName = hierarchicalParentEntityField.getName() + "Id";
				Filter hierarchicalChildIdsFilter = new Filter(hierarchicalParentIdFieldName, Operator.inIds, hierarchicalChildIds);
				for (E e : list(new Query().setActiveOnly(activeOnly).addFilter(hierarchicalChildIdsFilter))) {
					if (!ids.contains(e.getId())) {
						ids.add(e.getId());
						data.add(e);
						newHierarchicalChildEntities.add(e);
					}
				}

				// now get the down level
				sqlQueryCount += appendHierarchicalChildEntities(newHierarchicalChildEntities, data, ids, activeOnly);
			}
		}
		return sqlQueryCount;
	}

	/**
	 * This method will only work if the isolation level is READ_COMMITTED
	 * 
	 * @param filter
	 * @return
	 * @throws Exception
	 */
	private E createEntityFromUniqueConstraints(Query query) throws Exception {
		logger.debug("createEntityFromUniqueConstraints resource [" + getResourceName() + "] query: " + new Gson().toJson(query));

		UniqueFieldConstraints constraintsAnnotation = getTypeOfE().getAnnotation(UniqueFieldConstraints.class);
		if (constraintsAnnotation != null) {
			Object lock;
			synchronized (this.getClass()) {
				if (creationQueryMap.containsKey(query)) {
					lock = creationQueryMap.get(query);
				} else {
					lock = query;
					creationQueryMap.put(query, lock);
				}
			}

			E e = null;
			synchronized (lock) {
				// first try to get it
				try {
					e = get(query.setCreateIfNotFound(false));
					logger.debug("Got it after waiting [" + query + "]");
				} catch (NoResultException ex) {
					// still not found, create it
					e = getTypeOfE().getDeclaredConstructor().newInstance();

					// get the needed values from the filter for each unique constraint
					for (String fieldName : constraintsAnnotation.fieldNames()) {
						Field field = Util.getField(e, fieldName);
						if (field == null) {
							throw new Exception("Cannot create entity. Field missing [" + fieldName + "]");
						}

						if (field != null) {
							Object value = null;
							Filter filter = query.getFilter(fieldName);
							if (filter != null) {
								value = filter.getValue();
							}
							if (value == null) {
								throw new Exception("Cannot create entity. Field value missing [" + fieldName + "]");
							}

							// logger.debug("Setting " + fieldName + " to [" + value + "]");
							field.set(e, Util.convertValue(value, field.getType().getSimpleName()));
						}
					}

					// start transaction (if needed)
					PersistenceManager.getInstance().startTransaction(getEntityManager(true));

					// create and commit for other process to see it
					logger.debug("Creating on demand [" + query + "]");
					e = create(e);

					// clean the map
					creationQueryMap.remove(query);
				}
			}

			// return the new entity
			return e;
		} else {
			return null;
		}
	}

	/**
	 *
	 * @param query
	 * @return
	 * @throws Exception
	 */
	private Query verifyQuery(Query query) throws Exception {
		if (query == null) {
			query = new Query();
		}
		// logger.debug("**** activeOnly" + query.activeOnly() + "
		// isAssignableFrom:"
		// + Deactivable.class.isAssignableFrom(getTypeOfE()) + " getTypeOfE():" +
		// getTypeOfE().getName());

		// if the query is already verified, skip it
		if (!query.verified()) {
			query.setVerified(true);

			if (!query.keySearch()) {

				// if active only is requested, get the active only filter
				if (query.activeOnly()) {
					Filter activeOnlyFilter = getActiveOnlyFilter();
					if (activeOnlyFilter != null) {
						query.addFilter(activeOnlyFilter);
					} else {
						if (Deactivable.class.isAssignableFrom(getTypeOfE())) {
							// and remove deactivated option (deactivationDate)
							Filter filter = getDeactivableFilter();
							query.addFilter(filter);
						}
					}
				}

				// restrict the list to the permitted entities
				Filter restrictionsFilter = getRestrictionsFilter();
				if (restrictionsFilter != null) {
					if (!restrictionsFilter.isSecure()) {
						logger.error("RestrictionsFilter is not secure [" + getResourceName() + "]: " + new Gson().toJson(restrictionsFilter));
						// throw new ValidationException("restrictionsFilterNoSecure");
					}

					query.addFilter(restrictionsFilter);
				}
			} else {
				// restrict the list to the permitted entities
				// Note: do not add restrictions filter if we only need to verify if the key is
				// unique
				if (!query.countOnly()) {
					Filter restrictionsFilter = getRestrictionsFilter();
					if (restrictionsFilter != null) {
						if (!restrictionsFilter.isSecure()) {
							logger.error("RestrictionsFilter is not secure [" + getResourceName() + "]: " + new Gson().toJson(restrictionsFilter));
							// throw new ValidationException("restrictionsFilterNoSecure");
						}

						query.addFilter(restrictionsFilter);
					}
				}
			}

			// let a chance to the subclass to add a default sorting if needed
			if (!query.hasSort()) {
				Query.Sort[] sorts = getDefaultQuerySort();
				if (sorts != null) {
					for (Query.Sort s : sorts) {
						query.addSort(s);
					}
				}
			}

			// In Hibernate 6, we cannot search through entityIds for ManyToMany
			// but we can search for entity.id if the ManyToMany is defined correctly
			fixManyToManyFilters(query.getFilter());
		}

		return query;
	}

	/**
	 * In Hibernate 6, we cannot search through entityIds for ManyToMany but we can search for entities.id if the ManyToMany is defined correctly
	 * 
	 * @param filter
	 */
	private void fixManyToManyFilters(Filter filter) {
		if (filter != null) {
			if (filter.getField() != null && filter.getField().endsWith("Ids")) {
				filter.setField(filter.getField().replace("Ids", "s.id"));
			}
			if (filter.getFilters() != null) {
				for (Filter f : filter.getFilters()) {
					fixManyToManyFilters(f);
				}
			}
		}
	}

	/**
	 * If the query contains a field in the activeOnly filter, do not use the active only filter
	 * 
	 * @param query
	 * @throws Exception
	 */
	void verifyActiveOnlyFieldInQuery(Query query) throws Exception {
		// if active only is requested, get the active only filter
		if (query.activeOnly()) {
			Filter activeOnlyFilter = getActiveOnlyFilter();
			if (activeOnlyFilter != null) {
				// if filter already contains fields for active only,
				// do not include activeOnlyFilter
				boolean includeActiveOnlyFilter = true;
				for (String activeOnlyField : getActiveOnlyFields()) {
					if (query.getFilter(activeOnlyField) != null) {
						includeActiveOnlyFilter = false;
						break;
					}

					// if the UI is based on values (statusId) and the filter is based on the pgmKey (status.pgmKey)
					if (activeOnlyField.endsWith(".pgmKey")) {
						String idField = activeOnlyField.substring(0, activeOnlyField.length() - ".pgmKey".length()) + "Id";
						if (query.getFilter(idField) != null) {
							includeActiveOnlyFilter = false;
							break;
						}
					}

				}

				if (!includeActiveOnlyFilter) {
					query.setActiveOnly(false);
				}
			}
		}
	}

	/**
	 * Verify if the field is a reference
	 * 
	 * @param filter
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	void verifyKeyFieldReference(Filter filter) throws Exception {
		if (filter != null) {
			if (filter.getField() != null) {
				// verify if the field is a reference
				if (filter.getOperator().equals(Filter.Operator.eq) && !Util.isNull("" + filter.getValue())) {
					// get the path to the field
					Field field = Util.getField(getTypeOfE(), filter.getField());
					if (field == null) {
						// this can happen when this is a custom filter
						// logger.warn("verifyKeyFieldReference - Cannot find field [" + filter.getField() + "]");
					} else {

						// at least the name must be ??No
						if (field.getName().length() > 3) {

							@SuppressWarnings("rawtypes")
							AbstractBaseEntityService service = null;
							Field keyField = null;

							String keyFieldName = field.getName();
							String resourceName = field.getName().substring(0, field.getName().length() - "No".length());

							if (field.isAnnotationPresent(KeyField.class)) {
								// 1) Field: equipmentNo
								// 2) Sub: equipment.equipmentNo
								keyField = field;
								if (filter.getField().indexOf('.') == -1) {
									// keyfield in this class
									service = this;
								}
							} else if (field.isAnnotationPresent(KeyFieldReference.class) || filter.getField().endsWith("No")) {
								// 3) Formula: equipmentNo
								KeyFieldReference keyFieldReference = field.getAnnotation(KeyFieldReference.class);
								if (keyFieldReference != null) {
									if (!keyFieldReference.resourceName().equals("")) {
										resourceName = keyFieldReference.resourceName();
									}
									if (!keyFieldReference.keyFieldName().equals("")) {
										keyFieldName = keyFieldReference.keyFieldName();
									}
								}

								// get the keyField from the other resource class
								Class<?> entityClass = Util.findEntityClassByName(StringUtils.capitalize(resourceName));
								if (entityClass != null) {
									keyField = Util.getField(entityClass, keyFieldName);
								}
							}

							if (keyField != null) {
								if (service == null) {
									service = newService(resourceName);
								}
								if (service != null) {
									// Get the service for the field
									String previousKey = "" + filter.getValue();
									service.formatKeyField(filter, keyField);
									logger.debug("Field [" + filter.getField() + "] KeyField[" + resourceName + ":" + keyFieldName + "]: Value[" + previousKey + "] -> [" + filter.getValue() + "]"
											+ (filter.getOperator().equals(Operator.eq) ? "" : " (Using " + filter.getOperator().toString() + ")"));
								}
							}
						}
					}
				}
			} else {
				if (filter.getFilters() != null) {
					for (Filter f : filter.getFilters()) {
						verifyKeyFieldReference(f);
					}
				}
			}
		}
	}

	final public long count() throws Exception {
		return count(new Query());
	}

	final public long count(Filter filter) throws Exception {
		return count(new Query(filter));
	}

	/**
	 * YOU MUST OVERRIDE THIS METHOD IF YOU OVERRIDE THE LIST METHOD
	 *
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public long count(Query query) throws Exception {
		try {
			query = verifyQuery(query);

			// use the CriteriaBuilder to create the query
			CriteriaBuilder cb = getEntityManager(false).getCriteriaBuilder();
			CriteriaQuery<Long> q = cb.createQuery(Long.class);
			Root<E> root = q.from(getTypeOfE());

			if (!query.hasFilters() && !query.hasSort() && query.getPageSize() == null) {
				// use a simple query
				q.select(cb.count(root));
			} else {
				// create the query using filters

				// this must be called first. It will set distinctNeeded
				Predicate predicate = buildPredicates(cb, root, query.getFilter(), new HashMap<String, Join<?, ?>>(), false);
				if (predicate != null) {
					q.where(predicate);
				}

				if (distinctNeeded) {
					q.select(cb.countDistinct(root));
				} else {
					q.select(cb.count(root));
				}
			}

			// count the total number of records
			return getEntityManager(false).createQuery(q).getSingleResult();
		} catch (Exception ex) {
			logger.error("Error executing count query[" + getResourceName() + "]: " + ex + " - " + new Gson().toJson(query));
			throw ex;
		}

	}

	/**
	 *
	 * @param filter
	 * @param parentEntityName
	 */
	private void addParentEntityName(Filter filter, String parentEntityName, List<Filter> processedFilters) {
		if (parentEntityName == null) {
			logger.warn("Probably @ParentEntity annotation missing on the parent object in class [" + getTypeOfE().getSimpleName() + "]");
		}
		if (filter != null) {
			// do not process the same filter twice
			if (processedFilters == null) {
				// do not use HashSet because the hashing is done at insertion
				// and we change the field later
				processedFilters = new ArrayList<>();
			}
			if (!processedFilters.contains(filter)) {
				processedFilters.add(filter);
				if (filter.getField() != null) {
					filter.setField(parentEntityName + "." + filter.getField());
				}
				if (filter.getFilters() != null) {
					for (Filter f : filter.getFilters()) {
						addParentEntityName(f, parentEntityName, processedFilters);
					}
				}
			}
		}
	}

	/**
	 * Give a chance to service to restrict the list
	 *
	 * @return
	 * @throws Exception
	 */
	protected Filter getRestrictionsFilter() throws Exception {
		// by default, there is no restrictions on the resource
		// if there is a ParentEntity, use the restrictions of the parent
		if (getParentEntityField() != null) {
			Filter filter = getParentEntityService().getRestrictionsFilter();

			// for each filter, add the parent entity name
			addParentEntityName(filter, getParentEntityName(), null);

			return filter;
		} else {
			return null;
		}
	}

	/**
	 * If the entity is restricted, verify if the user is restricted. If the user is restricted, we will remove all restricted fields in the entity
	 *
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	protected boolean isUserRestricted(U user) throws Exception {
		// by default, there is no restrictions on the resource
		// if there is a ParentEntity, use the restrictions of the parent
		if (getParentEntityField() != null) {
			return getParentEntityService().isUserRestricted(user);
		} else {
			return false;
		}
	}

	/**
	 * Verify if the user can perform the action on the resource
	 *
	 * @param action
	 * @param e
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void verifyActionRestrictions(String action, E e) throws Exception {
		// logger.debug("verifyActionRestrictions: " + action + ":" +
		// getTypeOfE().getSimpleName() + ":"
		// + (e != null ? e.getId() : null));

		// if a owner is defined, must sure nobody else can modify the entity
		if (e != null && e instanceof Creatable && getTypeOfE().isAnnotationPresent(ProtectedByCreator.class)) {
			Integer creationUserId = ((Creatable) e).getCreationUserId();
			if (creationUserId != null && !creationUserId.equals(getUser().getId()) && !this.isUserAdmin()) {
				throw new ForbiddenException();
			}
		}

		// otherwise, by default, there is no restrictions on the actions on the
		// resource
		// if there is a ParentEntity, use the restrictions of the parent
		if (e != null && getParentEntityField() != null) {
			if (action.equals("duplicate")) {
				// by default, if the user can read the entity, it can duplicate it
			} else {
				IEntity parentEntity = getParentEntityInstance(e);
				if (parentEntity != null && parentEntity instanceof BasicEntity) {
					// this happen using the Basic and Extended pattern
					parentEntity = (IEntity) ((BasicEntity) parentEntity).getExtended();
				}
				getParentEntityService().verifyActionRestrictions("update", parentEntity);
			}
		}
	}

	/**
	 * Verify if the user is allowed to create an entity.
	 *
	 * @param e            the current entity selected in the grid (only useful for hierarchy structure) (could be null)
	 * @param parentEntity
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked" })
	public void verifyCreationRestrictions(E e, IEntity<?> parentEntity) throws Exception {
		// logger.debug(
		// "verifyCreationRestrictions: " + getTypeOfE().getSimpleName() + ":" + (e !=
		// null ? e.getId() : null)
		// + " parentId: " + (parentEntity != null ? parentEntity.getId() : "null"));

		// by default, master resource are always allowed to be created
		// sub resource are allowed based on the state of their master resource

		// if there is a ParentEntity, use the restrictions of the parent
		if (parentEntity != null && getParentEntityField() != null) {
			if (parentUpdatable == null) {
				try {
					getParentEntityService().verifyActionRestrictions("update", parentEntity);
					parentUpdatable = true;
				} catch (ForbiddenException ex) {
					parentUpdatable = false;
					throw ex;
				}
			} else {
				if (!parentUpdatable) {
					throw new ForbiddenException();
				}
			}
		}
	}

	/**
	 * Get the field annotated with the @HierarchicalParentEntity annotation
	 *
	 * @return
	 */
	private Field getHierarchicalParentEntityField() {
		if (this.hierarchicalParentEntityField == null) {
			for (Field field : getTypeOfE().getDeclaredFields()) {
				if (field.isAnnotationPresent(HierarchicalParentEntity.class)) {
					field.setAccessible(true);
					this.hierarchicalParentEntityField = field;
					break;
				}
			}
		}
		return this.hierarchicalParentEntityField;
	}

	/**
	 * Get the field annotated with the @ParentEntity annotation
	 *
	 * @return
	 */
	public Field getParentEntityField() {
		if (!this.parentEntityLookup) {
			this.parentEntityLookup = true;
			Class<?> clazz = getTypeOfE();
			while (clazz != null) {
				for (Field field : clazz.getDeclaredFields()) {
					if (field.isAnnotationPresent(ParentEntity.class)) {
						field.setAccessible(true);
						this.parentEntityField = field;
						return this.parentEntityField;
					}
				}
				clazz = clazz.getSuperclass();
			}

			// logger.debug("parentEntityField: " + parentEntityField);
		}
		return this.parentEntityField;
	}

	/**
	 * Get the parent entity name
	 *
	 * @return
	 */
	public String getParentEntityName() {
		return getParentEntityField() != null ? getParentEntityField().getName() : null;
	}

	@SuppressWarnings("rawtypes")
	public IEntity getParentEntityInstance(E e) throws Exception {
		if (getParentEntityField() != null) {

			// this does not work as it does not use the getter
			// IEntity parentEntityInstance = (IEntity) getParentEntityField().get(e);
			IEntity parentEntityInstance = (IEntity) PropertyUtils.getProperty(e, getParentEntityField().getName());

			// if the parent is LAZY, then we get a proxy
			// we need to initialize the proxy
			if (parentEntityInstance != null && parentEntityInstance instanceof HibernateProxy) {
				parentEntityInstance = (IEntity) Hibernate.unproxy(parentEntityInstance);
			}

			return parentEntityInstance;
		} else {
			return null;
		}
	}

	/**
	 * Get the service for the parent entity
	 *
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public AbstractBaseEntityService getParentEntityService() throws Exception {
		Field parentEntityField = getParentEntityField();
		if (parentEntityField != null) {
			Class parentEntityClass = parentEntityField.getType();

			if (parentEntityClass != null && BasicEntity.class.isAssignableFrom(parentEntityClass)) {
				// this happen using the Basic and Extended pattern
				parentEntityClass = getEntityFromBasicEntity(parentEntityClass);
			}

			String parentEntityServiceClassName = parentEntityClass.getCanonicalName() + "Service";
			Class parentEntityServiceClass = Class.forName(parentEntityServiceClassName);
			return newService(parentEntityServiceClass, parentEntityClass);
		} else {
			return null;
		}
	}

	/**
	 * Build the graph to allow Hibernate to load all required table in one call only
	 *
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private Object buildEntityGraph(Object entityGraph, Class<?> clazz, Query query, String filterPath) {
		do {
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				String filterName = (filterPath != null ? filterPath + "." : "") + field.getName();

				boolean eagerRelation = false;
				if (field.isAnnotationPresent(ManyToOne.class)) {
					ManyToOne rel = field.getAnnotation(ManyToOne.class);
					eagerRelation = rel.fetch() != null && rel.fetch() == FetchType.EAGER;
				} else if (field.isAnnotationPresent(OneToOne.class)) {
					OneToOne rel = field.getAnnotation(OneToOne.class);
					eagerRelation = rel.fetch() != null && rel.fetch() == FetchType.EAGER;
				}

				if (!eagerRelation) {
					// if the relations is lazy but there is a filter on a descendant of the field,
					// we still need to include it in the Entity Graph
					if (field.isAnnotationPresent(ManyToOne.class)) {
						List<Filter> filters = query.getFilters(filterName + ".", true);
						if (filters != null && !filters.isEmpty()) {
							eagerRelation = true;
						}
					}
				}

				if (eagerRelation) {
					Subgraph subGraph = null;
					if (entityGraph instanceof EntityGraph) {
						((EntityGraph) entityGraph).addAttributeNodes(field.getName());
						subGraph = ((EntityGraph) entityGraph).addSubgraph(field.getName());
					} else {
						((Subgraph) entityGraph).addAttributeNodes(field.getName());
						subGraph = ((Subgraph) entityGraph).addSubgraph(field.getName());
					}

					// now do the same for the Object (cascading)
					// we cannot go to the same class (circular reference)
					if (subGraph != null && !field.getType().equals(clazz)) {
						buildEntityGraph(subGraph, field.getType(), query, filterName);
					}
				}
			}
		} while ((clazz = clazz.getSuperclass()) != null);
		return entityGraph;
	}

	/**
	 * Give a change to subclass to define default sorts.
	 */
	protected Query.Sort[] getDefaultQuerySort() {
		return getUniqueQuerySort();
	}

	/**
	 * Give a change to subclass to define the unique sorts. This is mandatory because when sorting by a non unique value, the result set is not stable.<br>
	 * Expresso will always add this unique sort at the end of the order by clause
	 */
	protected Query.Sort[] getUniqueQuerySort() {
		return new Query.Sort[] { new Query.Sort("id", Query.Sort.Direction.desc) };
	}

	/**
	 * Give a change to subclass to define the "key" fields
	 */
	protected String[] getKeyFields() {
		if (this.keyFields == null) {
			List<String> keyFields = new ArrayList<>();
			Class<?> c = getTypeOfE();
			do {
				for (Field field : c.getDeclaredFields()) {
					if (field.isAnnotationPresent(KeyField.class)) {
						keyFields.add(field.getName());
					}
				}
			} while ((c = c.getSuperclass()) != null);
			// always add the id at the end
			keyFields.add("id");
			this.keyFields = keyFields.toArray(new String[0]);
		}

		return this.keyFields;
	}

	/**
	 * Return the first keyField
	 * 
	 * @return
	 */
	protected String getKeyField() {
		return getKeyFields()[0];
	}

	/**
	 * Replace the value in the filter with the formatted key value
	 * 
	 * @param filter
	 * @param keyField
	 */
	public void formatKeyField(Filter filter, Field keyField) {
		String formattedKey = formatKeyField(keyField, "" + filter.getValue());
		filter.setValue(formattedKey);
	}

	final public String formatKeyField(Object keyValue) {
		return formatKeyField(getKeyField(), keyValue);
	}

	final public String formatKeyField(String keyFieldName, Object keyValue) {
		if (keyValue == null) {
			return null;
		} else {
			try {
				return formatKeyField(Util.getField(getTypeOfE(), keyFieldName), "" + keyValue);
			} catch (Exception e) {
				// ignore
				return (String) keyValue;
			}
		}
	}

	/**
	 * Format the key value based on the pattern on the key field
	 *
	 * @param keyField
	 * @param keyValue
	 * @return formattedKey
	 */
	public String formatKeyField(Field keyField, String keyValue) {
		try {
			return Util.formatKeyField(keyField, keyValue);
		} catch (Exception e) {
			logger.warn("Cannot format keyField[" + keyField.getName() + "] with value[" + keyValue + "]");
			return keyValue;
		}
	}

	/**
	 * Give a change to subclass to define the application. By default, return the getResourceManager().
	 */
	protected String getApplicationName() throws Exception {
		return getResourceManager();
	}

	/**
	 * Nomenclature <br>
	 * - resourceName: activityLogRequestChange <br>
	 * - resourcePath: activityLogRequest/0/change <br>
	 * - resourceSecurityPath: activityLogRequest/change <br>
	 * 
	 * @return
	 */
	public String getResourceName() {
		return StringUtils.uncapitalize(getTypeOfE().getSimpleName());
	}

	/**
	 * This method will work only if the resource has a @KeyField and the type is string
	 * 
	 * @param e
	 * @return
	 * @throws Exception
	 */
	public String getResourceNo(E e) throws Exception {
		return BeanUtils.getProperty(e, getKeyField());
	}

	/**
	 * Get the resource paths for this entity.<br>
	 * IMPORTANT: The default implementation works well for the master resource only. For sub resources, you MAY need to override this
	 *
	 * Nomenclature <br>
	 * - resourceName: activityLogRequestChange <br>
	 * - resourcePath: activityLogRequest/0/change <br>
	 * - resourceSecurityPath: activityLogRequest/change <br>
	 * 
	 * @return
	 */
	public String getResourceSecurityPath() throws Exception {
		String resourcePath = getResourceName();
		if (getParentEntityField() != null) {
			// usually, sub resource starts with the name of the parent resource
			// Ex: project, projectLot and projectLotItem
			// the resource security path is: project/lot/item
			String parentResourceName = StringUtils.uncapitalize(getParentEntityService().getTypeOfE().getSimpleName());

			if (resourcePath.startsWith(parentResourceName)) {
				String parentResourcePath = getParentEntityService().getResourceSecurityPath();
				resourcePath = parentResourcePath + "/" + StringUtils.uncapitalize(resourcePath.substring(parentResourceName.length()));
			}
		}
		return resourcePath;
	}

	/**
	 * Get the resource paths for this entity.<br>
	 * IMPORTANT: The default implementation works well for the master resource only. For sub resources, you MAY need to override this
	 *
	 * Nomenclature <br>
	 * - resourceName: activityLogRequestChange <br>
	 * - resourcePath: activityLogRequest/0/change <br>
	 * - resourceSecurityPath: activityLogRequest/change <br>
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String getResourcePath(E e) throws Exception {
		String resourcePath = getResourceName();

		if (getParentEntityField() != null) {
			String parentResourceName = StringUtils.uncapitalize(getParentEntityService().getTypeOfE().getSimpleName());
			if (resourcePath.startsWith(parentResourceName)) {
				String parentResourcePath = getParentEntityService().getResourcePath((E) null);
				resourcePath = parentResourcePath + "/0/" + StringUtils.uncapitalize(resourcePath.substring(parentResourceName.length()));
			}
		}
		return resourcePath + (e != null ? "/" + e.getId() : "");
	}

	/**
	 * Exemples:<br>
	 * ActivityLogRequestManager<br>
	 * ActivityLogRequestManager.ChangeManager<br>
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getResourceManager() throws Exception {
		String resourceManager = getTypeOfE().getSimpleName() + "Manager";
		if (getParentEntityField() != null) {
			String parentResourceName = getParentEntityService().getTypeOfE().getSimpleName();

			if (resourceManager.startsWith(parentResourceName)) {
				resourceManager = resourceManager.substring(parentResourceName.length());
			}
			resourceManager = getParentEntityService().getResourceManager() + "." + resourceManager;
		}
		return resourceManager;
	}

	/**
	 * By default there is no active only filter.
	 *
	 * @return
	 * @throws Exception
	 */
	protected Filter getActiveOnlyFilter() throws Exception {
		// do not filter on parent active only status by default because in most cases,
		// it does not make sense
		// A sub resource could have 3 use cases that we need to address:
		// 1) All subs
		// 2) All active subs (regardless of the status of the parent)
		// 3) All active subs for active parent
		// If we use by default the parent status, we cannot address case 2 anymore
		// return getParentActiveOnlyFilter();
		return null;
	}

	/**
	 * A child is active only if its parent is active
	 *
	 * @return
	 * @throws Exception
	 */
	final protected Filter getParentActiveOnlyFilter() throws Exception {
		// if there is a ParentEntity, use the active only filter of the parent
		if (getParentEntityField() != null) {
			Filter filter = getParentEntityService().getActiveOnlyFilter();

			// for each filter, add the parent entity name
			addParentEntityName(filter, getParentEntityName(), null);

			return filter;
		} else {
			// by default, there is not active only filter
			return null;
		}
	}

	/**
	 * Return an array of fields that are used in the getActiveOnlyFilter method. If a filter is already defined on those fields, the getActiveOnlyFilter will not be used
	 *
	 * @return
	 * @throws Exception
	 */
	private Set<String> getActiveOnlyFields() throws Exception {
		if (this.activeOnlyFields == null) {
			this.activeOnlyFields = new HashSet<>();

			// for each filter in the getActiveOnlyFilter, add the field in the list
			Filter filter = getActiveOnlyFilter();
			extractFieldsFromFilter(filter, this.activeOnlyFields);
		}
		return this.activeOnlyFields;
	}

	/**
	 * Recursively extract the field from the filter and put them in the fields set
	 *
	 * @param filter
	 * @param fields
	 */
	private void extractFieldsFromFilter(Filter filter, Set<String> fields) {
		if (filter != null) {
			if (filter.getField() != null) {
				String fieldName = filter.getField();
				fields.add(fieldName);

				// if field end with "Id", add ".pgmKey" (and vice-versa)
				String idToken = "Id";
				String pgmKeyToken = ".pgmKey";
				if (fieldName.endsWith(idToken)) {
					fields.add(fieldName.substring(0, fieldName.length() - idToken.length()) + pgmKeyToken);
				}
				if (fieldName.endsWith(pgmKeyToken)) {
					fields.add(fieldName.substring(0, fieldName.length() - pgmKeyToken.length()) + idToken);
				}

			}
			if (filter.getFilters() != null) {
				for (Filter f : filter.getFilters()) {
					extractFieldsFromFilter(f, fields);
				}
			}
		}
	}

	/**
	 *
	 * @return
	 */
	protected Filter getDeactivableFilter() {
		// and remove deactivated option (deactivationDate)
		Filter filter = new Filter(Logic.or);
		filter.addFilter(new Filter("deactivationDate", Operator.isNull));
		filter.addFilter(new Filter("deactivationDate", Operator.gt, new Date()));
		return filter;
	}

	/**
	 * Get the search filter. By default, it does nothing. The subclass has to implement this method
	 *
	 * @param term
	 * @return
	 */
	protected Filter getSearchFilter(String term) {
		// if there is a keyField, define the default filter
		String keyFieldName = null;
		for (String k : getKeyFields()) {
			if (!k.equals("id")) {
				keyFieldName = k;
				break;
			}
		}
		if (keyFieldName != null) {
			Filter filter = new Filter(Logic.or);
			filter.addFilter(new Filter(keyFieldName, formatKeyField(keyFieldName, term)));
			return filter;
		} else {
			return null;
		}
	}

	/**
	 * Get the search overall filter. By default, called the getSearchFilter
	 *
	 * @param term
	 * @return
	 */
	protected Filter getSearchOverallFilter(String term) {
		return getSearchFilter(term);
	}

	/**
	 *
	 * @param join
	 * @param field
	 * @return
	 */
	final private <T> Path<T> retrieveProperty(From<?, ?> join, String field, Map<String, Join<?, ?>> joinMap, boolean fetch) {
		Path<T> p = null;
		if (field != null) {
			try {
				if (field.indexOf('.') != -1) {

					String[] paths = field.split("\\.");
					String joinString = "";
					for (int i = 0; i < (paths.length - 1); i++) {
						// s is a relation, not an attribute
						String s = paths[i];

						// verify if it is a collection
						p = join.get(s);

						// in Hibernate5: p.getJavaType() is Set for a OneToMany
						// in Hibernate6: p.getJavaType() is the generic type in the Set for a OneToMany
						if (Collection.class.isAssignableFrom(p.getJavaType()) || p instanceof SqmPluralValuedSimplePath /* || s.endsWith("s") */) {
							distinctNeeded = true;
						}

						// verify if the join already exists
						// reuse it if it exists
						joinString += (joinString.length() == 0 ? s : "." + s);
						// logger.debug("s [" + s + "] joinString [" + joinString + "]: " + joinMap.containsKey(joinString));
						if (joinMap.containsKey(joinString)) {
							join = joinMap.get(joinString);
						} else {
							// build the left join
							// fetch instead of join
							// if we use .join(), it will create a new join instead of using the same
							if (fetch) {
								join = (Join<?, ?>) join.fetch(s, JoinType.LEFT);
							} else {
								// for count query, we use join
								join = join.join(s, JoinType.LEFT);
							}
							joinMap.put(joinString, (Join<?, ?>) join);
						}
					}
					field = paths[paths.length - 1];
				}

				// get the path of the class attribute
				p = join.get(field);
			} catch (IllegalArgumentException ex) {
				// attribute not found
				// ignore. it means that the filter contains other filter than the one on the
				// entity
				throw new AttributeNotFoundException("Attribute [" + field + "] not found [" + join.getJavaType().getSimpleName() + "]");
			}
		}
		return p;
	}

	@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
	final private Predicate getPredicate(CriteriaBuilder cb, Path path, Filter filter) throws Exception {
		Predicate predicate;
		Operator op = filter.getOperator();
		String opName = filter.getOperator().name().toLowerCase();

		EntityManager entityManager = getEntityManager(false);
		boolean caseSensitive = (boolean) entityManager.getProperties().get("expresso.case_sensitive");
		String truncFunction = (String) entityManager.getProperties().get("expresso.trunc_date_function");
		boolean emptyStringIsNull = (boolean) entityManager.getProperties().get("expresso.empty_string_is_null");
		Integer inMaxValues = (Integer) entityManager.getProperties().get("expresso.in_max_values");

		// handle all is [not] null cases here
		if (filter.getValue() == null ||
		// special case for Javascript/HTML option null value
				filter.getValue().equals("null") || filter.getValue().equals("undefined") ||
				// special case for filtering in the Grid on no value
				(("" + filter.getValue()).equals("-2.0") && filter.getField().endsWith("Id"))
				// null or empty operators
				|| (("" + filter.getValue()).length() == 0 || opName.indexOf("null") != -1 || opName.indexOf("empty") != -1) && !(filter.getValue().equals("") && opName.indexOf("contains") != -1)) {
			switch (op) {

			case isNotEmpty:
			case isnotempty:
				if (emptyStringIsNull) {
					predicate = cb.isNotNull(path);
				} else {
					predicate = cb.notEqual(path, "");
				}
				break;

			case isEmpty:
			case isempty:
				if (emptyStringIsNull) {
					predicate = cb.isNull(path);
				} else {
					predicate = cb.equal(path, "");
				}
				break;

			case isnotnullorempty:
			case isNotNullOrEmpty:
				if (emptyStringIsNull) {
					predicate = cb.isNotNull(path);
				} else {
					predicate = cb.and(cb.isNotNull(path), cb.notEqual(path, ""));
				}
				break;

			case isnullorempty:
			case isNullOrEmpty:
				if (emptyStringIsNull) {
					predicate = cb.isNull(path);
				} else {
					predicate = cb.or(cb.isNull(path), cb.equal(path, ""));
				}
				break;

			case neq:
			case isnotnull:
			case isNotNull:
				predicate = cb.isNotNull(path);
				break;

			case eq:
			case isNull:
			case isnull:
			case equalsNoKey:
				predicate = cb.isNull(path);
				break;

			default:
				// throw new Exception("Operator [" + filter.getOperator() + "] not supported
				// for null value");
				predicate = null;
				break;
			}
		} else {

			// need to get the type of the field
			String type = path.getJavaType().getSimpleName();
			String valueType = filter.getValue() != null ? filter.getValue().getClass().getSimpleName() : null;

			// logger.debug("Field: " + path.getAlias() + " Type: " + type + " ValueType: "
			// + valueType);

			// convert type to Class
			switch (type) {
			case "int":
			case "Integer":
			case "long":
			case "Long":
			case "short":
			case "Short":
				type = "Integer";
				break;

			case "float":
			case "Float":
			case "double":
			case "Double":
				type = "Double";
				break;

			case "Date":
			case "Timestamp":
				type = "Date";
				break;

			}

			switch (type) {
			case "Integer":
				Integer integerValue = null;
				List<Integer> integerValues = null;
				if (valueType.equals("Integer")) {
					integerValue = (Integer) filter.getValue();
				} else if (valueType.equals("Double")) {
					integerValue = ((Double) filter.getValue()).intValue();
				} else if (Collection.class.isAssignableFrom(filter.getValue().getClass())) {
					integerValues = new ArrayList<>((Collection<Integer>) filter.getValue());
				} else if (valueType.equals("Integer[]")) {
					integerValues = Arrays.asList((Integer[]) filter.getValue());
				} else {
					String v = (String) filter.getValue();
					if (v.indexOf(',') != -1) {
						// list of integer. Usually for Operator.in
						integerValues = new ArrayList<>(Util.stringIdsToIntegers(v));
					} else {
						try {
							if (v.indexOf('.') == -1) {
								integerValue = Integer.parseInt(v);
							} else {
								integerValue = (int) Float.parseFloat(v);
							}
						} catch (NumberFormatException ex) {
							// invalid number, use null (predicate will always be false)
						}
					}
				}
				switch (op) {
				case gt:
					predicate = cb.gt(path, integerValue);
					break;

				case gte:
					predicate = cb.greaterThanOrEqualTo(path, integerValue);
					break;

				case lt:
					predicate = cb.lt(path, integerValue);
					break;

				case lte:
					predicate = cb.lessThanOrEqualTo(path, integerValue);
					break;

				case neq:
					predicate = cb.notEqual(path, integerValue);
					break;

				// contains should not be necessary, but sometime in the UI if we use composed
				// name KendoUI will assume a string instead of int
				case contains:
				case eq:
					predicate = cb.equal(path, integerValue);
					break;

				case inIds:
					// do not use distinct because if there is a clob, Oracle will throw an exception
					// use this operator only for Ids
					if (integerValues == null) {
						integerValues = new ArrayList<Integer>();
						if (integerValue != null) {
							integerValues.add(integerValue);
						}
					}

					// if integerValues is empty -> no result
					predicate = cb.or(getInPredicate(inMaxValues, integerValues, path, cb, false).toArray(new Predicate[0]));
					break;

				case in:
					distinctNeeded = true;
					if (integerValues == null) {
						integerValues = new ArrayList<Integer>();
						if (integerValue != null) {
							integerValues.add(integerValue);
						}
					}

					// if integerValues is empty -> no result
					predicate = cb.or(getInPredicate(inMaxValues, integerValues, path, cb, false).toArray(new Predicate[0]));
					break;

				case notIn:
					distinctNeeded = true;
					if (integerValues == null) {
						integerValues = new ArrayList<Integer>();
						if (integerValue != null) {
							integerValues.add(integerValue);
						}
					}

					predicate = cb.not(cb.or(getInPredicate(inMaxValues, integerValues, path, cb, false).toArray(new Predicate[0])));
					break;

				default:
					throw new Exception("Operator [" + filter.getOperator() + "] not supported for type [" + type + "]");
				}
				break;

			case "Double":
				Double doubleValue;
				if (valueType.equals("Double")) {
					doubleValue = (Double) filter.getValue();
				} else if (valueType.equals("Integer")) {
					doubleValue = ((Integer) filter.getValue()).doubleValue();
				} else {
					doubleValue = Double.parseDouble((String) filter.getValue());
				}
				switch (op) {
				case gt:
					predicate = cb.gt(path, doubleValue);
					break;

				case gte:
					predicate = cb.greaterThanOrEqualTo(path, doubleValue);
					break;

				case lt:
					predicate = cb.lt(path, doubleValue);
					break;

				case lte:
					predicate = cb.lessThanOrEqualTo(path, doubleValue);
					break;

				case neq:
					predicate = cb.notEqual(path, doubleValue);
					break;

				case eq:
					predicate = cb.equal(path, doubleValue);
					break;

				default:
					throw new Exception("Operator [" + filter.getOperator() + "] not supported for type [" + type + "]");
				}
				break;

			case "Date":
				Date dateValue = null;
				Date fromDate = null;
				Date toDate = null;

				if (valueType.equals("Date")) {
					dateValue = (Date) filter.getValue();
				} else if (valueType.equals("Timestamp")) {
					dateValue = (Date) filter.getValue();
				} else if (valueType.equals("Calendar") || valueType.equals("GregorianCalendar")) {
					dateValue = ((Calendar) filter.getValue()).getTime();
				} else {

					String dateString = (String) filter.getValue();

					// verify if dateString is a key
					Calendar calendar = DateUtils.truncate(Calendar.getInstance(), Calendar.DATE);
					switch (dateString) {

					// the following option are available from the menu in KendoGrid
					case "TODAY":
						fromDate = calendar.getTime();
						calendar.add(Calendar.DAY_OF_YEAR, 1);
						toDate = calendar.getTime();
						break;
					case "YESTERDAY":
						toDate = calendar.getTime();
						calendar.add(Calendar.DAY_OF_YEAR, -1);
						fromDate = calendar.getTime();
						break;
					case "LASTWEEK":
						calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
						toDate = calendar.getTime();
						calendar.add(Calendar.DAY_OF_YEAR, -7);
						fromDate = calendar.getTime();
						break;
					case "THISWEEK":
						calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
						fromDate = calendar.getTime();
						calendar.add(Calendar.DAY_OF_YEAR, 7);
						toDate = calendar.getTime();
						break;
					case "NEXTWEEK":
						calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
						calendar.add(Calendar.DAY_OF_YEAR, 7);
						fromDate = calendar.getTime();
						calendar.add(Calendar.DAY_OF_YEAR, 14);
						toDate = calendar.getTime();
						break;
					case "LASTMONTH":
						calendar.set(Calendar.DAY_OF_MONTH, 1);
						toDate = calendar.getTime();
						calendar.add(Calendar.MONTH, -1);
						fromDate = calendar.getTime();
						break;
					case "THISMONTH":
						calendar.set(Calendar.DAY_OF_MONTH, 1);
						fromDate = calendar.getTime();
						calendar.add(Calendar.MONTH, 1);
						toDate = calendar.getTime();
						break;
					case "LAST3DAYS":
						calendar.add(Calendar.DAY_OF_YEAR, 1);
						toDate = calendar.getTime();
						calendar.add(Calendar.DAY_OF_YEAR, -3);
						fromDate = calendar.getTime();
						break;
					case "LAST7DAYS":
						calendar.add(Calendar.DAY_OF_YEAR, 1);
						toDate = calendar.getTime();
						calendar.add(Calendar.DAY_OF_YEAR, -7);
						fromDate = calendar.getTime();
						break;
					case "LAST30DAYS":
						calendar.add(Calendar.DAY_OF_YEAR, 1);
						toDate = calendar.getTime();
						calendar.add(Calendar.DAY_OF_YEAR, -30);
						fromDate = calendar.getTime();
						break;
					case "LAST365DAYS":
						calendar.add(Calendar.DAY_OF_YEAR, 1);
						toDate = calendar.getTime();
						calendar.add(Calendar.DAY_OF_YEAR, -365);
						fromDate = calendar.getTime();
						break;

					// the following options are available for any filter
					case "TOMORROW":
						calendar.add(Calendar.DAY_OF_YEAR, 1);
						dateValue = calendar.getTime();
						break;
					case "FIRST_DAY_OF_MONTH":
						calendar.set(Calendar.DAY_OF_MONTH, 1);
						dateValue = calendar.getTime();
						break;
					case "LAST_DAY_OF_MONTH":
						calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
						dateValue = calendar.getTime();
						break;
					case "FIRST_DAY_OF_YEAR":
						calendar.set(Calendar.DAY_OF_YEAR, 1);
						dateValue = calendar.getTime();
						break;
					case "LAST_DAY_OF_YEAR":
						calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR));
						dateValue = calendar.getTime();
						break;
					case "LAST_SUNDAY":
						calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
						dateValue = calendar.getTime();
						break;
					case "LAST_MONDAY":
						calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
						dateValue = calendar.getTime();
						break;

					default:
						dateValue = DateUtil.parseDate(filter.getValue());
						break;
					}

					// if dateValue is not set, by default, use the fromDate
					// it may not represent the best value in all cases
					if (dateValue == null) {
						dateValue = fromDate;
					}
				}

				switch (op) {
				case gt:
					predicate = cb.greaterThan(path, dateValue);
					break;

				case gte:
					predicate = cb.greaterThanOrEqualTo(path, dateValue);
					break;

				case lt:
					predicate = cb.lessThan(path, dateValue);
					break;

				case lte:
					predicate = cb.lessThanOrEqualTo(path, dateValue);
					break;

				case neq:
					predicate = cb.notEqual(path, dateValue);
					break;

				case timestampEquals:
					predicate = cb.equal(path, dateValue);
					break;

				case sameDayEquals:
					// we only compare date (not datetime)
					dateValue = DateUtils.truncate(dateValue, Calendar.DATE);
					predicate = cb.equal(cb.function(truncFunction, Date.class, path), dateValue);
					break;

				case eq:
					if (fromDate == null) {
						// THIS IS THE DEFAULT FROM THE KENDO UI GRID.
						// if the database is a datetime and not a date, it will not work
						if (dateValue == null) {
							// date format must have been invalid. Compare with null (it will return nothing)
							predicate = cb.equal(path, dateValue);
						} else {
							if (dateValue.getTime() != DateUtils.truncate(dateValue, Calendar.DATE).getTime()) {
								logger.warn(
										"Comparing field[" + getTypeOfE().getSimpleName() + "." + filter.getField() + "] date only: " + dateValue + " (Use timestampEquals or sameDayEquals instead)");
							}

							// we only compare date (not datetime)
							dateValue = DateUtils.truncate(dateValue, Calendar.DATE);
							predicate = cb.equal(cb.function(truncFunction, Date.class, path), dateValue);
						}
					} else {
						// DYNAMIC dates
						// fromDate to toDate
						predicate = cb.and(cb.greaterThanOrEqualTo(path, fromDate), cb.lessThan(path, toDate));
					}
					break;

				case truncGt:
					dateValue = DateUtils.truncate(dateValue, Calendar.DATE);
					predicate = cb.greaterThan(cb.function(truncFunction, Date.class, path), dateValue);
					break;

				case truncGte:
					dateValue = DateUtils.truncate(dateValue, Calendar.DATE);
					predicate = cb.greaterThanOrEqualTo(cb.function(truncFunction, Date.class, path), dateValue);
					break;

				case truncLt:
					dateValue = DateUtils.truncate(dateValue, Calendar.DATE);
					predicate = cb.lessThan(cb.function(truncFunction, Date.class, path), dateValue);
					break;

				case truncLte:
					dateValue = DateUtils.truncate(dateValue, Calendar.DATE);
					predicate = cb.lessThanOrEqualTo(cb.function(truncFunction, Date.class, path), dateValue);
					break;

				default:
					throw new Exception("Operator [" + filter.getOperator() + "] not supported for type [" + type + "]");
				}
				break;

			case "char":
				char charValue = ((String) filter.getValue()).charAt(0);
				switch (op) {
				case eq:
					predicate = cb.equal(path, charValue);
					break;
				default:
					throw new Exception("Operator [" + filter.getOperator() + "] not supported for type [" + type + "]");
				}
				break;

			case "String":
				String stringValue = null;
				List<String> stringValues = null;
				Expression stringPath = path;

				if (valueType.equals("String") && (filter.getOperator().equals(Operator.eq) || filter.getOperator().equals(Operator.neq) || !caseSensitive)) {
					stringValue = (String) filter.getValue();
				} else if (valueType.equals("ArrayList")) {
					stringValues = (List<String>) filter.getValue();
				} else if (valueType.equals("HashSet")) {
					stringValues = new ArrayList<>((Set<String>) filter.getValue());
				} else if (valueType.equals("String[]")) {
					stringValues = Arrays.asList((String[]) filter.getValue());
				} else { // valueType.equals("String")
					// compare lowercase only
					stringValue = ((String) filter.getValue()).toLowerCase();
					stringPath = cb.lower(path);
				}

				switch (op) {
				case eq:
				case equalsNoKey:
					predicate = cb.equal(stringPath, stringValue);
					break;
				case equalsIgnoreCase:
					predicate = cb.equal(stringPath, stringValue);
					break;
				case neq:
					predicate = cb.notEqual(stringPath, stringValue);
					break;
				case startsWith:
				case startswith:
					predicate = cb.like(stringPath, stringValue + "%");
					break;
				case doesNotStartWith:
					predicate = cb.notLike(stringPath, stringValue + "%");
					break;
				case endsWith:
				case endswith:
					predicate = cb.like(stringPath, "%" + stringValue);
					break;
				case doesNotEndWith:
					predicate = cb.notLike(stringPath, "%" + stringValue);
					break;
				case doesNotContain:
				case doesnotcontain:
					predicate = cb.notLike(stringPath, "%" + stringValue + "%");
					break;
				case contains:
					// if the string contains space search for each term separately
					if (stringValue.trim().contains(" ")) {
						List<Predicate> predicates = new ArrayList<>();
						for (String s : stringValue.trim().split(" ")) {
							predicates.add(cb.like(stringPath, "%" + s + "%"));
						}
						predicate = cb.and(predicates.toArray(new Predicate[0]));

					} else {
						predicate = cb.like(stringPath, "%" + stringValue.trim() + "%");
					}
					break;

				case trimCompare:
					predicate = cb.equal(cb.trim(stringPath), stringValue.trim());
					break;

				case neqTrimCompare:
					predicate = cb.notEqual(cb.trim(stringPath), stringValue.trim());
					break;

				case in:
					distinctNeeded = true;
					if (stringValues == null) {
						stringValues = new ArrayList<String>();
						if (stringValue != null) {
							stringValues.add(stringValue);
						}
					}
					predicate = cb.or(getInPredicate(inMaxValues, stringValues, path, cb, false).toArray(new Predicate[0]));
					break;
				case notIn:
					distinctNeeded = true;
					if (stringValues == null) {
						stringValues = new ArrayList<String>();
						if (stringValue != null) {
							stringValues.add(stringValue);
						}
					}
					predicate = cb.not(cb.or(getInPredicate(inMaxValues, stringValues, path, cb, false).toArray(new Predicate[0])));
					break;
				case trimIn:
					distinctNeeded = true;
					if (stringValues == null) {
						stringValues = new ArrayList<String>();
						if (stringValue != null) {
							stringValues.add(stringValue);
						}
					}
					stringValues.replaceAll(String::trim);
					predicate = cb.or(getInPredicate(inMaxValues, stringValues, path, cb, true).toArray(new Predicate[0]));
					break;
				default:
					throw new Exception("Operator [" + filter.getOperator() + "] not supported for type [" + type + "]");
				}
				break;
			case "boolean":
			case "Boolean":
				Boolean boolValue;
				if (valueType.equals("Boolean")) {
					boolValue = (Boolean) filter.getValue();
				} else {
					boolValue = Boolean.parseBoolean((String) filter.getValue());
				}
				switch (op) {
				case neq:
					predicate = cb.notEqual(path, boolValue);
					break;
				case eq:
					predicate = cb.equal(path, boolValue);
					break;

				default:
					throw new Exception("Operator [" + filter.getOperator() + "] not supported for type [" + type + "]");
				}
				break;

			case "Set":
				// this is for many to many in Hibernate5
				// it will not work in Hibernate6 as getJavaType will not return a Set, but the generic type
				this.distinctNeeded = true;
				Integer integerSetValue;
				if (valueType.equals("Integer")) {
					integerSetValue = (Integer) filter.getValue();
				} else if (valueType.equals("Double")) {
					integerSetValue = ((Double) filter.getValue()).intValue();
				} else {
					integerSetValue = Integer.parseInt((String) filter.getValue());
				}
				switch (op) {
				case eq:
					// usually from a grid dropdownlist
					predicate = cb.isMember(cb.literal(integerSetValue), (Expression<Set<Integer>>) path);
					break;
				case contains:
					predicate = cb.isMember(cb.literal(integerSetValue), (Expression<Set<Integer>>) path);
					break;
				case doesNotContain:
				case doesnotcontain:
					predicate = cb.isNotMember(cb.literal(integerSetValue), (Expression<Set<Integer>>) path);
					break;

				default:
					throw new Exception("Operator [" + filter.getOperator() + "] not supported for type [" + type + "]");
				}
				break;

			default:
				if (path.getJavaType().isAnnotationPresent(Embeddable.class)) {
					predicate = cb.equal(path, filter.getValue());
				} else if (!path.getJavaType().isEnum()) {
					throw new Exception("Type [" + type + "] valueType [" + valueType + "] not supported");
				} else {
					// The field is an Enum
					try {
						Enum enumValue;
						if (valueType.equals("String")) {
							enumValue = Enum.valueOf((Class<Enum>) path.getJavaType(), (String) filter.getValue());
						} else {
							enumValue = (Enum) filter.getValue();
						}

						switch (op) {
						case eq:
							predicate = cb.equal(path, enumValue);
							break;
						case neq:
							predicate = cb.notEqual(path, enumValue);
							break;

						default:
							throw new Exception("Operator [" + filter.getOperator() + "] not supported for type [" + type + "]");
						}
					} catch (IllegalArgumentException ex) {
						throw new Exception("Value [" + filter.getValue() + "] not supported by enum [" + path.getJavaType() + "]");
					}
				}
				break;
			}
		}

		return predicate;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<Predicate> getInPredicate(Integer inMaxValues, List<? extends Object> values, Path path, CriteriaBuilder cb, boolean trim) {
		List<Predicate> partitionPredicates = new ArrayList<>();
		if (inMaxValues != null) {
			List<?> partition = ListUtils.partition(values, inMaxValues);
			for (Object object : partition) {
				List<Object> objects = (List<Object>) object;
				partitionPredicates.add(trim ? cb.trim(path).in(objects) : path.in(objects));
			}
		} else {
			partitionPredicates.add(trim ? cb.trim(path).in(values) : path.in(values));
		}
		return partitionPredicates;
	}

	/**
	 *
	 * @param cb
	 * @param from
	 * @param filter
	 * @return
	 * @throws Exception
	 */
	final private Predicate buildPredicates(CriteriaBuilder cb, From<?, ?> from, Filter filter, Map<String, Join<?, ?>> joinMap, boolean fetch) throws Exception {
		Predicate predicate = null;

		if (filter != null) {

			try {
				if (filter.getFilters() != null) {
					List<Predicate> restrictions = new ArrayList<>();

					if (filter.getField() != null) {
						logger.warn("Do we really need this special case:" + filter);
						// this is a special case when we want to apply filters to the same join
						from = from.join(filter.getField(), JoinType.INNER);
						this.distinctNeeded = true;
					}

					// build predicates for each sub filters
					for (Filter p : filter.getFilters()) {
						Predicate subPredicate = buildPredicates(cb, from, p, joinMap, fetch);
						if (subPredicate != null) {
							restrictions.add(subPredicate);
						}
					}

					// build the predicate
					if (!restrictions.isEmpty()) {
						if (filter.getLogic().equals(Logic.or)) {
							predicate = cb.or(restrictions.toArray(new Predicate[0]));
						} else {
							predicate = cb.and(restrictions.toArray(new Predicate[0]));
						}
					}
				} else if (filter.getField() != null) {
					// get the predicate for this field
					predicate = getPredicate(cb, retrieveProperty(from, filter.getField(), joinMap, fetch), filter);
				}
			} catch (AttributeNotFoundException ex) {
				if (from == null || from.getJavaType() == null || filter == null || filter.getField() == null
						|| (!from.getJavaType().getSimpleName().equals("Document") && !from.getJavaType().getSimpleName().equals("AuditTrail") && !filter.getField().equals("unused"))) {
					throw ex;
				}
			}
		}
		return predicate;
	}

	public final void setRefreshAfterMerge(boolean refreshAfterMerge) {
		this.refreshAfterMerge = refreshAfterMerge;
	}

	public void setUpdateLastModified(boolean updateLastModified) {
		this.updateLastModified = updateLastModified;
	}

	public String getLink(E e) throws Exception {
		return getLink(e, null);
	}

	public String getLink(E e, Map<String, String> params) throws Exception {
		String keyField = getKeyField();
		Object keyValue = BeanUtils.getProperty(e, keyField);
		String url = SystemEnv.INSTANCE.getDefaultProperties().getProperty("base_url");
		if (params != null) {
			for (String name : params.keySet()) {
				String value = params.get(name);
				url += (url.contains("?") ? "&" : "?") + (name + "=" + value);
			}
		}

		return url + "#" + getApplicationName() + "(" + getKeyField() + "-" + keyValue + ")(" + e.getId() + ")";
	}

	/**
	 * 
	 * @param basicEntity
	 * @return
	 */
	private Class<?> getEntityFromBasicEntity(Class<?> basicEntity) {
		for (java.lang.reflect.Type type : basicEntity.getGenericInterfaces()) {
			if (type instanceof BasicEntity || type.getTypeName().startsWith(BasicEntity.class.getName())) {
				return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
			}
		}
		return basicEntity;
	}

	/**
	 * Generate all base files for a resource manager
	 *
	 * @throws Exception
	 */
	public void generateResourceManager(String namespace) throws Exception {
		Field parentEntityField = getParentEntityField();
		String resourceManagerName = getResourceManager();
		String resourcePath = StringUtils.uncapitalize(getTypeOfE().getSimpleName());
		if (parentEntityField != null && resourcePath.startsWith(parentEntityField.getName())) {
			resourcePath = StringUtils.uncapitalize(resourcePath.substring(parentEntityField.getName().length()));
		}

		Map<String, AppClassField> appClassFieldMap = getAppClassFieldMap(false);

		// remove type
		appClassFieldMap.remove("type");

		StringBuilder sb = new StringBuilder();

		// create a temp directory
		java.nio.file.Path resourceManagerPath = Files.createTempDirectory(resourceManagerName);

		// app_class.js
		sb = new StringBuilder();
		// if (namespace != null && namespace.length() > 0) {
		// String ns = "";
		//
		// // start from the top
		// String[] namespaces = namespace.split("\\.");
		// for (int i = 0; i < namespaces.length; i++) {
		// if (i == 0) {
		// ns = namespaces[i];
		// sb.append("var ");
		// } else {
		// ns = ns + "." + namespaces[i];
		// }
		// sb.append(ns + " = " + ns + " || {};\n");
		// }
		// sb.append("\n");
		// }

		// for sub manager, it does not work
		// ValueManager
		// NOT KpiManager.KpiValueManager
		String managerName = resourceManagerName;
		if (getParentEntityField() != null) {
			String parentApplicationName = getParentEntityService().getResourceManager();

			if (parentApplicationName.endsWith("Manager") && managerName.endsWith("Manager")) {
				parentApplicationName = parentApplicationName.substring(0, parentApplicationName.length() - "Manager".length());
				managerName = managerName.substring(managerName.lastIndexOf('.') + 1);
				if (managerName.startsWith(parentApplicationName)) {
					managerName = managerName.substring(parentApplicationName.length());
				}
			}
		}

		sb.append(namespace + "." + managerName + " = " + "expresso.layout.resourcemanager.ResourceManager.extend({\n");
		sb.append("\n");
		sb.append("    // @override\n");
		sb.append("    init: function (applicationPath) {\n");
		sb.append("        var fields = " + getAppClassFields(false) + ";\n");
		sb.append("\n");
		sb.append("        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, \"" + resourcePath + "\", fields, {\n");
		sb.append("            preview: false\n");
		sb.append("        });\n");
		sb.append("    }\n");
		sb.append("});\n");

		File appClassFile = new File(resourceManagerPath.toFile().getAbsolutePath() + File.separator + "app_class.js");
		FileUtils.write(appClassFile, sb.toString(), StandardCharsets.UTF_8);

		// form.html
		sb = new StringBuilder();
		sb.append("<div class=\"exp-form\">\n");

		// fields (each 2 fields, add a div)
		int count = 0;
		for (String fieldName : appClassFieldMap.keySet()) {
			AppClassField appClassField = appClassFieldMap.get(fieldName);
			if (count % 2 == 0) {
				if (count != 0) {
					sb.append("    </div>\n\n");
				}
				sb.append("    <div>\n");
			}
			count++;

			if (appClassField.getReference() != null || appClassField.getValues() != null) {
				sb.append("        <select name=\"" + fieldName + "\"></select>\n");
			} else if (appClassField.getMaxLength() != null && appClassField.getMaxLength() > 500) {
				sb.append("        <textarea name=\"" + fieldName + "\"></textarea>\n");
			} else {
				sb.append("        <input name=\"" + fieldName + "\">\n");
			}
		}
		sb.append("    </div>\n");
		sb.append("</div>\n");

		File formHtmlFile = new File(resourceManagerPath.toFile().getAbsolutePath() + File.separator + "form.html");
		FileUtils.write(formHtmlFile, sb.toString(), StandardCharsets.UTF_8);

		// form.js
		sb = new StringBuilder();
		sb.append(namespace + ".Form = expresso.layout.resourcemanager.Form.extend({\n");
		sb.append("\n");
		sb.append("    // @override\n");
		sb.append("    initForm: function ($window, resource) {\n");
		sb.append("        expresso.layout.resourcemanager.Form.fn.initForm.call(this, $window, resource);\n");
		sb.append("    }\n");
		sb.append("});\n");

		File formJsFile = new File(resourceManagerPath.toFile().getAbsolutePath() + File.separator + "form.js");
		FileUtils.write(formJsFile, sb.toString(), StandardCharsets.UTF_8);

		// grid.html
		sb = new StringBuilder();
		sb.append("<div class=\"exp-grid\"></div>\n");
		File gridHtmlFile = new File(resourceManagerPath.toFile().getAbsolutePath() + File.separator + "grid.html");
		FileUtils.write(gridHtmlFile, sb.toString(), StandardCharsets.UTF_8);

		// grid.js
		sb = new StringBuilder();
		sb.append(namespace + ".Grid = expresso.layout.resourcemanager.Grid.extend({\n");
		sb.append("\n");
		sb.append("    // @override\n");
		sb.append("    getColumns: function () {\n");

		if (getParentEntityField() != null) {
			sb.append("        var columns = [];\n");
			sb.append("        if (this.resourceManager.siblingResourceManager || this.resourceManager.displayAsMaster) {\n");
			sb.append("            //columns.push({\n");
			sb.append("            //   field: \"parent.name\",\n");
			sb.append("            //   width: 250\n");
			sb.append("            //});\n");
			sb.append("         }\n\n");
			sb.append("         columns.push({\n");
		} else {
			sb.append("        var columns = [{\n");
		}

		// columns
		for (String fieldName : appClassFieldMap.keySet()) {
			String field = fieldName;
			AppClassField appClassField = appClassFieldMap.get(fieldName);
			if (appClassField.getReference() != null) {
				if (field.endsWith("Id")) {
					// remove the ID
					field = field.substring(0, field.length() - 2);
					if (appClassField.getReference() instanceof String && ((String) appClassField.getReference()).equals("user")) {
						field += ".fullName";
					} else if (appClassField.getReference() instanceof Reference) {
						// Reference reference = (Reference) appClassField.getReference();
						// do no put the reference in columns
						// use formula instead
						continue;
					} else {
						field += ".description";
					}
				} else {
					// do not include Ids (multiple)
					continue;
				}
			}
			sb.append("            field: \"" + field + "\"");

			// be default, hide some fields
			if (fieldName.equals("deactivationDate")) {
				sb.append(",\n");
				sb.append("            hidden: true");
			}

			if (appClassField.getType() != null && (appClassField.getType().equals("date") || appClassField.getType().equals("boolean"))) {
				// no width
				sb.append("\n");
			} else {
				sb.append(",\n");
				int width;
				if (field.equals("sortOrder")) {
					width = 70;
				} else if (field.equals("description")) {
					width = 250;
				} else if (appClassField.getType() != null && appClassField.getType().equals("number") && appClassField.getReference() == null) {
					width = 100;
				} else if (field.endsWith("No")) {
					width = 80;
				} else if (appClassField.getMaxLength() != null && appClassField.getMaxLength() > 100) {
					width = 200;
				} else {
					width = 120;
				}
				sb.append("            width: " + width + "\n");
			}

			sb.append("        }, {\n");
		}
		if (getParentEntityField() != null) {
			sb.append("        });\n");
		} else {
			sb.append("        }];\n");
		}

		sb.append("        return columns;\n");
		sb.append("    },\n\n");

		// getMobileColumns
		sb.append("    // @override\n");
		sb.append("    getMobileColumns: function () {\n");
		sb.append("        return {\n");
		sb.append("            mobileNumberFieldName: \"" + getKeyField() + "\",\n");
		sb.append("            mobileTopRightFieldName: null,\n");
		sb.append("            mobileMiddleLeftFieldName: null,\n");
		sb.append("            mobileMiddleRightFieldName: null,\n");
		sb.append("            mobileDescriptionFieldName: null\n");
		sb.append("        };\n");
		sb.append("    }\n");

		// end of grid.js
		sb.append("});\n");

		File gridJsFile = new File(resourceManagerPath.toFile().getAbsolutePath() + File.separator + "grid.js");
		FileUtils.write(gridJsFile, sb.toString(), StandardCharsets.UTF_8);

		// labels.js
		sb = new StringBuilder();
		sb.append(namespace + ".Labels = {\n");
		sb.append("    " + StringUtils.uncapitalize(getTypeOfE().getSimpleName()) + ": " + "\"" + getTypeOfE().getSimpleName() + "\",\n");
		sb.append("    " + StringUtils.uncapitalize(getTypeOfE().getSimpleName()) + "s" + ": " + "\"" + getTypeOfE().getSimpleName() + "s\",\n");

		// fields
		for (String fieldName : appClassFieldMap.keySet()) {
			if (fieldName.equals("deactivationDate") || fieldName.equals("sortOrder") || fieldName.equals("pgmKey") || fieldName.equals("description")) {
				// skip
			} else {
				String field = fieldName;
				if (field.endsWith("Id")) {
					field = field.substring(0, field.length() - 2);
				}
				sb.append("    " + field + ": \"" + StringUtils.capitalize(field) + "\",\n");
			}
		}
		sb.append("\n");
		sb.append("    _: \"\"\n");
		sb.append("};\n");
		File labelsFile = new File(resourceManagerPath.toFile().getAbsolutePath() + File.separator + "labels.js");
		FileUtils.write(labelsFile, sb.toString(), StandardCharsets.UTF_8);

		// copy the label.js to label_en.js
		File labelsEnglishFile = new File(resourceManagerPath.toFile().getAbsolutePath() + File.separator + "labels_en.js");
		FileUtils.copyFile(labelsFile, labelsEnglishFile);

		// all files are done now
		if (getResponse() != null) {
			// zip the directory
			File zipFile = File.createTempFile(resourceManagerName, ".zip");

			// build an array with the absolute path of each file
			File[] files = resourceManagerPath.toFile().listFiles();
			String[] filePaths = new String[files.length];
			for (int i = 0; i < files.length; i++) {
				filePaths[i] = files[i].getAbsolutePath();
			}

			ZipUtil.zip(filePaths, zipFile.getAbsolutePath());

			// send back the ZIP file
			Util.downloadFile(getResponse(), zipFile);

		} else {
			logger.debug("Resource manager: " + resourceManagerPath.toString());
		}
	}

	/**
	 * Get the list of fields to be included in the app_class.js
	 *
	 * @return
	 * @throws Exception
	 */
	public String getAppClassFields(boolean jsonCompliance) throws Exception {
		Map<String, AppClassField> appClassFieldMap = getAppClassFieldMap(jsonCompliance);
		String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(appClassFieldMap);
		if (!jsonCompliance) {
			// remove the " for key (it is not JSON compliant, but it is better for the app_class
			jsonString = jsonString.replaceAll("\"([a-zA-Z0-9]+)\":", "$1:").replaceAll("\"NULL\"", "null");

			// pad left with spaces
			jsonString = jsonString.replaceAll("\n", "\n          ");

			// last line, we need to remove 2 spaces
			jsonString = jsonString.replaceAll("  }$", "}");
		}
		return jsonString;
	}

	/**
	 * Get the list of fields to be included in the app_class.js
	 *
	 * @return
	 * @throws Exception
	 */
	public Map<String, AppClassField> getAppClassFieldMap(boolean includeName) throws Exception {
		Map<String, AppClassField> appClassFieldMap = new LinkedHashMap<>();

		// inner class to store column meta data
		class ColumnMetaData {
			String columnsize;
			String isNullable;
			String datatype;
			String decimals;

			@Override
			public String toString() {
				return "ColumnMetaData [columnsize=" + columnsize + ", isNullable=" + isNullable + ", datatype=" + datatype + ", decimals=" + decimals + "]";
			}
		}
		Map<String, ColumnMetaData> columnMap = new HashMap<>();

		Class<?> c = getTypeOfE();
		Connection connection = getConnection(false);
		Table table = c.getAnnotation(Table.class);
		if (table == null) {
			// not an entity based
			return null;
		}
		DatabaseMetaData databaseMetaData = connection.getMetaData();
		boolean usingSchema = table.schema() != null && table.schema().length() > 0;

		// logger.info("Getting Database meta data for [" + (usingSchema ? table.schema() + "." : "") + table.name() + "]");

		// NOTE: Oracle is case sensitive for schema and table names
		ResultSet columns = databaseMetaData.getColumns(null, usingSchema ? table.schema().toUpperCase() : null, usingSchema ? table.name().toUpperCase() : table.name(), null);
		boolean found = false;
		while (columns.next()) {
			found = true;
			ColumnMetaData columnMetaData = new ColumnMetaData();
			String columnName = columns.getString("COLUMN_NAME");
			columnMetaData.columnsize = columns.getString("COLUMN_SIZE");
			columnMetaData.isNullable = columns.getString("IS_NULLABLE");
			columnMetaData.datatype = columns.getString("DATA_TYPE");
			columnMetaData.decimals = columns.getString("DECIMAL_DIGITS");
			// logger.debug(columnName + "= " + columnMetaData);
			columnMap.put(columnName.toLowerCase(), columnMetaData);
		}

		if (!found) {
			throw new Exception("Table not found [" + (usingSchema ? table.schema() + "." : "") + table.name() + "]");
		}

		// Get Bean info
		BeanInfo info = Introspector.getBeanInfo(c);
		PropertyDescriptor[] props = info.getPropertyDescriptors();

		// Create the mandatory "type"
		AppClassField appClassField = new AppClassField();
		appClassField.setType("string");
		if (includeName) {
			appClassField.setName("type");
		}
		appClassField.setEditable(false);
		appClassField.setDefaultValue(StringUtils.uncapitalize(c.getSimpleName()));
		appClassFieldMap.put("type", appClassField);

		// get the list of fields
		do {
			Field[] allFields = c.getDeclaredFields();
			for (Field field : allFields) {
				String fieldName = field.getName();

				// those fields are defined by default in the app_class
				switch (fieldName) {
				case "derived":
				case "creationDate":
				case "lastModifiedDate":
				case "creationUser":
				case "creationUserId":
				case "lastModifiedUser":
				case "lastModifiedUserId":
				case "lastModifiedUserFullName":
				case "creationUserFullName":
					continue;
				}

				// do not export static field
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}

				Class<?> type = field.getType();
				if (fieldName.equals("id") && type.getCanonicalName().equals("java.lang.Integer")) {
					continue; // no need. This is the Expresso default
				}

				// if there is no getter, do not output them
				boolean getterFound = false;
				for (PropertyDescriptor pd : Introspector.getBeanInfo(c, Object.class).getPropertyDescriptors()) {
					String name = pd.getName();
					Method getter = pd.getReadMethod();
					if (name.equals(fieldName) && getter != null) {
						getterFound = true;
						break;
					}
				}
				if (!getterFound) {
					if (fieldName.endsWith("StringIds")) {
						// ok
					} else {
						logger.debug("Skipping [" + fieldName + "] from [" + getResourceName() + "] No getter method");
					}
					continue;
				}

				appClassField = null;

				// if the field is an Id, try to get the Object appClassField
				if (fieldName.endsWith("Id")) {
					appClassField = appClassFieldMap.get(fieldName);
				} else {
					// if the first is an Object, try to get the Id
					appClassField = appClassFieldMap.get(fieldName + "Id");
				}

				if (appClassField == null) {
					appClassField = new AppClassField();
				}

				Column column = field.getAnnotation(Column.class);
				// Id id = field.getAnnotation(Id.class);

				ColumnMetaData columnMetaData = null;
				if (column != null) {
					columnMetaData = columnMap.get(column.name().toLowerCase());
					// logger.debug(column.name().toLowerCase() + ":" + columnMetaData);
				}

				String appClassFieldType = getAppClassFieldType(type);
				if (appClassFieldType == null) {

					// Object: not a primitive type
					OneToOne oneToOne = field.getAnnotation(OneToOne.class);
					ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
					ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
					JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);

					if (manyToOne != null) {
						Object ref = true;
						if (field.getGenericType() != null) {
							// ex: ca.cezinc.expressoservice.resources.humanresources.person.User
							String refString = getEntityFromBasicEntity((Class<?>) field.getGenericType()).getSimpleName();
							refString = StringUtils.uncapitalize(refString);
							if (!fieldName.equals(refString)) {
								ref = refString;
							}
						}

						if (manyToOne.fetch() != null && manyToOne.fetch() == FetchType.EAGER) {
							// Only BaseType or
							if (IBaseType.class.isAssignableFrom(type)) {
								appClassField.setValues(ref);
							} else {
								appClassField.setReference(ref);
							}
						} else {
							appClassField.setReference(ref);
						}

						// we do not export the Object for the app_class
						// Only the Id

						// verify if the ID exist
						fieldName += "Id";
						appClassField.setType("number");
						if (Util.getField(getTypeOfE(), fieldName) == null) {
							// this could happen when using a Formula or JoinFormula to an transient object
							continue;
						}

						if (joinColumn != null) {
							columnMetaData = columnMap.get(joinColumn.name().toLowerCase());
						}
					} else if (oneToOne != null) {
						// ok
					} else if (manyToMany != null) {
						// get the Set<?> and if it is not the name of
						// the attribute, then set the name of the reference
						Object ref = true;
						if (field.getGenericType() != null) {
							// ex:
							// java.util.Set<ca.cezinc.expressoservice.resources.humanresources.person.User>

							String refString = field.getGenericType().toString();
							refString = refString.substring(refString.lastIndexOf('.') + 1);
							refString = refString.substring(0, refString.length() - 1);
							refString = StringUtils.uncapitalize(refString);
							if (!fieldName.equals(refString)) {
								ref = refString;
							}
						}

						appClassField.setReference(ref);
						appClassField.setMultipleSelection(true);
						if (fieldName.endsWith("ies")) {
							// remove the "ies" and add "yIds"
							fieldName = fieldName.substring(0, fieldName.length() - 3) + "yIds";
						} else {
							// remove the "s" and add "Ids"
							fieldName = fieldName.substring(0, fieldName.length() - 1) + "Ids";
						}

						// if there is no getter/setter, do not export
						if (Util.getField(getTypeOfE(), fieldName) == null) {
							// this could happen when using a Formula or JoinFormula to an transient object
							continue;
						}

					} else {
						// logger.warn(String.format("Type [%s] not supported",
						// type.getCanonicalName()));
						continue;
					}
				} else {
					appClassField.setType(appClassFieldType);

					if (appClassFieldType.equals("string")) {
						if (columnMetaData != null && columnMetaData.columnsize != null) {
							appClassField.setMaxLength(Integer.parseInt(columnMetaData.columnsize));
						}
					} else if (appClassFieldType.equals("number") && !fieldName.endsWith("Id")) {
						int decimals;
						if (columnMetaData != null && columnMetaData.decimals != null && columnMetaData.decimals.length() > 0) {
							try {
								decimals = Integer.parseInt(columnMetaData.decimals);
								if (decimals == -127) {
									// in Oracle, it means no decimal
									decimals = 0;
								}
							} catch (Exception e) {
								decimals = 0;
							}
						} else {
							// try to determine the number of decimals from the type
							switch (type.getCanonicalName()) {
							case "float":
							case "double":
							case "java.lang.Float":
							case "java.lang.Double":
							case "java.lang.BigDecimal":
								decimals = 2;
								break;

							case "short":
							case "int":
							case "long":
							case "java.lang.Short":
							case "java.lang.Integer":
							case "java.lang.Long":
							default:
								decimals = 0;
								break;
							}
						}
						appClassField.setDecimals(decimals);
						// appClassField.setAllowNegative(false); // Default is false
						appClassField.setDefaultValue("NULL");
					} else if (appClassFieldType.equals("date")) {
						Temporal temporal = field.getAnnotation(Temporal.class);
						if (temporal != null && temporal.value() != null) {
							if (temporal.value().equals(TemporalType.TIMESTAMP)) {
								appClassField.setTimestamp(true);
							} else if (temporal.value().equals(TemporalType.TIME)) {
								appClassField.setTimeOnly(true);
							}
						}
					}
				}

				// if it is a formula, it cannot be saved, set it transient
				if (field.isAnnotationPresent(Formula.class)) {
					appClassField.setTransient(true);
				}

				if (columnMetaData != null) {
					boolean nullable = columnMetaData.isNullable != null && columnMetaData.isNullable.equals("YES");
					if (nullable) {
						appClassField.setNullable(true);
					}

					// verify is the field is not nullable but the database is nullable (or
					// vice-versa)
					if (type.isPrimitive() && nullable) {
						appClassField.addNote("Class type is a primitive [" + type.getSimpleName() + "] but column is nullable");
					} else if (!type.isPrimitive() && !nullable) {
						if (fieldName.endsWith("Id")) {
							// ok, this is by design: all IDs are mapped as Integer
						} else if (type.getSimpleName().equals("String") || type.getSimpleName().equals("Date")) {
							// String and Date cannot be a primitive, so there is no way to know
						} else {
							appClassField.addNote("Class type is NOT a primitive [" + type.getSimpleName() + "] but column is NOT nullable");
						}
					}
				}

				// set the name
				if (includeName) {
					appClassField.setName(fieldName);
				}

				if (fieldName.endsWith("Key")) {
					appClassField.setUnique(true);
				}

				if (/* fieldName.endsWith("No") && */ Arrays.asList(getKeyFields()).contains(fieldName) && !fieldName.endsWith("id")) {
					appClassField.setKeyField(true);
					appClassField.setEditable(false);
				}

				if (fieldName.equals("sortOrder")) {
					appClassField.setDefaultValue(1);
				}

				if (field.isAnnotationPresent(HierarchicalParentEntity.class)) {
					appClassField.setHierarchicalParent(true);
				}

				// verify if the field is protected by a restricted role
				if (field.isAnnotationPresent(FieldRestriction.class)) {
					appClassField.setRestrictedRole(FieldRestrictionUtil.INSTANCE.getFieldRestrictionRole(getResourceName(), fieldName));
				}

				// verify if the field is protected by approbation for modification
				if (getTypeOfE().getAnnotation(RequireApproval.class) != null && field.isAnnotationPresent(RequireApproval.class)) {
					String requireApprovalRole = field.getAnnotation(RequireApproval.class).role();
					if (requireApprovalRole == null || requireApprovalRole.length() == 0) {
						// get it from the resource
						requireApprovalRole = getTypeOfE().getAnnotation(RequireApproval.class).role();
					}
					appClassField.setRequireApprovalRole(requireApprovalRole);
				}

				// add the new field to the map
				appClassFieldMap.put(fieldName, appClassField);
			}

			// then get all method with @XmlElement that are primitive
			Method[] methods = c.getDeclaredMethods();
			for (Method method : methods) {
				XmlElement xmlElement = method.getAnnotation(XmlElement.class);
				if (xmlElement != null) {
					String fieldName = null;
					for (PropertyDescriptor pd : props) {
						if (method.equals(pd.getReadMethod())) {
							fieldName = pd.getName();
						}
					}
					if (fieldName != null) {
						// if the appClassFieldMap already contains the fieldName, skip it
						if (!appClassFieldMap.containsKey(fieldName + "Id") && !appClassFieldMap.containsKey(fieldName) && !fieldName.equals("creationUser") && !fieldName.equals("lastModifiedUser")
								&& !fieldName.equals("label") && !fieldName.equals("lastModifiedUserFullName") && !fieldName.equals("creationUserFullName")) {
							String appClassFieldType = getAppClassFieldType(method.getReturnType());
							if (appClassFieldType != null) {
								appClassField = new AppClassField();
								appClassField.setType(appClassFieldType);
								if (includeName) {
									appClassField.setName(fieldName);
								}
								appClassField.setTransient(true);
								appClassField.setFilterable(false);
								appClassFieldMap.put(fieldName, appClassField);
							} else {
								// what to do with object?
								// for now, ignore them
							}
						}
					}
				}
			}
		} while ((c = c.getSuperclass()) != null);

		return appClassFieldMap;
	}

	private String getAppClassFieldType(Class<?> c) {
		String type;
		switch (c.getCanonicalName()) {
		case "boolean":
		case "java.lang.Boolean":
			type = "boolean";
			break;
		case "long":
		case "int":
		case "short":
		case "float":
		case "double":
		case "java.lang.Long":
		case "java.lang.Integer":
		case "java.lang.Short":
		case "java.lang.Float":
		case "java.lang.Double":
		case "java.lang.BigDecimal":
			type = "number";
			break;
		case "java.util.Date":
			type = "date";
			break;
		case "java.lang.String":
			type = "string";
			break;
		case "char":
			type = "string";
			break;
		default:
			type = null;
			break;
		}
		return type;
	}

	final public void sync() throws Exception {
		sync((String) null, null);
	}

	final public void sync(String section, ProgressSender progressSender) throws Exception {
		sync(section, progressSender, 100);
	}

	/**
	 * Provide a standard method for Entity Service to synchronize with external resources
	 *
	 * @param section
	 * @param progressSender
	 * @param progressWeight
	 * @throws Exception
	 */
	public void sync(String section, ProgressSender progressSender, int progressWeight) throws Exception {
		// by default, do nothing
	}

	public void sync(MultivaluedMap<String, String> formParams, ProgressSender progressSender) throws Exception {
		// if not overwritten, call the former sync(section)
		sync(formParams != null ? formParams.getFirst("section") : (String) null, progressSender, 100);
	}

	/**
	 * Get the properties for the application
	 *
	 * @return
	 */
	public Properties getApplicationConfigProperties() {
		String applicationFolder = getTypeOfE().getSimpleName().toLowerCase();
		return getApplicationConfigProperties(applicationFolder, "config");
	}

	/**
	 * Get a service based on the resource program key **** Use with caution: this method is not efficient ****
	 *
	 * @param resourceName
	 * @return
	 */
	@SuppressWarnings({ "unchecked" })
	final public <S extends AbstractBaseEntityService<T, U, I>, T extends IEntity<I>> S newService(String resourceName) {

		try {
			Class<T> entityClass = (Class<T>) Util.findEntityClassByName(StringUtils.capitalize(resourceName));
			Class<S> serviceClass = (Class<S>) Class.forName(entityClass.getCanonicalName() + "Service");

			S service = serviceClass.getDeclaredConstructor().newInstance();

			service.setTypeOfE(entityClass);
			service.setRequest(getRequest());
			service.setResponse(getResponse());

			// put service in request to be closed
			registerService(service);

			return service;
		} catch (Exception ex) {
			logger.warn("Problem creating the service for resource [" + resourceName + "]", ex);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	static public <S extends AbstractBaseEntityService<T, V, J>, T extends IEntity<J>, V extends IUser, J> S newServiceStatic(Class<S> serviceClass, Class<T> entityClass) {
		// always starts a transaction for a static service (usually from the main)
		return newServiceStatic(serviceClass, entityClass, (V) UserManager.getInstance().getUser(), true);
	}

	@SuppressWarnings("unchecked")
	static public <S extends AbstractBaseEntityService<T, V, J>, T extends IEntity<J>, V extends IUser, J> S newServiceStatic(Class<S> serviceClass, Class<T> entityClass, boolean startTransaction) {
		return newServiceStatic(serviceClass, entityClass, (V) UserManager.getInstance().getUser(), startTransaction);
	}

	static public <S extends AbstractBaseEntityService<T, V, J>, T extends IEntity<J>, V extends IUser, J> S newServiceStatic(Class<S> serviceClass, Class<T> entityClass, V user) {
		return newServiceStatic(serviceClass, entityClass, user, true);
	}

	/**
	 * This method is used only for testing purpose (using a main method)
	 *
	 * @param serviceClass
	 * @param entityClass
	 * @param em
	 * @return
	 * @throws Exception
	 * @throws InstantiationException
	 */
	static public <S extends AbstractBaseEntityService<T, V, J>, T extends IEntity<J>, V extends IUser, J> S newServiceStatic(Class<S> serviceClass, Class<T> entityClass, V user,
			boolean startTransaction) {
		try {
			S service = serviceClass.getDeclaredConstructor().newInstance();
			service.setTypeOfE(entityClass);
			service.getEntityManager(startTransaction);

			if (user == null) {
				user = service.getSystemUser();
			}
			UserManager.getInstance().setUser(user);
			return service;
		} catch (Exception ex) {
			staticLogger.error("Cannot create new service", ex);
			return null;
		}
	}

	/**
	 *
	 * @param action
	 * @param resourceSecurityPath
	 * @param resourceName
	 * @param resourceId
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	final public void verifyUserPrivileges(String action, String resourceSecurityPath, String resourceName, Object resourceId) throws ForbiddenException {
		if (isUserAdmin()) {
			// ok
		} else {
			// verify if the user has the privilege on this resource
			if (!isUserAllowed(action, Arrays.asList(resourceSecurityPath.split("/")))) {
				throw new ForbiddenException("User [" + getUser().getUserName() + "] is not allowed to [" + action + "] the resourceSecurityPath [" + resourceSecurityPath + "]");
			}

			// if the action is authorized, verify if it can access THIS resource
			if (resourceId != null) {
				Integer integerResourceId = null;
				try {
					integerResourceId = Integer.parseInt("" + resourceId);
				} catch (Exception ex) {
					// ignore
				}
				if (resourceName != null && (integerResourceId == null || (integerResourceId != 0 && integerResourceId != -1))) {
					try {
						AbstractBaseEntityService service = newService(resourceName);
						if (service != null) {
							service.get(new Filter("id", resourceId));
						} else {
							throw new Exception("Cannot create service for resource[" + resourceName + "]");
						}
					} catch (Exception ex) {
						throw new ForbiddenException("User [" + getUser().getUserName() + "] is not allowed to access resourceName[" + resourceName + "] resourceId[" + resourceId + "]");
					}
				}
			}
		}
	}

	/**
	 *
	 * @param action
	 * @param resourceName
	 * @param resourceId
	 * @throws ForbiddenException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	final protected void verifyUserPrivileges(String action, String resourceName, Object resourceId) throws ForbiddenException {
		try {
			AbstractBaseEntityService service = newService(resourceName);
			if (service != null) {
				verifyUserPrivileges(action, service.getResourceSecurityPath(), resourceName, resourceId);
			} else {
				throw new Exception("Cannot create service for resource[" + resourceName + "]");
			}
		} catch (ForbiddenException ex) {
			throw ex;
		} catch (Exception ex) {
			String msg = "Cannot validate privileges for user [" + getUser().getUserName() + "] action[" + action + "] resourceName[" + resourceName + "] resourceId[" + resourceId + "]";
			logger.error(msg, ex);
			throw new ForbiddenException(msg + ": " + ex);
		}
	}

	public boolean isLogLongRequest() {
		return logLongRequest;
	}

	public void setLogLongRequest(boolean logLongRequest) {
		this.logLongRequest = logLongRequest;
	}
}
