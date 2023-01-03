package com.sgitmanagement.expresso.audit;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Interceptor;
import org.hibernate.annotations.Formula;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.FieldRestriction;
import com.sgitmanagement.expresso.base.IEntity;
import com.sgitmanagement.expresso.base.IUser;
import com.sgitmanagement.expresso.base.PersistenceManager;
import com.sgitmanagement.expresso.base.UserManager;
import com.sgitmanagement.expresso.util.DateUtil;
import com.sgitmanagement.expresso.util.Util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;

abstract public class AbstractAuditTrailInterceptor implements Interceptor {
	final static protected Logger logger = LoggerFactory.getLogger(AbstractAuditTrailInterceptor.class);
	private static ThreadLocal<Set<String>> auditThreadLocal = new ThreadLocal<>();

	public AbstractAuditTrailInterceptor() {
		super();
		// logger.debug("New AbstractAuditTrailInterceptor");
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		if (entity instanceof Auditable) {
			try {
				// record the audit trail
				for (int i = 0; i < currentState.length; i++) {
					Object previousValue = previousState[i];
					Object currentValue = currentState[i];
					Type type = types[i];

					if (!type.isEntityType() && !type.isAssociationType() && !type.isCollectionType() && !Util.equals(previousValue, currentValue)) {

						try {
							String prop = propertyNames[i];
							Field field = Util.getField(entity, prop);

							recordAuditTrail((IEntity) entity, field, previousValue, currentValue);
						} catch (Exception ex) {
							logger.error("Cannot record audit trail: " + ex);
						}
					}
				}
			} catch (Exception e) {
				logger.error("Error in AuditTrailInterceptor.onFlushDirty", e);
			}
		}
		return false;
	}

	/**
	 * 
	 * @param e
	 * @param field
	 * @param currentValue
	 * @param newValue
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	private void recordAuditTrail(IEntity e, Field field, Object currentValue, Object newValue) throws Exception {
		String fieldName = field != null ? field.getName() : "";

		// entity name
		// if this is a proxy object, the entityClassName will contains a underscore (remove it)
		String entityClassName = e.getClass().getSimpleName();
		if (entityClassName.indexOf('_') != -1) {
			entityClassName = entityClassName.substring(0, entityClassName.indexOf('_'));
		}

		if (auditThreadLocal.get() == null) {
			auditThreadLocal.set(new HashSet<String>());
		}

		// because flush could be called multiples times in the same transaction, we need to verify
		String key = entityClassName + ":" + e.getId() + ":" + fieldName + ":" + currentValue + ":" + newValue;
		if (auditThreadLocal.get().contains(key)) {
			// logger.debug("Avoid double auditing: " + key);
		} else {
			auditThreadLocal.get().add(key);

			if (fieldName.equals("") || fieldName.equals("lastModifiedUserId") || fieldName.equals("lastModifiedDate") || fieldName.equals("creationUserId") || fieldName.equals("creationDate")) {
				// do not log
			} else if ((currentValue == null && (newValue != null && newValue.equals(""))) || (newValue == null && (currentValue != null && currentValue.equals("")))) {
				// ignore difference between null and empty
			} else if (field != null && (field.isAnnotationPresent(ForbidAudit.class) || field.isAnnotationPresent(FieldRestriction.class) || field.isAnnotationPresent(Formula.class)
					|| field.isAnnotationPresent(Transient.class))) {
				// do not audit
			} else if (fieldName.endsWith("Ids")) {
				// do not audit many to many
			} else {
				// logger.debug("AUDIT TRAIL - Date[" + new Date() + "] Id[" + e.getId() + "] User[" + UserManager.getInstance().getUser().getUserName() + "] Entity[" + e.getClass().getSimpleName()
				// + "] Field[" + fieldName + "] currentValue[" + currentValue + "] NewValue[" + newValue + "]");

				try {
					String currentStringValue = null;
					String newStringValue = null;

					// if the field is a reference, get the reference
					if (fieldName.endsWith("Id")) {
						String listName = fieldName.substring(0, fieldName.length() - 2);

						// get the field associated with it
						Field listField = Util.getField(e, listName);
						if (listField != null) {
							// logger.debug("listField.getDeclaringClass(): " + listField.getType().getName());
							EntityManager em = PersistenceManager.getInstance().getEntityManager();

							IEntity currentValueEntity = null;
							if (currentValue != null) {
								currentValueEntity = (IEntity) em.find(listField.getType(), (Integer) currentValue);
								currentStringValue = currentValueEntity.getLabel();
							}

							IEntity newValueEntity = null;
							if (newValue != null) {
								newValueEntity = (IEntity) em.find(listField.getType(), (Integer) newValue);
								if (newValueEntity != null) {
									newStringValue = newValueEntity.getLabel();
								} else {
									logger.warn("Cannot find [" + listField.getType() + " with id [" + newValue + "]");
								}
							}
						}
					} else {
						if (currentValue instanceof Date || newValue instanceof Date) {
							// format date or date time
							if (field.isAnnotationPresent(Temporal.class)) {
								Temporal temporalAnnotation = field.getAnnotation(Temporal.class);

								if (temporalAnnotation.value().equals(TemporalType.TIMESTAMP)) {
									// date time
									currentStringValue = DateUtil.formatDate((Date) currentValue, DateUtil.DATETIME_NOSEC_FORMAT_TL.get());

									newStringValue = DateUtil.formatDate((Date) newValue, DateUtil.DATETIME_NOSEC_FORMAT_TL.get());
								} else {
									// date only
									currentStringValue = DateUtil.formatDate((Date) currentValue, DateUtil.DATE_FORMAT_TL.get());

									newStringValue = DateUtil.formatDate((Date) newValue, DateUtil.DATE_FORMAT_TL.get());
								}
							} else {
								logger.error("Date with no annotation? [" + fieldName + "]");
							}
						} else if (currentValue instanceof Boolean || newValue instanceof Boolean) {
							currentStringValue = currentValue != null && ((Boolean) currentValue).booleanValue() ? "Oui" : "Non";
							newStringValue = newValue != null && ((Boolean) newValue).booleanValue() ? "Oui" : "Non";
						} else {
							currentStringValue = currentValue != null ? "" + currentValue : null;
							newStringValue = newValue != null ? "" + newValue : null;
						}
					}

					// max 1000 char
					int maxLength = 1000;
					if (currentStringValue != null && currentStringValue.length() > maxLength) {
						currentStringValue = currentStringValue.substring(0, maxLength - 1);
					}
					if (newStringValue != null && newStringValue.length() > maxLength) {
						newStringValue = newStringValue.substring(0, maxLength - 1);
					}

					// save Audit (only if it changed)
					if (!Util.equals(newStringValue, currentStringValue)) {
						// NOTE: only support Integer id
						createAuditTrail(StringUtils.uncapitalize(entityClassName), (Integer) e.getId(), fieldName, currentStringValue, newStringValue, UserManager.getInstance().getUser());
					}
				} catch (Exception ex) {
					// do not stop because there is a problem with the AuditTrail
					logger.error("Cannot record the audit trail", ex);
				}
			}
		}
	}

	static public void close() {
		auditThreadLocal.remove();
	}

	abstract public void createAuditTrail(String resourceName, Integer resourceId, String fieldName, String currentStringValue, String newStringValue, IUser user);
}
