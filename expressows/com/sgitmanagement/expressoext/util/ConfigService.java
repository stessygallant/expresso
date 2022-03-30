package com.sgitmanagement.expressoext.util;

import com.sgitmanagement.expressoext.base.BaseEntityService;

public class ConfigService extends BaseEntityService<Config> {
	/**
	 * Get the entity (config) from the pgmKey
	 *
	 * @param key
	 * @return
	 */
	public Config get(String key) {
		return getEntityManager().createQuery("SELECT e FROM Config e WHERE e.key = :key", getTypeOfE()).setParameter("key", key).getSingleResult();
	}
}
