package com.sgitmanagement.expresso.base;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.util.SystemEnv;

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
	public EntityManager getEntityManager(String persistenceUnit, boolean startTransaction) {
		persistenceUnit = "persistence_unit" + (persistenceUnit == null ? "" : "_" + persistenceUnit);
		EntityManagerFactory entityManagerFactory = emFactoryMap.get(persistenceUnit);
		if (entityManagerFactory == null) {
			// create the factory (only one)
			synchronized (instance) {
				// then verify again
				entityManagerFactory = emFactoryMap.get(persistenceUnit);
				if (entityManagerFactory == null) {
					String persistenceUnitValue = SystemEnv.INSTANCE.getDefaultProperties().getProperty(persistenceUnit);
					logger.info("Getting new connection to database: " + persistenceUnitValue);
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
			// logger.debug("New connection to [" + persistenceUnit + "]");
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
						tx.rollback();
					} else {
						try {
							tx.commit();
						} catch (Exception e) {
							thrownException = e;
							// logger.error("Error while committing the transaction: " + e);
							try {
								tx.rollback();
							} catch (Exception e3) {
								// ignore
							}
						}
					}
				}
			} catch (Exception e) {
				// cannot do much at this point
				thrownException = e;
				// logger.error("Unexpected error", e);
			}

			if (startNewTransaction) {
				tx.begin();
				if (thrownException != null) {
					// if there was a problem, do not commit the next transaction
					tx.setRollbackOnly();
				}
			}
		} catch (Exception e2) {
			thrownException = e2;
			// logger.error("Unexpected error", e2);
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
		entityManagersThreadLocal.set(null);
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
			} catch (Exception e) {
				// cannot do much at this point
				logger.error("Cannot rollback", e);
			}

			if (startNewTransaction) {
				tx.begin();
			}
		} catch (Exception e2) {
			logger.error("Unexpected error", e2);
		}
	}

	/**
	 * Utility method to commit and close all opened connection for this thread
	 */
	public void commitAndClose() {
		if (entityManagersThreadLocal.get() != null) {
			for (EntityManager entityManager : entityManagersThreadLocal.get().values()) {
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
		} catch (Exception e2) {
			// ignore
		} finally {
			try {
				em.close();
			} catch (Exception e2) {
				// ignore
			}
		}

		// int activeConnections =
		entityManagerCounter.decrementAndGet();
		// logger.debug("CLOSED: Number of DB connections: " + activeConnections);
	}

	/**
	 * Utility method to close the EntityManager with no exception
	 *
	 * @param em
	 */
	public void close(EntityManager em) {
		try {
			em.close();
		} catch (Exception e2) {
			// ignore
		}
	}

	public int getNumberOpenConnections() {
		return entityManagerCounter.get();
	}

	@Override
	public void close() throws Exception {
		commitAndClose();
	}
}