package com.sgitmanagement.expresso.base;

import org.slf4j.Logger;

import com.sgitmanagement.expresso.exception.ForbiddenException;

import jakarta.persistence.EntityManager;

public abstract class AbstractBaseWebSocketResource<S extends AbstractBaseService<U>, U extends IUser> {
	protected Logger logger;
	private S service;
	private Class<S> serviceClass;

	protected AbstractBaseWebSocketResource(Class<S> serviceClass) {
		this.serviceClass = serviceClass;
	}

	@SuppressWarnings("unchecked")
	final protected U getUser() {
		return (U) UserManager.getInstance().getUser();
	}

	final public EntityManager getEntityManager() {
		return this.service.getEntityManager();
	}

	final public PersistenceManager getPersistenceManager() {
		return this.service.getPersistenceManager();
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
			S service = serviceClass.getDeclaredConstructor().newInstance();

			// put service in request to be closed
			service.registerService(service);

			return service;
		} catch (ForbiddenException e) {
			throw e;
		} catch (Exception ex) {
			logger.error("Problem creating the service [" + serviceClass + "]", ex);
			return null;
		}
	}
}
