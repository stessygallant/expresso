package com.sgitmanagement.expresso.base;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.exception.BaseException;
import com.sgitmanagement.expresso.exception.ValidationException;
import com.sgitmanagement.expresso.util.Util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Methods allowed on an entity<br>
 * Ex: /workOrder/235456<br>
 * GET - get an entity of the resource<br>
 * DELETE - delete an entity of the resource<br>
 * PUT - update the entire entity or only some fields<br>
 * POST - POST method on an entity is only for executing an action, never to create an entity<br>
 */
public abstract class AbstractBaseEntityResource<E extends IEntity<I>, S extends AbstractBaseEntityService<E, U, I>, U extends IUser, I> extends AbstractBaseResource<S, U> {
	private I id;
	private Class<E> typeOfE;

	public AbstractBaseEntityResource(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
		this.typeOfE = typeOfE;
	}

	protected void setId(I id) {
		this.id = id;
	}

	protected I getId() {
		return this.id;
	}

	final protected Class<E> getTypeOfE() {
		return typeOfE;
	}

	protected E get(I id) throws Exception {
		// this is not secure: it does not take into account the restrictions filter
		// E e = getService().get(this.id);

		Query query = new Query(new Filter("id", id));
		query.setKeySearch(true);

		// user super.getService because if getService is overwritten, you will get StackOverflowError
		List<E> list = super.getService().list(query);

		if (list.isEmpty()) {
			throw new BaseException(HttpServletResponse.SC_NOT_FOUND, "ID [" + id + "] not found for entity [" + getTypeOfE().getSimpleName() + "]");
		} else {
			return list.get(0);
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public E get() throws Exception {
		String action = Util.getParameterValue(getRequest(), "action");
		E e = get(this.id);

		// usually, the action will be [print|download] or anything
		// that does not modify the entity
		if (action != null) {
			try {
				// this is no body, then there is no formParams
				Method method = this.getClass().getMethod(action);
				method.invoke(this);
			} catch (NoSuchMethodException ex) {
				// but it may happen that the developer will use the signature with MultivaluedMap
				Method method = this.getClass().getMethod(action, MultivaluedMap.class);
				method.invoke(this, new MultivaluedHashMap<String, String>());
			}
		}
		return e;
	}

	@DELETE
	public void delete() throws Exception {
		try {
			getPersistenceManager().startTransaction(getEntityManager());

			// get the entity
			E e = get(this.id);

			// verify if the user can update this resource
			getService().verifyActionRestrictions("delete", e);

			getService().delete(this.id);
			getPersistenceManager().commit(getEntityManager());
		} catch (jakarta.persistence.PersistenceException | java.sql.SQLIntegrityConstraintViolationException ex) {
			getPersistenceManager().rollback(getEntityManager());
			throw new ValidationException("constraintViolationException");
		} catch (Exception ex) {
			getPersistenceManager().rollback(getEntityManager());
			throw ex;
		}
	}

	/**
	 * Update the entire object
	 *
	 * @param v
	 * @return
	 * @throws Exception
	 */
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public E update(E v) throws Exception {
		try {
			getPersistenceManager().startTransaction(getEntityManager());

			// get the entity
			E e = get(this.id);

			// verify if the user can update this resource
			getService().verifyActionRestrictions("update", e);

			return getService().update(v);
		} catch (Exception ex) {
			getPersistenceManager().rollback(getEntityManager());
			throw ex;
		} finally {
			getPersistenceManager().commit(getEntityManager());
		}
	}

	/**
	 * Update only one or more fields
	 *
	 * @return
	 * @throws Exception
	 */
	@PUT
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public E updateField(MultivaluedMap<String, String> formParams) throws Exception {
		try {
			getPersistenceManager().startTransaction(getEntityManager());

			// get the entity
			E e = get(this.id);

			// verify if the user can update this resource
			getService().verifyActionRestrictions("update", e);

			if (formParams != null) {
				// only get the first value (multiple values not supported for now)
				for (Map.Entry<String, List<String>> entry : formParams.entrySet()) {
					String fieldName = entry.getKey();
					String stringValue = entry.getValue().get(0);
					getService().updateField(e, fieldName, stringValue);
				}
			}

			return getService().update(e);
		} catch (Exception ex) {
			getPersistenceManager().rollback(getEntityManager());
			throw ex;
		} finally {
			getPersistenceManager().commit(getEntityManager());
		}
	}

	/**
	 * POST method on an object is only for executing an action, never to create an object
	 *
	 * @param formParams
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public E performAction(MultivaluedMap<String, String> formParams) throws Exception {
		String action = Util.getParameterValue(getRequest(), "action");

		logger.debug("Performing action [" + action + "] on single [" + this.getClass().getSimpleName() + "]");
		// with " + formParams);

		try {
			getPersistenceManager().startTransaction(getEntityManager());

			// verify if the user can get this resource
			E e = get(this.id);

			Method method;
			switch (action) {

			// action with no modification
			case "download":
			case "print":
			case "customprint":
				method = this.getClass().getMethod(action, MultivaluedMap.class);
				e = (E) method.invoke(this, formParams);
				break;

			default:
				// verify if the user can update this resource
				getService().verifyActionRestrictions(action, e);

				method = this.getClass().getMethod(action, MultivaluedMap.class);
				e = (E) method.invoke(this, formParams);
				break;
			}

			return e;
		} catch (NoSuchMethodException ex) {
			getPersistenceManager().rollback(getEntityManager());
			logger.error("No such method [" + action + "] exists");
			throw new BaseException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "No method defined for the action [" + action + "]");
		} catch (BaseException ex) {
			getPersistenceManager().rollback(getEntityManager());
			throw ex;
		} catch (Exception ex) {
			getPersistenceManager().rollback(getEntityManager());
			if (ex instanceof InvocationTargetException && ex.getCause() != null && ex.getCause() instanceof BaseException) {
				throw (BaseException) ex.getCause();
			} else {
				throw new BaseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot call the method for the action [" + action + "]", ex);
			}
		} finally {
			getPersistenceManager().commit(getEntityManager());
		}
	}

	/**
	 * Duplicate a resource and return the new resource
	 *
	 * @param formParams
	 * @return
	 * @throws Exception
	 */
	public E duplicate(MultivaluedMap<String, String> formParams) throws Exception {
		E e = get(this.id);
		return getService().duplicate(e);
	}

	/**
	 * Deactivate the resource
	 *
	 * @param formParams
	 * @return
	 * @throws Exception
	 */
	public E deactivate(MultivaluedMap<String, String> formParams) throws Exception {
		E e = get(this.id);
		return getService().deactivate(e);
	}

	@Override
	final protected S newService(Class<S> serviceClass) {
		S service = super.newService(serviceClass);
		service.setTypeOfE(typeOfE);
		return service;
	}
}
