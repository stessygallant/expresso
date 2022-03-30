package com.sgitmanagement.expressoext.base;

import java.io.IOException;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.ExternalEntity;
import com.sgitmanagement.expresso.util.ProgressSender;
import com.sgitmanagement.expressoext.security.User;

public class BaseExternalInterface<E extends BaseUpdatableEntity & ExternalEntity<Integer>, S extends BaseExternalEntityService<E>> implements ExternalInterface<E, S, User, Integer> {
	final static protected Logger logger = LoggerFactory.getLogger(BaseExternalInterface.class);

	private EntityManager entityManager;
	private Class<E> typeOfE;
	private S service;
	private boolean inSync = false;

	final protected <U extends BaseEntityService<T>, T extends BaseEntity> U newService(Class<U> serviceClass, Class<T> entityClass) {
		try {
			U service = serviceClass.getDeclaredConstructor().newInstance();
			service.setTypeOfE(entityClass);
			return service;
		} catch (Exception e) {
			logger.error("Problem creating the service [" + serviceClass + "]: " + e);
			return null;
		}
	}

	@Override
	final public void setService(S service) {
		this.service = service;
		this.entityManager = service.getEntityManager();
	}

	@Override
	final public boolean isSynchronizing() {
		return inSync;
	}

	final public void setSynchronizing(boolean sync) {
		this.inSync = sync;
	}

	/**
	 * Send the progression to the UI (avoid the browser to close the connection)
	 *
	 * @param progressSender
	 * @param count
	 * @param total
	 * @throws IOException
	 */
	protected void sendProgress(ProgressSender progressSender, int count, int total) throws IOException {
		String s = String.format("%s (%d/%d)", this.getClass().getSimpleName(), count, total);
		logger.debug("Sending progress: " + s);
		if (progressSender != null) {
			progressSender.send(s, count * 100.0 / total);
		}
	}

	/**
	 * Before merge, subclasses shall call this method
	 *
	 * @param e
	 */
	final protected void flushAndRefresh(Object e) {
		entityManager.flush();
		entityManager.refresh(e);
	}

	public Class<E> getTypeOfE() {
		return typeOfE;
	}

	@Override
	public void setTypeOfE(Class<E> typeOfE) {
		this.typeOfE = typeOfE;
	}

	public S getService() {
		return service;
	}

	public boolean isInSync() {
		return inSync;
	}

	public void setInSync(boolean inSync) {
		this.inSync = inSync;
	}

	@Override
	public void sync(String section, ProgressSender progressSender, int progressWeight) throws Exception {
		// by default, do nothing
	}

	@Override
	public void delete(E e, boolean async) throws Exception {
		// by default, do nothing
	}

	@Override
	public void merge(E e, boolean async) throws Exception {
		// by default, do nothing
	}

	final public EntityManager getEntityManager() {
		return entityManager;
	}

	final public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}
}
