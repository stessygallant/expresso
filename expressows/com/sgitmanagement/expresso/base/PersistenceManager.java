package com.sgitmanagement.expresso.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.util.SystemEnv;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Persistence;
import jakarta.persistence.RollbackException;

public class PersistenceManager implements AutoCloseable {

	final static protected Logger logger = LoggerFactory.getLogger(PersistenceManager.class);
	private static Map<String, EntityManagerFactory> emFactoryMap = new HashMap<>();
	private static PersistenceManager instance;

	// EntityManager are NOT thread safe
	private static ThreadLocal<Map<String, EntityManager>> entityManagersThreadLocal = new ThreadLocal<>();
	private static AtomicInteger entityManagerCounter = new AtomicInteger(0);

	static {
		instance = new PersistenceManager();
	}

	static public PersistenceManager getInstance() {
		return instance;
	}

	// make sure application cannot instantiate the PersistenceManager
	private PersistenceManager() {
	}

	public EntityManager getEntityManager() {
		return getEntityManager(null, true);
	}

	public EntityManager getEntityManager(String persistenceUnit) {
		return getEntityManager(persistenceUnit, true);
	}

	public EntityManager getEntityManager(boolean startTransaction) {
		return getEntityManager(null, startTransaction);
	}

	/**
	 * Get a new entity manager (create the factory if needed)
	 *
	 * @param persistenceUnit
	 * @param startTransaction
	 * @return
	 */
	public EntityManager getEntityManager(String persistenceUnitName, boolean startTransaction) {
		String persistenceUnit = "persistence_unit" + (persistenceUnitName == null ? "" : "_" + persistenceUnitName);
		EntityManagerFactory entityManagerFactory = emFactoryMap.get(persistenceUnit);
		if (entityManagerFactory == null) {
			// create the factory (only one)
			synchronized (instance) {
				// then verify again
				entityManagerFactory = emFactoryMap.get(persistenceUnit);
				if (entityManagerFactory == null) {
					String persistenceUnitValue = SystemEnv.INSTANCE.getDefaultProperties().getProperty(persistenceUnit);
					logger.info("New factory to database: " + persistenceUnitValue);
					entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitValue);
					emFactoryMap.put(persistenceUnit, entityManagerFactory);
				}
			}
		}

		// keep a reference to the entityManager for closing at the end of this request
		if (entityManagersThreadLocal.get() == null) {
			entityManagersThreadLocal.set(new HashMap<>());
		}

		Map<String, EntityManager> entityManagerMap = entityManagersThreadLocal.get();
		EntityManager entityManager = entityManagerMap.get(persistenceUnit);
		if (entityManager == null || !entityManager.isOpen()) {
			// Create the entity manager
			entityManager = createEntityManager(entityManagerFactory);
			entityManagerMap.put(persistenceUnit, entityManager);
			persistenceUnitName = (persistenceUnitName == null ? "default" : persistenceUnitName);
			entityManager.setProperty("expresso.persistence_unit", persistenceUnitName);
			// logger.info("NEW connection to [" + persistenceUnitName + "] startTransaction [" + startTransaction + "]");
		} else {
			// logger.info("GOT connection to [" + persistenceUnitName + "] startTransaction [" + startTransaction + "]");
		}

		// Start the transaction
		if (startTransaction) {
			startTransaction(entityManager);
		}

