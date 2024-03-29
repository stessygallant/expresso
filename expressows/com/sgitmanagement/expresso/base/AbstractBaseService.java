package com.sgitmanagement.expresso.base;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.security.Authorizable;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.Util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MultivaluedMap;

abstract public class AbstractBaseService<U extends IUser> implements AutoCloseable, Authorizable<U> {
	private static ThreadLocal<List<AbstractBaseService<?>>> servicesThreadLocal = new ThreadLocal<>();
	public static Logger staticLogger = LoggerFactory.getLogger(AbstractBaseService.class);

	protected Logger logger;
	private HttpServletRequest request;
	private HttpServletResponse response;

	private EntityManager entityManager;
	private Integer parentId;

	protected AbstractBaseService() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	protected AbstractBaseService(HttpServletRequest request, HttpServletResponse response) {
		this();
		this.request = request;
		this.response = response;
	}

	final public EntityManager getEntityManager() {
		return getEntityManager(true);
	}

	final public EntityManager getEntityManager(boolean startTransaction) {
		if (this.entityManager == null) {
			// logger.debug(getClass().getSimpleName() + " Getting new EntityManager");
			this.entityManager = getPersistenceManager().getEntityManager(getPersistenceUnit(), startTransaction);
		} else {
			if (startTransaction) {
				// start the transaction if not started
				getPersistenceManager().startTransaction(this.entityManager);
			}
		}

		return this.entityManager;
	}

	final public Integer getParentId() {
		return parentId;
	}

