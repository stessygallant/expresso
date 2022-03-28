package com.sgitmanagement.expresso.base;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.EntityGraph;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.LockModeType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceException;
import javax.persistence.Subgraph;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Formula;
import org.hibernate.proxy.HibernateProxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sgitmanagement.expresso.base.AppClassField.Reference;
import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expresso.dto.Query.Sort;
import com.sgitmanagement.expresso.dto.Query.Sort.Direction;
import com.sgitmanagement.expresso.exception.AttributeNotFoundException;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expresso.exception.WrongVersionException;
import com.sgitmanagement.expresso.util.DateUtil;
import com.sgitmanagement.expresso.util.FieldRestrictionUtil;
import com.sgitmanagement.expresso.util.ProgressSender;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expresso.util.ZipUtil;

abstract public class AbstractBaseEntityService<E extends IEntity<I>, U extends IUser, I> extends AbstractBaseService<U> {
	final public static int MAX_SEARCH_RESULTS = 50;

	// this is only for optimization (do not use distinct is not needed)
	private boolean distinctNeeded = false;

	private boolean refreshAfterMerge = true;

	private Set<String> activeOnlyFields = null;

	private boolean parentEntityLookup = false;
	private Field parentEntityField = null;

	// for performance optimization
	private Boolean parentUpdatable = null;
	private static final Map<Query, Future<?>> creationQueryFutureMap = Collections.synchronizedMap(new HashMap<>());

	private Class<E> typeOfE;

	protected AbstractBaseEntityService() {

	}

	protected AbstractBaseEntityService(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
		this.typeOfE = typeOfE;
	}

	final public Class<E> getTypeOfE() {
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
			if (refreshAfterMerge) {
				flushAndRefresh(e);
			}
			onPostCreate(e);
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
		return getEntityManager().getReference(getTypeOfE(), id);
	}

	/**
	 * This method cannot be called by an external class because if does not validate the security on the resources
	 *
	 * @param id
	 * @return
	 */
	public E get(I id) {
		if (id != null) {
			return getEntityManager().find(getTypeOfE(), id);
		} else {
			return null;
		}
	}

