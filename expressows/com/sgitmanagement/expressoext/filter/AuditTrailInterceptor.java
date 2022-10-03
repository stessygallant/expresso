package com.sgitmanagement.expressoext.filter;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.audit.AbstractAuditTrailInterceptor;
import com.sgitmanagement.expresso.base.IUser;
import com.sgitmanagement.expresso.base.PersistenceManager;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.modif.AuditTrail;

public class AuditTrailInterceptor extends AbstractAuditTrailInterceptor {
	final static protected Logger logger = LoggerFactory.getLogger(AuditTrailInterceptor.class);
	private static ThreadLocal<Map<String, String>> sqlParamMapThreadLocal = new ThreadLocal<>();

	@Deprecated
	static public void setSQLParamMap(Map<String, String> map) {
		sqlParamMapThreadLocal.set(map);
	}

	static public void clear() {
		sqlParamMapThreadLocal.remove();
	}

	@Override
	public void createAuditTrail(String resourceName, Integer resourceId, String fieldName, String currentStringValue, String newStringValue, IUser user) {
		try {
			AuditTrail auditTrail = new AuditTrail(resourceName, resourceId, fieldName, currentStringValue, newStringValue, user.getId());
			PersistenceManager.getInstance().getEntityManager().persist(auditTrail);
		} catch (Exception ex) {
			logger.error("Cannot create audit trail: " + ex);
		}
	}

	@Override
	public String onPrepareStatement(String sql) {

		// this is used by @Formula
		// we will try to replace placeholder if needed
		if (sql.indexOf('{') != -1) {
			Map<String, String> map = sqlParamMapThreadLocal.get();
			if (map == null) {
				// logger.warn("There is parameters in the SQL Formula but there is no map provided");
				// we need to replace the key with null
				sql = sql.replaceAll("\\{[a-zA-z0-9_]*\\}", "NULL");
			} else {
				sql = Util.replacePlaceHolders(sql, map);
			}
		}

		return super.onPrepareStatement(sql);
	}
}
