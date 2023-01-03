package com.sgitmanagement.expressoext.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.audit.AbstractAuditTrailInterceptor;
import com.sgitmanagement.expresso.base.IUser;
import com.sgitmanagement.expresso.base.PersistenceManager;
import com.sgitmanagement.expressoext.modif.AuditTrail;

public class AuditTrailInterceptor extends AbstractAuditTrailInterceptor {
	final static protected Logger logger = LoggerFactory.getLogger(AuditTrailInterceptor.class);

	@Override
	public void createAuditTrail(String resourceName, Integer resourceId, String fieldName, String currentStringValue, String newStringValue, IUser user) {
		try {
			AuditTrail auditTrail = new AuditTrail(resourceName, resourceId, fieldName, currentStringValue, newStringValue, user.getId());
			PersistenceManager.getInstance().getEntityManager().persist(auditTrail);
		} catch (Exception ex) {
			logger.error("Cannot create audit trail", ex);
		}
	}
}