	final public E get(I id, boolean forUpdate) {
		return getEntityManager().find(getTypeOfE(), id, LockModeType.PESSIMISTIC_WRITE);
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
	public void onPostCreate(E e) throws Exception {
		onPostMerge(e);
	}

	/**
	 * This method is used to validate constraint upon create/update methods
	 *
	 * @param e
	 * @return
	 * @throws Exception
	 */
	public void onPostUpdate(E e) throws Exception {
		onPostMerge(e);
	}

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
	 * Use when calling updateField from the UI
	 * 
	 * @param e
	 * @param fieldName
	 * @param stringValue
	 */
	public void updateField(E e, String fieldName, String stringValue) throws Exception {
		Field field = Util.getField(e, fieldName);
		if (field != null) {
			// need to convert the type
			String fieldTypeClassName = field.getType().getName();
			Object value = Util.convertValue(stringValue, fieldTypeClassName);
			field.set(e, value);
		}
		flushAndRefresh(e);
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

		if (getEntityManager().contains(e)) {
			// ok, already in the session
			if (Updatable.class.isAssignableFrom(getTypeOfE())) {
				((Updatable) e).setLastModifiedDate(new Date());
				((Updatable) e).setLastModifiedUserId(getUser().getId());
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

				if (!NotVersionable.class.isAssignableFrom(getTypeOfE())) {
					if (!Util.equals(updatable.getLastModifiedDate(), updatableNew.getLastModifiedDate())) {
						logger.warn("WARNING: WrongVersionException: " + getTypeOfE().getSimpleName() + ":" + e.getId() + ":" + updatable.getLastModifiedDate() + ":"
								+ updatableNew.getLastModifiedDate() + ":" + updatable.getLastModifiedUserId());
						throw new WrongVersionException();
					}
				}

				// update last modified date
				updatableNew.setLastModifiedDate(new Date());
				updatableNew.setLastModifiedUserId(getUser().getId());
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

			setProperties(getTypeOfE(), p, e);
			e = p;
		}

		if (refreshAfterMerge) {
			flushAndRefresh(e);
		}

		onPostUpdate(e);
		return e;
	}

	/**
	 *
	 * @param typeOf
	 * @param dest
	 * @param source
	 * @throws Exception
	 */
	final private boolean setProperties(Class<?> typeOf, E dest, E source) throws Exception {
		// from the base, set the properties on the entity
		boolean updated = false;

		Map<String, String> restrictedFields = FieldRestrictionUtil.INSTANCE.getFieldRestrictionMap(getResourceName());

		Class<?> c = typeOf;
		do {
			Field[] allFields = c.getDeclaredFields();
			for (Field field : allFields) {
				field.setAccessible(true);

				// if the entity has FieldRestriction, make sure that the user has to role
				if (restrictedFields != null) {
					String restrictedRole = restrictedFields.get(field.getName());
					if (restrictedRole != null && !isUserInRole(restrictedRole)) {
						// do not assign them. Keep the current value
						logger.debug("Skipping assigning [" + field.getName() + "] keeping oldValue[" + field.get(dest) + "]");
						continue;
					}
				}

				Object oldValue = field.get(dest);
				if (oldValue != null && (IEntity.class.isInstance(oldValue) || EntityDerived.class.isInstance(oldValue))) {
					// do not assign them
				} else if (oldValue != null && Set.class.isInstance(oldValue) && field.getAnnotation(CollectionTable.class) != null) {
					// logger.debug("Got a ManyToMany [" + field.getName() + "] for the Entity [" +
					// typeOf.getSimpleName()
					// + "]");

					// this is a collection of ID (Integer).
					@SuppressWarnings("unchecked")
					Set<Integer> newIdSet = (Set<Integer>) field.get(source);
					@SuppressWarnings("unchecked")
					Set<Integer> previousIdSet = (Set<Integer>) oldValue;

					// add new ids
					for (Integer id : newIdSet) {
						if (!previousIdSet.contains(id)) {
							updated = true;
							previousIdSet.add(id);
							// logger.debug("Adding " + id);
						}
					}

					// remove deleted ids
					for (Integer id : new ArrayList<>(previousIdSet)) {
						if (!newIdSet.contains(id)) {
							updated = true;
							previousIdSet.remove(id);
							// logger.debug("Removing " + id);
						}
					}
				} else if (oldValue != null && Collection.class.isInstance(oldValue)) {
					// do not assign them (OneToMany)
					Object newValue = field.get(source);
					if (newValue != null) {
						// ignore
						// logger.debug("Got a Collection [" + field.getName() + "] for the Entity ["
						// + typeOf.getSimpleName() + "]");
					}
				} else {
					Object newValue = field.get(source);

					if (!Util.equals(newValue, oldValue)) {
						// System.out.println("Setting [" + field.getName() + "]: [" + oldValue + " -> "
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
		getEntityManager().flush();
		getEntityManager().refresh(e, LockModeType.PESSIMISTIC_WRITE);
		return e;
	}

	/**
	 * Try to lock the entity but do not wait
	 *
	 * @param e
	 */
	final public E lockNoWait(E e) {
		Map<String, Object> properties = new HashMap<>();
		properties.put("javax.persistence.lock.timeout", 0);
		getEntityManager().flush();
		getEntityManager().refresh(e, LockModeType.PESSIMISTIC_WRITE, properties);
		return e;
	}

	/**
	 * Search the entity using a predefined query (usually called by ComboBox, Multiselect, etc)
	 *
	 * @param searchString
	 * @return
	 */
	public List<E> search(Query query, String searchString) throws Exception {
		Filter filter;
		if (searchString == null) {
			filter = new Filter();
		} else {
			filter = getSearchFilter(searchString);
		}

		if (filter != null) {
			query.addFilter(filter);
			// logger.debug("Search query: " + new Gson().toJson(query));
			return list(query);
		} else {
			logger.error("getSearchFilter method not implemented for the resource [" + getTypeOfE().getSimpleName() + "]");
			return new ArrayList<>();
		}
	}

	/**
	 * By default, use the same search as the Combo Box
	 *
	 * @param query
	 * @param searchString
	 * @return
	 * @throws Exception
	 */
	public List<E> searchOverall(Query query, String searchString) throws Exception {
		Filter filter;
		if (searchString == null) {
			filter = new Filter();
		} else {
			// if multiple words, each word must be present
			String[] words = searchString.trim().split(" ");
			if (words.length == 1) {
				filter = getSearchOverallFilter(searchString);
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
			logger.error("getSearchFilter method not implemented for the resource [" + getTypeOfE().getSimpleName() + "]");
			return new ArrayList<>();
		}
	}

	public void delete(I id) throws Exception {
		E e = get(id);

		if (e != null) {
			getEntityManager().remove(e);
			getEntityManager().flush();
		}
	}

	final public boolean exists(I id) {
		return (get(id) != null);
	}

	final public E get(Filter filter) throws Exception {
		return get(new Query(filter));
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

			// use the CriteriaBuilder to create the query
			CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
			CriteriaQuery<E> q = cb.createQuery(getTypeOfE());
			Root<E> root = q.from(getTypeOfE());
			q.select(root);

			if (!query.hasFilters() && !query.hasSort() && query.getPageSize() == null) {
				// use a simple query
				// by default, sort by the id
				q.orderBy(cb.desc(root.get(getKeyFields()[0])));
				TypedQuery<E> typedQuery = getEntityManager().createQuery(q);
				return typedQuery.getResultList();
			} else {
				// create the query using filters, paging and sort

				// this must be called first. It will set distinctNeeded
				Map<String, Join<?, ?>> joinMap = new HashMap<>();
				Predicate predicate = buildPredicates(cb, root, query.getFilter(), joinMap);

				// sorts may also trigger a distinct select
				List<Order> orders = new ArrayList<>();
				if (query.getSort() != null && query.getSort().size() > 0 && !query.countOnly()) {
					for (Sort sort : query.getSort()) {
						if (sort.getDir() != null && sort.getDir().equals(Direction.asc)) {
							orders.add(cb.asc(retrieveProperty(root, sort.getField(), joinMap)));
						} else {
							orders.add(cb.desc(retrieveProperty(root, sort.getField(), joinMap)));
						}
					}
				}

				// always sort by the id at the end to make sure the result set is stable
				for (Sort sort : getUniqueQuerySort()) {
					if (sort.getDir() != null && sort.getDir().equals(Direction.asc)) {
						orders.add(cb.asc(root.get(sort.getField())));
					} else {
						orders.add(cb.desc(root.get(sort.getField())));
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

				TypedQuery<E> typedQuery = getEntityManager().createQuery(q);

				if (query.getSkip() != null) {
					typedQuery.setFirstResult(query.getSkip());
				}

				if (query.getPageSize() != null) {
					typedQuery.setMaxResults(query.getPageSize());
				}

				// This is mandatory to eagerly fetch the data using CriteriaBuilder
				EntityGraph<E> fetchGraph = getEntityManager().createEntityGraph(getTypeOfE());
				typedQuery.setHint("javax.persistence.loadgraph", buildEntityGraph(fetchGraph, getTypeOfE(), query, null));

				// then issue the SQL query
				Date startDate = new Date();
				List<E> data = typedQuery.getResultList();
				Date endDate = new Date();

				if (data.size() > 5000) {
					logger.warn(
							"Got a request that returns " + data.size() + " resources [" + getTypeOfE().getSimpleName() + " from: " + getUser().getFullName() + ". Query: " + new Gson().toJson(query));
				}

				long delay = (endDate.getTime() - startDate.getTime());
				if (delay > 500) {
					logger.warn("Execution " + getTypeOfE().getSimpleName() + " SQL time: " + delay + " ms (" + data.size() + ") " + getTypeOfE().getSimpleName() + ": " + new Gson().toJson(query));
				}

				// if the query required the entity to be created if is does not exist, create it
				if (data.size() == 0 && query.isCreateIfNotFound()) {
					E e = createEntityFromUniqueConstraints(query);
					if (e != null) {
						data.add(e);
					}
				}

				return data;
			}
		} catch (Exception ex) {
			logger.error("Error executing query: " + ex + " - " + new Gson().toJson(query));
			throw ex;
		}
	}

	/**
	 * 
	 * @param filter
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private E createEntityFromUniqueConstraints(Query query) throws Exception {
		UniqueFieldConstraints constraintsAnnotation = getTypeOfE().getAnnotation(UniqueFieldConstraints.class);
		if (constraintsAnnotation != null) {

			// we need to make sure that we do not try to add the same entity twice
			// because when a persist failed, the session is rollback only,
			// which means that all operations will be lost

			// the first will create the entity and the other threads will get it
			Future<E> future = null;
			Callable<E> callable = null;
			Object lock = this.getClass();
			synchronized (lock) {
				future = (Future<E>) creationQueryFutureMap.get(query);
				if (future == null) {
					callable = new Callable<E>() {

						@Override
						public E call() throws Exception {
							E e = null;

							// first try to get it
							try {
								e = get(query.setCreateIfNotFound(false));
							} catch (NoResultException ex) {
								// still not found, create it
								e = getTypeOfE().newInstance();

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

										// System.out.println("Setting " + fieldName + " to [" + value + "]");
										field.set(e, Util.convertValue(value, field.getType().getSimpleName()));
									}
								}

								// start transaction (if needed)
								PersistenceManager.getInstance().startTransaction(getEntityManager());

								e = create(e);
								// logger.debug("created");
							}
							return e;
						}
					};
					future = new FutureTask<E>(callable);
					creationQueryFutureMap.put(query, future);
				}
			}

			if (callable != null) {
				// actually create the entity
				((FutureTask<E>) future).run();

				// then clean from the map in a few seconds
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(10 * 1000);
						} catch (InterruptedException e) {
							// ignore
						}
						creationQueryFutureMap.remove(query);
					}
				}).start();
			}

			// wait for the creation of it (this is done outside the lock for this class)
			E e = future.get();

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
		// System.out.println("**** activeOnly" + query.activeOnly() + "
		// isAssignableFrom:"
		// + Deactivable.class.isAssignableFrom(getTypeOfE()) + " getTypeOfE():" +
		// getTypeOfE().getName());

		// if the query is already verified, skip it
		if (!query.isVerified()) {
			query.setVerified(true);

			if (!query.isKeySearch()) {

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
						}

						if (includeActiveOnlyFilter) {
							query.addFilter(activeOnlyFilter);
						}
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
					query.addFilter(restrictionsFilter);
				}

			} else {
				// restrict the list to the permitted entities
				// Note: do not add restrictions filter if we only need to verify if the key is
				// unique
				if (!query.countOnly()) {
					Filter restrictionsFilter = getRestrictionsFilter();
					if (restrictionsFilter != null) {
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
		}

		return query;
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
			if (query == null || query.countOnly()) {
				query = verifyQuery(query);
			}

			// use the CriteriaBuilder to create the query
			CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
			CriteriaQuery<Long> q = cb.createQuery(Long.class);
			Root<E> root = q.from(getTypeOfE());

			if (!query.hasFilters() && !query.hasSort() && query.getPageSize() == null) {
				// use a simple query
				q.select(cb.count(root));
			} else {
				// create the query using filters

				// this must be called first. It will set distinctNeeded
				Predicate predicate = buildPredicates(cb, root, query.getFilter(), new HashMap<String, Join<?, ?>>());
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
			return getEntityManager().createQuery(q).getSingleResult();
		} catch (Exception ex) {
			logger.error("Error executing query: " + ex + " - " + new Gson().toJson(query));
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
			// do not proccess the same filter twice
			if (processedFilters == null) {
				// do not use HashSet because the hashing is done at insertion
				// and we change de field later
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
	@SuppressWarnings("unchecked")
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
				getParentEntityService().verifyActionRestrictions("update", getParentEntityInstance(e));
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
		if (parentEntity != null) {
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
	 * Get the field annotated with the @ParentEntity annotation
	 *
	 * @return
	 */
	public Field getParentEntityField() {
		if (!this.parentEntityLookup) {
			this.parentEntityLookup = true;
			for (Field field : getTypeOfE().getDeclaredFields()) {
				if (field.isAnnotationPresent(ParentEntity.class)) {
					field.setAccessible(true);
					this.parentEntityField = field;
					break;
				}
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
			// String parentEntityClassName = parentEntityClass.getCanonicalName();
			// if (parentEntityClassName.contains(".Basic")) {
			// parentEntityClassName = parentEntityClassName.replaceAll("\\.Basic", ".");
			// parentEntityClass = Class.forName(parentEntityClassName);
			// }

			Class parentEntityServiceClass = Class.forName(parentEntityClass.getCanonicalName() + "Service");
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
	private Object buildEntityGraph(Object fetchGraph, Class<?> clazz, Query query, String filterPath) {
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
					// we still need
					// to include it in the Entity Graph
					if (field.isAnnotationPresent(ManyToOne.class)) {
						List<Filter> filters = query.getFilters(filterName + ".", true);
						if (filters != null && !filters.isEmpty()) {
							eagerRelation = true;
						}
					}
					// for an unknown reason it works...
					// if you have problem with filter and many to many, here is the problem
					// else if (field.isAnnotationPresent(ManyToMany.class)) {
					// List<Filter> filters = query.getFilters(filterName + ".", true);
					// if (filters != null && !filters.isEmpty()) {
					// logger.warn("Filter on ManyToMany [" + filters.get(0).getField() + "] may not
					// work");
					// }
					// }
				}

				if (eagerRelation) {
					Subgraph subGraph = null;
					if (fetchGraph instanceof EntityGraph) {
						subGraph = ((EntityGraph) fetchGraph).addSubgraph(field.getName());
					} else {
						subGraph = ((Subgraph) fetchGraph).addSubgraph(field.getName());
					}

					// now do the same for the Object (cascading)
					// we cannot go to the same class (circular reference)
					if (subGraph != null && !field.getType().equals(clazz)) {
						buildEntityGraph(subGraph, field.getType(), query, filterName);
					}
				}
			}
		} while ((clazz = clazz.getSuperclass()) != null);
		return fetchGraph;
	}

	/**
	 * Give a change to subclass to define default sorts. Default is to sort on the first keyField
	 */
	protected Query.Sort[] getDefaultQuerySort() {
		return new Query.Sort[] { new Query.Sort(getKeyFields()[0], Query.Sort.Direction.desc) };
	}

	/**
	 * Give a change to subclass to define the unique sorts. This is mandatory because when sorting by a non unique value, the result set is not stable. Expresso will always add this unique sort at
	 * the end of the order by clause
	 */
	protected Query.Sort[] getUniqueQuerySort() {
		return new Query.Sort[] { new Query.Sort(getKeyFields()[0], Query.Sort.Direction.desc) };
	}

	/**
	 * Give a change to subclass to define the "key" fields
	 */
	protected String[] getKeyFields() {
		List<String> keyFields = new ArrayList<>();
		Class<?> c = getTypeOfE();
		do {
			for (Field field : c.getDeclaredFields()) {
				if (field.isAnnotationPresent(KeyField.class)) {
					keyFields.add(field.getName());
				}
			}
		} while ((c = c.getSuperclass()) != null);
		keyFields.add("id");

		// if not defined, return id
		return keyFields.toArray(new String[0]);
	}

	/**
	 * If the keyField needs a padding, do it before the search
	 *
	 * @param keyField
	 * @param keyValue
	 * @return
	 * @throws Exception
	 */
	public String formatKeyField(String keyField, Object keyValue) {
		// always trim
		String key = ("" + keyValue).trim();

		try {
			Field field = Util.getField(getTypeOfE(), keyField);
			if (field.isAnnotationPresent(KeyField.class)) {
				KeyField keyFieldAnnotation = field.getAnnotation(KeyField.class);

				// format if needed
				if (keyFieldAnnotation.format().length() > 0) {
					key = Util.formatKey(keyFieldAnnotation.format(), key);
				}

				// add padding
				if (keyFieldAnnotation.padding() > 0) {
					// key = String.format("%1$" + keyFieldAnnotation.padding() + "s",
					// key).replace(' ', '0');
					key = StringUtils.leftPad(key, keyFieldAnnotation.padding(), '0');
				}

				// add prefix
				if (keyFieldAnnotation.prefix().length() > 0) {
					if (!key.startsWith(keyFieldAnnotation.prefix())) {

						// add the prefix only if the key is complete
						if (keyFieldAnnotation.length() != 0 && (key.length() + keyFieldAnnotation.prefix().length()) != keyFieldAnnotation.length()) {
							// this means the key is not complete, do not add the prefix
						} else {
							key = keyFieldAnnotation.prefix() + key;
						}
					}
				}

				// verify the key total length
				if (keyFieldAnnotation.length() != 0) {
					if (key.length() != keyFieldAnnotation.length()) {
						// this means the key is not complete, we cannot search by EQUALS
						// should we search by CONTAINS?
					}
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return key;
	}

	/**
	 * Give a change to subclass to define the application responsible for this entity. Use getResourceManager().
	 */
	@Deprecated
	protected String getApplicationName() throws Exception {
		String applicationName = "";
		if (getParentEntityField() != null) {
			applicationName = getParentEntityService().getApplicationName() + ".";
		}
		return applicationName + getTypeOfE().getSimpleName() + "Manager";
	}

	public String getResourceName() {
		return StringUtils.uncapitalize(getTypeOfE().getSimpleName());
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
	 * Get the resource paths for this entity. IMPORTANT: The default implementation works well for the master resource only. For sub resources, you MAY need to override this
	 *
	 * @return
	 */
	public String getResourcePath() throws Exception {
		String resourcePath = getResourceName();

		if (getParentEntityField() != null) {
			// usually, sub resource starts with the name of the parent resource
			// Ex: project, projectLot and projectLotItem
			// the resource path is: project/lot/item
			String parentResourceName = StringUtils.uncapitalize(getParentEntityService().getTypeOfE().getSimpleName());

			if (resourcePath.startsWith(parentResourceName)) {
				String parentResourcePath = getParentEntityService().getResourcePath();
				resourcePath = parentResourcePath + "/" + StringUtils.uncapitalize(resourcePath.substring(parentResourceName.length()));
			}
		}
		return resourcePath;
	}

	/**
	 * This method uses the following methods: <br>
	 * protected String[] getKeyFields() (the first key returned is used by default)<br>
	 * protected String getApplicationName() <br>
	 *
	 * @param keyValue
	 * @return
	 */
	public String getEntityURL(Object keyValue) throws Exception {
		return SystemEnv.INSTANCE.getDefaultProperties().getProperty("base_url") + "#" + getResourceManager() + "(" + getKeyFields()[0] + "-" + keyValue + ")";
	}

	/**
	 * By default there is no active only filter.
	 *
	 * @return
	 * @throws Exception
	 */
	protected Filter getActiveOnlyFilter() throws Exception {
		// by default, there is not active only filter
		return null; // getParentActiveOnlyFilter();
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
		filter.addFilter(new Filter("deactivationDate", null));
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
		return null;
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
	final private <T> Path<T> retrieveProperty(From<?, ?> join, String field, Map<String, Join<?, ?>> joinMap) {
		Path<T> p = null;
		if (field != null) {
			try {
				if (field.indexOf('.') != -1) {

					String[] paths = field.split("\\.");
					String joinString = null;
					for (int i = 0; i < (paths.length - 1); i++) {
						// s is a relation, not an attribute
						String s = paths[i];

						// verify if it is a collection
						p = join.get(s);
						if (Collection.class.isAssignableFrom(p.getJavaType())) {
							distinctNeeded = true;
						}

						// verify if the join already exists
						// reuse it if it exists
						joinString = (joinString == null ? s : "." + s);
						if (joinMap.containsKey(joinString)) {
							join = joinMap.get(joinString);
						} else {
							// build the left join
							join = join.join(s, JoinType.LEFT);
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

		boolean caseSensitive = (boolean) getEntityManager().getProperties().get("expresso.case_sensitive");
		String truncFunction = (String) getEntityManager().getProperties().get("expresso.trunc_date_function");
		boolean emptyStringIsNull = (boolean) getEntityManager().getProperties().get("expresso.empty_string_is_null");

		// handle all is [not] null cases here
		if (filter.getValue() == null ||
		// special case for filtering in the Grid on no value
				(("" + filter.getValue()).equals("-1.0") && filter.getField().endsWith("Id"))
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
			case equalsnokey:
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

			}

			switch (type) {
			case "Integer":
				Integer integerValue = null;
				List<Integer> integerValues = null;
				if (valueType.equals("Integer")) {
					integerValue = (Integer) filter.getValue();
				} else if (valueType.equals("Double")) {
					integerValue = ((Double) filter.getValue()).intValue();
				} else if (valueType.equals("ArrayList")) {
					integerValues = (List<Integer>) filter.getValue();
				} else {
					String v = (String) filter.getValue();
					if (v.indexOf('.') == -1) {
						integerValue = Integer.parseInt(v);
					} else {
						integerValue = (int) Float.parseFloat(v);
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
				case equalsNoKey:
				case equalsnokey:
					predicate = cb.equal(path, integerValue);
					break;

				case in:
					distinctNeeded = true;
					if (integerValues != null && !integerValues.isEmpty()) {
						predicate = path.in(integerValues);
					} else {
						predicate = null;
					}
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
				} else {

					String dateString = (String) filter.getValue();

					// verify if dateString is a key
					Calendar calendar = DateUtils.truncate(Calendar.getInstance(), Calendar.DATE);
					switch (dateString) {
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

					default:
						dateValue = DateUtil.parseDate(filter.getValue());
						break;
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
					if (dateValue != null) {
						// THIS IS THE DEFAULT FROM THE KENDO UI GRID.
						// if the database is a datetime and not a date, it will not work

						if (dateValue.getTime() != DateUtils.truncate(dateValue, Calendar.DATE).getTime()) {
							logger.warn("Comparing field[" + getTypeOfE().getSimpleName() + "." + filter.getField() + "] date only: " + dateValue + " (Use timestampEquals or sameDayEquals instead)");
						}

						// we only compare date (not datetime)
						dateValue = DateUtils.truncate(dateValue, Calendar.DATE);
						predicate = cb.equal(cb.function(truncFunction, Date.class, path), dateValue);
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

			case "String":
				String stringValue = null;
				List<String> stringValues = null;
				Expression stringPath;

				if (filter.getOperator().equals(Operator.eq) || filter.getOperator().equals(Operator.neq) || !caseSensitive) {
					stringValue = (String) filter.getValue();
					stringPath = path;
				} else if (valueType.equals("ArrayList")) {
					stringValues = (List<String>) filter.getValue();
					stringPath = path;
				} else {
					// compare lowercase only
					stringValue = ((String) filter.getValue()).toLowerCase();
					stringPath = cb.lower(path);
				}

				switch (op) {
				case eq:
				case equalsNoKey:
				case equalsnokey:
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
				case containsNoKey:
				case containsnokey:
					predicate = cb.like(stringPath, "%" + stringValue.trim() + "%");
					break;

				case trimCompare:
					predicate = cb.equal(cb.trim(stringPath), stringValue.trim());
					break;

				case neqTrimCompare:
					predicate = cb.notEqual(cb.trim(stringPath), stringValue.trim());
					break;

				case in:
					distinctNeeded = true;
					if (stringValues != null && !stringValues.isEmpty()) {
						predicate = path.in(stringValues);
					} else {
						predicate = null;
					}
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
				// this is for many to many
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
				if (!path.getJavaType().isEnum()) {
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

	/**
	 *
	 * @param cb
	 * @param from
	 * @param filter
	 * @return
	 * @throws Exception
	 */
	final private Predicate buildPredicates(CriteriaBuilder cb, From<?, ?> from, Filter filter, Map<String, Join<?, ?>> joinMap) throws Exception {
		Predicate predicate = null;

		if (filter != null) {

			try {
				if (filter.getFilters() != null) {
					List<Predicate> restrictions = new ArrayList<>();

					if (filter.getField() != null) {
						// this is a special case when we want to apply filters to the same join
						from = from.join(filter.getField(), JoinType.INNER);
						this.distinctNeeded = true;
					}

					// build predicates for each sub filters
					for (Filter p : filter.getFilters()) {
						Predicate subPredicate = buildPredicates(cb, from, p, joinMap);
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
					predicate = getPredicate(cb, retrieveProperty(from, filter.getField(), joinMap), filter);
				}
			} catch (AttributeNotFoundException ex) {
				if (from.getJavaType() == null || filter.getField() == null
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

	public String getLink(E e) throws Exception {
		String keyField = getKeyFields()[0];

		Object keyValue = BeanUtils.getProperty(e, keyField);
		return getEntityURL(keyValue);
	}

	/**
	 * Generate all base files for a resource manager
	 *
	 * @throws Exception
	 */
	public void generateResourceManager(String namespace) throws Exception {
		Field parentEntityField = getParentEntityField();
		String resourceManagerName = getApplicationName();
		String resourcePath = StringUtils.uncapitalize(getTypeOfE().getSimpleName());
		if (parentEntityField != null && resourcePath.startsWith(parentEntityField.getName())) {
			resourcePath = StringUtils.uncapitalize(resourcePath.substring(parentEntityField.getName().length()));
		}

		Map<String, AppClassField> appClassFieldMap = getAppClassFieldMap();

		// remove type
		appClassFieldMap.remove("type");

		StringBuilder sb = new StringBuilder();

		// create a temp directory
		java.nio.file.Path resourceManagerPath = Files.createTempDirectory(resourceManagerName);

		// app_class.js
		sb = new StringBuilder();
		if (namespace != null && namespace.length() > 0) {
			String ns = "";

			// start from the top
			String[] namespaces = namespace.split("\\.");
			for (int i = 0; i < namespaces.length; i++) {
				if (i == 0) {
					ns = namespaces[i];
					sb.append("var ");
				} else {
					ns = ns + "." + namespaces[i];
				}
				sb.append(ns + " = " + ns + " || {};\n");
			}
			sb.append("\n");
		}

		// for sub manager, it does not work
		// ValueManager
		// NOT KpiManager.KpiValueManager
		String managerName = resourceManagerName;
		if (getParentEntityField() != null) {
			String parentApplicationName = getParentEntityService().getApplicationName();

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
		sb.append("   // @override\n");
		sb.append("    init: function (applicationPath) {\n");
		sb.append("        var fields = " + getAppClassFields(false) + ";\n");
		sb.append("\n");
		sb.append("       expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, \"" + resourcePath + "\", fields, {\n");
		sb.append("            form: true,\n");
		sb.append("            grid: true,\n");
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
			sb.append("         columns.push.apply(columns, [{\n");
		} else {
			sb.append("        var columns = [{\n");
		}

		// columns
		for (String fieldName : appClassFieldMap.keySet()) {
			String field = fieldName;
			AppClassField appClassField = appClassFieldMap.get(fieldName);
			if (appClassField.getReference() != null) {
				// remove the ID
				field = field.substring(0, field.length() - 2);
				if (appClassField.getReference() instanceof String && ((String) appClassField.getReference()).equals("user")) {
					field += ".fullName";
				} else if (appClassField.getReference() instanceof Reference) {
					// Reference reference = (Reference) appClassField.getReference();
					field += ".description";
				}
			}
			sb.append("            field: \"" + field + "\",\n");
			sb.append("            width: 120\n");
			sb.append("        }, {\n");
		}
		if (getParentEntityField() != null) {
			sb.append("        }]);\n");
		} else {
			sb.append("        }];\n");
		}

		sb.append("        return columns;\n");
		sb.append("    }\n");
		sb.append("});\n");

		File gridJsFile = new File(resourceManagerPath.toFile().getAbsolutePath() + File.separator + "grid.js");
		FileUtils.write(gridJsFile, sb.toString(), StandardCharsets.UTF_8);

		// labels.js
		sb = new StringBuilder();
		sb.append(namespace + ".Labels = {\n");
		sb.append("    " + StringUtils.uncapitalize(getTypeOfE().getSimpleName()) + ": " + "\"" + getTypeOfE().getSimpleName() + "\",\n");

		// fields
		for (String fieldName : appClassFieldMap.keySet()) {
			if (fieldName.equals("deactivationDate")) {
				// skip
			} else {
				String field = fieldName;
				if (field.endsWith("Id")) {
					field = field.substring(0, field.length() - 2);
				}
				sb.append("    " + field + ": \"" + field + "\",\n");
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
		Map<String, AppClassField> appClassFieldMap = getAppClassFieldMap();
		String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(appClassFieldMap);
		if (!jsonCompliance) {
			// remove the " for key (it is not JSON compliant, but it is better for the
			// app_class
			jsonString = jsonString.replaceAll("\"([a-zA-Z0-9]+)\":", "$1:").replaceAll("\"NULL\"", "null");
		}
		return jsonString;
	}

	/**
	 * Get the list of fields to be included in the app_class.js
	 *
	 * @return
	 * @throws Exception
	 */
	private Map<String, AppClassField> getAppClassFieldMap() throws Exception {
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
		Connection connection = getConnection();
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
					logger.debug("Skipping [" + fieldName + "]. No getter method");
					continue;
				}

				boolean exported = true;
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
					// System.out.println(column.name().toLowerCase() + ":" + columnMetaData);
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
							String refString = field.getGenericType().toString();
							refString = refString.substring(refString.lastIndexOf('.') + 1);
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
						fieldName += "Id";
						appClassField.setType("number");

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
						// remove the "s" and add "Ids"
						fieldName = fieldName.substring(0, fieldName.length() - 1) + "Ids";
					} else {
						// logger.warn(String.format("Type [%s] not supported",
						// type.getCanonicalName()));
						exported = false;
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

				// verify if the field is protected by a restricted role
				appClassField.setRestrictedRole(FieldRestrictionUtil.INSTANCE.getFieldRestrictionRole(getResourceName(), fieldName));

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

				if (fieldName.endsWith("Key")) {
					appClassField.setUnique(true);
				}

				if (/* fieldName.endsWith("No") && */ Arrays.asList(getKeyFields()).contains(fieldName) && !fieldName.endsWith("id")) {
					appClassField.setKeyField(true);
					appClassField.setEditable(false);
				}

				if (fieldName.endsWith("sortOrder")) {
					appClassField.setDefaultValue(1);
				}

				if (exported) {
					appClassFieldMap.put(fieldName, appClassField);
				}
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
		default:
			type = null;
			break;
		}
		return type;
	}

	final public void sync() throws Exception {
		sync(null, null);
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
	@SuppressWarnings("unchecked")
	final public <S extends AbstractBaseEntityService<T, U, I>, T extends IEntity<I>> S newService(String resourceName) {

		try {
			Class<T> entityClass = (Class<T>) findEntityClassByName(StringUtils.capitalize(resourceName));
			Class<S> serviceClass = (Class<S>) Class.forName(entityClass.getCanonicalName() + "Service");

			S service = serviceClass.newInstance();

			service.setUser(getUser());
			service.setTypeOfE(entityClass);
			service.setRequest(getRequest());
			service.setResponse(getResponse());

			return service;
		} catch (Exception e) {
			logger.error("Problem creating the service for resource [" + resourceName + "]", e);
			return null;
		}
	}

	/**
	 * Search a class inside the base package
	 *
	 * @param name
	 * @param entityBasePackage
	 * @return
	 */
	private Class<?> findEntityClassByName(String name) {
		String entityBasePackage = SystemEnv.INSTANCE.getDefaultProperties().getProperty("entity_base_package");
		for (Package p : Package.getPackages()) {
			if (p.getName().startsWith(entityBasePackage)) {
				try {
					return Class.forName(p.getName() + "." + name);
				} catch (ClassNotFoundException e) {
					// not in this package, try another
				}
			}
		}
		return null;
	}

	static public <S extends AbstractBaseEntityService<T, V, J>, T extends IEntity<J>, V extends IUser, J> S newServiceStatic(Class<S> serviceClass, Class<T> entityClass) {
		return newServiceStatic(serviceClass, entityClass, null);
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
	static public <S extends AbstractBaseEntityService<T, V, J>, T extends IEntity<J>, V extends IUser, J> S newServiceStatic(Class<S> serviceClass, Class<T> entityClass, V user) {
		try {
			S service = serviceClass.newInstance();
			service.setTypeOfE(entityClass);

			if (user == null) {
				user = service.getSystemUser();
			}
			service.setUser(user);
			return service;
		} catch (Exception ex) {
			ex.printStackTrace(); // cannot user logger.error
			return null;
		}
	}
}