		return entityManager;
	}

	/**
	 * 
	 * @param entityManagerFactory
	 * @return
	 */
	private EntityManager createEntityManager(EntityManagerFactory entityManagerFactory) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		// for (Map.Entry<String, Object> prop : entityManagerFactory.getProperties().entrySet()) {
		// System.out.println(prop.getKey() + "= " + prop.getValue());
		// }

		// put custom properties
		String databaseDialect = (String) entityManagerFactory.getProperties().get("hibernate.dialect");
		// logger.info("Using database dialect [" + databaseDialect + "]");
		if (databaseDialect == null) {
			logger.error("Database dialect 'hibernate.dialect' not defined in persistence.xml");
		} else if (databaseDialect.contains("Oracle")) {
			entityManager.setProperty("expresso.trunc_date_function", "TRUNC");
			entityManager.setProperty("expresso.case_sensitive", true);
			entityManager.setProperty("expresso.flush_mode", null);
			entityManager.setProperty("expresso.empty_string_is_null", true);
			entityManager.setProperty("expresso.in_max_values", 1000);
		} else if (databaseDialect.contains("MySQL")) {
			entityManager.setProperty("expresso.trunc_date_function", "date");
			entityManager.setProperty("expresso.case_sensitive", false);
			entityManager.setProperty("expresso.flush_mode", "commit");
			entityManager.setProperty("expresso.empty_string_is_null", false);
		} else if (databaseDialect.contains("SQLServer")) {
			entityManager.setProperty("expresso.trunc_date_function", "cast");
			entityManager.setProperty("expresso.case_sensitive", false);
			entityManager.setProperty("expresso.flush_mode", "commit");
			entityManager.setProperty("expresso.empty_string_is_null", false);
		} else {
			logger.error("Database dialect [" + databaseDialect + "] not recognized in persistence.xml");
		}

		// in order to avoid multiple flush because of query (this will create multiple audits),
		// we need to define the flush at the commit time only
		// the code will flush if needed
		String flushMode = (String) entityManager.getProperties().get("expresso.flush_mode");
		if (flushMode != null && flushMode.equals("commit")) {
			// this works well for MySQL, but it does not work for Oracle (persist does not return ID)
			entityManager.setFlushMode(FlushModeType.COMMIT);
		}

		// increase the count
		int activeConnections = entityManagerCounter.incrementAndGet();
		if (activeConnections > 50) {
			logger.warn("Number of active DB connections: " + activeConnections);
		}
		return entityManager;
	}

	public EntityTransaction startTransaction(EntityManager em) {
		EntityTransaction tx = em.getTransaction();
		if (!tx.isActive()) {
			tx.begin();
		}
		return tx;
	}

	/**
	 * Utility method to commit all opened connection for this thread
	 */
	public void commit() throws Exception {
		if (entityManagersThreadLocal.get() != null) {
			for (EntityManager entityManager : entityManagersThreadLocal.get().values()) {
				commit(entityManager);
			}
		}
	}

	public void commit(EntityManager em) throws Exception {
		commit(em, false);
	}

	/**
	 *
	 * @param em
	 * @param startNewTransaction
	 * @throws RollbackException
	 */
	public void commit(EntityManager em, boolean startNewTransaction) throws Exception {
		Exception thrownException = null;
		try {
			EntityTransaction tx = em.getTransaction();
			try {
				if (tx.isActive()) {
					if (tx.getRollbackOnly()) {
						// logger.debug("Rollbacking");
						tx.rollback();
					} else {
						try {
							tx.commit();
							// logger.debug("Committed");
						} catch (Exception ex) {
							thrownException = ex;
							logger.warn("Error while committing the transaction: " + ex);
							try {
								tx.rollback();
							} catch (Exception e3) {
								// ignore
							}
						}
					}
				}
			} catch (Exception ex) {
				// cannot do much at this point
				thrownException = ex;
				logger.warn("Unexpected error", ex);
			}

			if (startNewTransaction) {
				tx.begin();
				if (thrownException != null) {
					// if there was a problem, do not commit the next transaction
					tx.setRollbackOnly();
				}
			}
		} catch (Exception ex) {
			thrownException = ex;
			logger.warn("Unexpected error", ex);
		}

		if (thrownException != null) {
			throw thrownException;
		}
	}

	/**
	 * Utility method to commit and close all opened connection for this thread
	 */
	public void rollback() {
		if (entityManagersThreadLocal.get() != null) {
			for (EntityManager entityManager : entityManagersThreadLocal.get().values()) {
				rollback(entityManager);
			}
		}
	}

	public void rollback(EntityManager em) {
		rollback(em, false);
	}

	/**
	 * Rollback the current transaction (no exception thrown)
	 *
	 * @param em
	 * @param startNewTransaction
	 */
	public void rollback(EntityManager em, boolean startNewTransaction) {
		try {
			EntityTransaction tx = em.getTransaction();
			try {
				if (tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ex) {
				// cannot do much at this point
				logger.error("Cannot rollback", ex);
			}

			if (startNewTransaction) {
				tx.begin();
			}
		} catch (Exception ex) {
			logger.error("Unexpected error", ex);
		}
	}

	/**
	 * Utility method to commit and close all opened connection for this thread
	 */
	public void commitAndClose() {
		if (entityManagersThreadLocal.get() != null) {
			for (EntityManager entityManager : new ArrayList<>(entityManagersThreadLocal.get().values())) {
				commitAndClose(entityManager);
			}
		}
		entityManagersThreadLocal.remove();
	}

	/**
	 * Utility method to commit and close the EntityManager with no exception
	 *
	 * @param em
	 */
	public void commitAndClose(EntityManager em) {
		try {
			commit(em, false);
		} catch (Exception ex) {
			logger.warn("Cannot commitAndClose entity manager: " + ex);
		} finally {
			close(em);
		}
	}

	/**
	 * Utility method to close the EntityManager with no exception
	 *
	 * @param em
	 */
	public void close(EntityManager em) {
		entityManagerCounter.decrementAndGet();

		// remove it from the cache
		entityManagersThreadLocal.get().remove(em.getProperties().get("expresso.persistence_unit"));

		// logger.info("CLOSED [" + em.getProperties().get("expresso.persistence_unit") + "]: Number of DB connections: " + entityManagerCounter.get());
		try {
			em.close();
		} catch (Exception ex) {
			logger.warn("Cannot close entity manager: " + ex);
		}
	}

	@Override
	public void close() throws Exception {
		commitAndClose();
	}
}