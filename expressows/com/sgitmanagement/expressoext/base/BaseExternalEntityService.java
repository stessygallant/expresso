package com.sgitmanagement.expressoext.base;

import java.util.Date;
import java.util.Properties;

import org.apache.commons.lang3.time.DateUtils;

import com.sgitmanagement.expresso.base.ExternalEntity;
import com.sgitmanagement.expresso.util.DateUtil;
import com.sgitmanagement.expresso.util.ProgressSender;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expressoext.util.Config;

import jakarta.persistence.NoResultException;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class BaseExternalEntityService<E extends BaseUpdatableEntity & ExternalEntity<Integer>> extends BaseEntityService<E> implements AutoCloseable {

	private ExternalInterface externalInterface;
	private boolean externalInterfaceInitialized = false;
	private boolean autoSynchronize = true;

	// load the external properties mapping file
	final static private Properties externalInterfaceProps;
	static {
		externalInterfaceProps = SystemEnv.INSTANCE.getProperties("externalinterface");
	}

	public ExternalInterface getExternalInterface() throws Exception {
		// initialize the external interface
		if (!this.externalInterfaceInitialized) {
			this.externalInterfaceInitialized = true;
			logger.debug("Getting external interface for class [" + getTypeOfE().getCanonicalName() + "]");

			if (externalInterfaceProps != null) {
				String externalEntityClassName = externalInterfaceProps.getProperty(getTypeOfE().getCanonicalName());

				if (externalEntityClassName != null) {
					this.externalInterface = (ExternalInterface) Class.forName(externalEntityClassName).getDeclaredConstructor().newInstance();
					this.externalInterface.setTypeOfE(getTypeOfE());
					this.externalInterface.setService(this);
				}
			}
		}
		return this.externalInterface;
	}

	/**
	 * By default, create is synchronous (we need to get the extKey back before we continue)
	 */
	@Override
	public E create(E e) throws Exception {
		e = super.create(e);
		if (autoSynchronize && getExternalInterface() != null) {
			getExternalInterface().merge(e, false);
		}
		return e;
	}

	public E create(E e, boolean async) throws Exception {
		e = super.create(e);
		if (autoSynchronize && getExternalInterface() != null) {
			getExternalInterface().merge(e, async);
		}
		return e;
	}

	/**
	 * By default, an update is done asynchronously
	 */
	@Override
	public E update(E v) throws Exception {
		return update(v, true);
	}

	final public E update(E v, boolean async) throws Exception {
		E e;
		if (autoSynchronize && getExternalInterface() != null) {
			e = super.update(v);
			getExternalInterface().merge(e, async);
		} else {
			e = super.update(v);
		}
		return e;
	}

	@Override
	/**
	 * By default, an update is done synchronously
	 */
	public void delete(Integer id) throws Exception {
		delete(id, false);
	}

	final public void delete(Integer id, boolean async) throws Exception {
		E e = get(id);
		if (e != null) {
			if (autoSynchronize && getExternalInterface() != null) {
				getExternalInterface().delete(e, async);
			}
		}
		super.delete(id);
	}

	@Override
	public void sync(String section, ProgressSender progressSender, int progressWeight) throws Exception {
		if (getExternalInterface() != null) {
			Date newSyncDate = new Date();

			if (progressSender != null) {
				progressSender.addLevel("Synchronisation");
			}

			setUpdateLastModified(false);
			getExternalInterface().sync(section, progressSender, progressWeight);

			if (progressSender != null) {
				progressSender.completeLevel();
			}
			setLastSyncDate(newSyncDate);
		}
	}

	final public boolean isAutoSynchronize() {
		return autoSynchronize;
	}

	final public void setAutoSynchronize(boolean autoSynchronize) {
		this.autoSynchronize = autoSynchronize;
	}

	final public String getConfigKey() {
		return "last-sync-" + getTypeOfE().getSimpleName();
	}

	/**
	 *
	 * @return
	 */
	final public Date getLastSyncDate() {
		// get the last sync date
		Date lastSync;
		try {
			Config syncConfig = getEntityManager().createQuery("SELECT s FROM Config s WHERE s.key = :key", Config.class).setParameter("key", getConfigKey()).getSingleResult();
			lastSync = syncConfig.getDatetimeValue();

			// always let 5 minutes overlap
			lastSync = DateUtils.addMinutes(lastSync, -5);

		} catch (NoResultException e) {
			// by default, go back 7 days
			lastSync = DateUtil.addDays(new Date(), -7);
		}
		return lastSync;
	}

	/**
	 *
	 */
	final public void setLastSyncDate(Date date) {
		Config syncConfig;
		try {
			syncConfig = getEntityManager().createQuery("SELECT s FROM Config s WHERE s.key = :key", Config.class).setParameter("key", getConfigKey()).getSingleResult();

			// update the date
			syncConfig.setDatetimeValue(date);
		} catch (NoResultException e) {
			// create a new record for sync config
			syncConfig = new Config(getConfigKey());
			syncConfig.setDatetimeValue(date);
			getEntityManager().persist(syncConfig);
		}
	}

	@Override
	public void close() throws Exception {
		if (this.externalInterface != null && this.externalInterface instanceof AutoCloseable) {
			((AutoCloseable) this.externalInterface).close();
		}
		super.close();
	}
}
