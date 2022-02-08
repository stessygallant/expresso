package com.sgitmanagement.expresso.base;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.exception.BaseException;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expresso.util.Util;

public abstract class AbstractBaseResource<S extends AbstractBaseService<U>, U extends IUser> {
	protected Logger logger;
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	private S service;
	private Class<S> serviceClass;

	protected AbstractBaseResource(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
		this.logger = LoggerFactory.getLogger(this.getClass());
	}

	protected AbstractBaseResource(HttpServletRequest request, HttpServletResponse response, Class<S> serviceClass) {
		this(request, response);
		this.serviceClass = serviceClass;
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void performFormAction(MultivaluedMap<String, String> formParams) throws Exception {
		String action = Util.getParameterValue(getRequest(), "action");

		if (action == null) {
			action = "performPost";
		}
		logger.info("Performing action [" + action + "] on [" + this.getClass().getSimpleName() + "]");
		// with " + formParams);

		try {
			getService().getPersistenceManager().startTransaction(getEntityManager());
			Method method = this.getClass().getMethod(action, MultivaluedMap.class);
			method.invoke(this, formParams);
		} catch (NoSuchMethodException e) {
			getService().getPersistenceManager().rollback(getEntityManager());
			logger.error("No such method [" + action + "] exists");
			throw new BaseException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "No method defined for the action [" + action + "]");
		} catch (BaseException e) {
			getService().getPersistenceManager().rollback(getEntityManager());
			throw e;
		} catch (Exception e) {
			getService().getPersistenceManager().rollback(getEntityManager());
			if (e instanceof InvocationTargetException && e.getCause() != null && e.getCause() instanceof BaseException) {
				throw (BaseException) e.getCause();
			} else {
				// throw new BaseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				// "Cannot call the method for the action [" + action + "]", e);
				throw e;
			}
		} finally {
			getService().getPersistenceManager().commit(getEntityManager());
		}
	}

	@SuppressWarnings("unchecked")
	final protected U getUser() {
		return (U) request.getAttribute("user");
	}

	final public EntityManager getEntityManager() {
		return this.service.getEntityManager();
	}

	final public PersistenceManager getPersistenceManager() {
		return this.service.getPersistenceManager();
	}

	final protected HttpServletRequest getRequest() {
		return request;
	}

	final protected HttpServletResponse getResponse() {
		return response;
	}

	protected S getService() {
		if (this.service == null) {
			this.service = newService(serviceClass);
		}
		return service;
	}

	final public void setService(S service) {
		this.service = service;
	}

	protected S newService(Class<S> serviceClass) {
		try {
			S service = serviceClass.newInstance();
			service.setUser(getUser());
			service.setRequest(request);
			service.setResponse(response);

			return service;
		} catch (ForbiddenException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Problem creating the service [" + serviceClass + "]");
			return null;
		}
	}

	/**
	 * By default, call the process method from the getService()
	 *
	 * @param formParams
	 * @throws Exception
	 */
	public void process(MultivaluedMap<String, String> formParams) throws Exception {
		synchronized (this.getClass()) {
			this.getService().commit();
			String section = Util.getParameterValue(getRequest(), "section");
			if (section != null) {
				// backward compatible when section is on query string
				// CAUTION: this case does not support multiple parameters
				this.getService().process(section);
			} else {
				this.getService().process(formParams);
			}
			this.getService().commit();
		}

	}
}