	final public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}

	public String getPersistenceUnit() {
		return null; // default persistence unit by default
	}

	@SuppressWarnings("unchecked")
	final public U getUser() {
		return (U) UserManager.getInstance().getUser();
	}

	public PersistenceManager getPersistenceManager() {
		return PersistenceManager.getInstance();
	}

	final public HttpServletRequest getRequest() {
		return request;
	}

	final public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	final public HttpServletResponse getResponse() {
		return response;
	}

	final public void setResponse(HttpServletResponse response) {
		this.response = response;
	}

	final public void flush() {
		getEntityManager(false).flush();
	}

	final public void clearCache() {
		// clear the cache
		EntityTransaction tx = getEntityManager(false).getTransaction();
		if (tx.isActive()) {
			flush();
		}

		getEntityManager(false).clear();
		getEntityManager(false).getEntityManagerFactory().getCache().evictAll();
	}

	/**
	 * 
	 *
	 * @throws Exception
	 */
	final public void startTransaction() throws Exception {
		getPersistenceManager().startTransaction(getEntityManager(true));
	}

	/**
	 * Commit the current transaction (only if there is one active). It automatically starts a new one
	 *
	 * @throws Exception
	 */
	final public void commit() throws Exception {
		commit(true);
	}

	/**
	 * Commit the current transaction (only if there is one active). It automatically starts a new one
	 *
	 * @throws Exception
	 */
	final public void commit(boolean startNewTransaction) throws Exception {
		getPersistenceManager().commit(getEntityManager(false), startNewTransaction);
	}

	/**
	 * Rollback the current transaction (only if there is one active). It automatically starts a new one
	 *
	 */
	final public void rollback() {
		getPersistenceManager().rollback(getEntityManager(false), true);

		// then we need to clear the cache
		// because the object in cache are no longer valid
		clearCache();
	}

	final public void process() throws Exception {
		process((MultivaluedMap<String, String>) null);
	}

	public void process(String operation) throws Exception {
		// by default, do nothing
	}

	public void process(MultivaluedMap<String, String> formParams) throws Exception {
		// if not overwritten, call the former process(operation)
		process(formParams != null ? formParams.getFirst("section") : (String) null);
	}

	/**
	 * Inner class only use to get the connection
	 *
	 */
	private static class ConnectionWork implements Work {
		Connection connection;

		@Override
		public void execute(Connection connection) throws SQLException {
			this.connection = connection;
		}

		Connection getConnection() {
			return connection;
		}
	}

	/**
	 * Get the underlining database connection
	 *
	 * @return
	 */
	public Connection getConnection() {
		return getConnection(true);
	}

	public Connection getConnection(boolean startTransaction) {
		return getConnection(getEntityManager(startTransaction));
	}

	/**
	 *
	 * @return
	 */
	public boolean isInternalNetwork() {
		return Util.isInternalIpAddress(Util.getIpAddress(getRequest()));
	}

	/**
	 *
	 * @param entityManager
	 * @return
	 */
	public Connection getConnection(EntityManager entityManager) {
		Session session = entityManager.unwrap(Session.class);
		ConnectionWork connectionWork = new ConnectionWork();
		session.doWork(connectionWork);
		return connectionWork.getConnection();
	}

	/**
	 * Get a service
	 *
	 * @param serviceClass
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <S extends AbstractBaseService<U>> S newService(Class<S> serviceClass) {
		try {
			S service = serviceClass.getDeclaredConstructor().newInstance();
			service.setRequest(getRequest());
			service.setResponse(getResponse());

			if (AbstractBaseEntityService.class.isAssignableFrom(serviceClass) && ((AbstractBaseEntityService) service).getTypeOfE() == null) {
				String entityClassName = serviceClass.getCanonicalName().substring(0, serviceClass.getCanonicalName().length() - "Service".length());
				((AbstractBaseEntityService) service).setTypeOfE(Class.forName(entityClassName));
			}

			registerService(service);

			return service;
		} catch (Exception ex) {
			logger.error("Problem creating the service [" + serviceClass + "]", ex);
			return null;
		}
	}

	/**
	 * Get a service
	 *
	 * @param serviceClass
	 * @param entityClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <S extends AbstractBaseEntityService<T, V, J>, T extends IEntity<J>, V extends IUser, J> S newService(Class<S> serviceClass, Class<T> entityClass) {
		return newService(serviceClass, entityClass, (V) getUser());
	}

	/**
	 * Get a service
	 *
	 * @param serviceClass
	 * @param entityClass
	 * @return
	 */
	public <S extends AbstractBaseEntityService<T, V, J>, T extends IEntity<J>, V extends IUser, J> S newService(Class<S> serviceClass, Class<T> entityClass, V user) {
		try {
			S service = serviceClass.getDeclaredConstructor().newInstance();
			service.setTypeOfE(entityClass);
			service.setRequest(getRequest());
			service.setResponse(getResponse());
			if (user == null) {
				user = service.getSystemUser();
			}
			UserManager.getInstance().setUser(user);

			registerService(service);
			return service;
		} catch (Exception ex) {
			logger.error("Problem creating the service [" + serviceClass + "]", ex);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	static public <S extends AbstractBaseService<V>, V extends IUser> S newServiceStatic(Class<S> serviceClass) throws Exception {
		return newServiceStatic(serviceClass, (V) UserManager.getInstance().getUser());
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
	static public <S extends AbstractBaseService<V>, V extends IUser> S newServiceStatic(Class<S> serviceClass, V user) throws Exception {
		S service = serviceClass.getDeclaredConstructor().newInstance();
		if (user == null) {
			user = service.getSystemUser();
		}
		UserManager.getInstance().setUser(user);
		return service;
	}

	/**
	 * Get the properties for the application
	 *
	 * @return
	 */
	static public Properties getApplicationConfigProperties(String applicationFolder, String configFileName) {
		String applicationConfigDir = SystemEnv.INSTANCE.getDefaultProperties().getProperty("application_config_dir");
		if (applicationConfigDir == null) {
			applicationConfigDir = "";
		}
		return SystemEnv.INSTANCE.getProperties(applicationConfigDir + applicationFolder + "/" + configFileName);
	}

	/**
	 * 
	 * @param applicationFolder
	 * @return
	 */
	static public Properties getApplicationConfigProperties(String applicationFolder) {
		return getApplicationConfigProperties(applicationFolder, "config");
	}

	public <S extends AbstractBaseService<V>, V extends IUser> void registerService(S service) {
		List<AbstractBaseService<?>> services = servicesThreadLocal.get();
		if (services == null) {
			services = new ArrayList<>();
			servicesThreadLocal.set(services);
		}
		services.add(service);
	}

	/**
	 * Close all services for the thread
	 */
	static public void staticCloseServices() {
		List<AbstractBaseService<?>> services = servicesThreadLocal.get();
		if (services != null) {
			for (AbstractBaseService<?> service : services) {
				try {
					service.close();
				} catch (Exception ex) {
					staticLogger.error("Cannot close service [" + service.getClass().getSimpleName() + "]", ex);
				}
			}
		}
		servicesThreadLocal.remove();
	}

	@Override
	public void close() throws Exception {
		// noop
	}
}
