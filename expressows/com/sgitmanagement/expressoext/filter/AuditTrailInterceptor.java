package com.sgitmanagement.expressoext.filter;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.annotations.Formula;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.Auditable;
import com.sgitmanagement.expresso.base.Creatable;
import com.sgitmanagement.expresso.base.FieldRestriction;
import com.sgitmanagement.expresso.base.ForbidAudit;
import com.sgitmanagement.expresso.base.PersistenceManager;
import com.sgitmanagement.expresso.base.Updatable;
import com.sgitmanagement.expresso.util.DateUtil;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseEntity;
import com.sgitmanagement.expressoext.modif.AuditTrail;
import com.sgitmanagement.expressoext.security.User;
import com.sgitmanagement.expressoext.security.UserService;

public class AuditTrailInterceptor extends EmptyInterceptor {
	final static protected Logger logger = LoggerFactory.getLogger(AuditTrailInterceptor.class);

	private static ThreadLocal<Integer> userIdThreadLocal = new ThreadLocal<>();
	private static ThreadLocal<Map<String, String>> sqlParamMapThreadLocal = new ThreadLocal<>();

	static public void setUserId(Integer userId) {
		userIdThreadLocal.set(userId);
	}

	static public void setSQLParamMap(Map<String, String> map) {
		sqlParamMapThreadLocal.set(map);
	}

	static public void clear() {
		userIdThreadLocal.remove();
		sqlParamMapThreadLocal.remove();
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		try {
			if (!(entity instanceof Creatable)) {
				return false;
			}

			for (int i = 0; i < propertyNames.length; i++) {
				if ("creationDate".equals(propertyNames[i])) {
					if (state[i] == null) {
						state[i] = new Date();
					}
				}
				if ("creationUserId".equals(propertyNames[i])) {
					if (state[i] == null) {
						state[i] = getUserId();
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error in AuditTrailInterceptor.onSave", e);
		}
		return true;
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		try {
			if (!(entity instanceof Updatable)) {
				return false;
			}

			// we cannot do it here because this method could be called multiple times in the same transaction
			// Updatable updatable = (Updatable) entity;
			// for (int i = 0; i < propertyNames.length; i++) {
			// if ("lastModifiedDate".equals(propertyNames[i])) {
			// updatable.setLastModifiedDate(new Date());
			// currentState[i] = updatable.getLastModifiedDate();
			// }
			// if ("lastModifiedUserId".equals(propertyNames[i])) {
			// updatable.setLastModifiedUserId(getUserId());
			// currentState[i] = updatable.getLastModifiedUserId();
			// }
			// }

			// record the audit trail
			if (entity instanceof Auditable) {
				for (int i = 0; i < currentState.length; i++) {
					Object previousValue = previousState[i];
					Object currentValue = currentState[i];
					Type type = types[i];

					if (!type.isEntityType() && !type.isAssociationType() && !type.isCollectionType() && !Util.equals(previousValue, currentValue)) {

						try {
							String prop = propertyNames[i];
							Field field = Util.getField(entity, prop);
							recordAuditTrail((BaseEntity) entity, field, previousValue, currentValue);
						} catch (Exception ex) {
							logger.error("Cannot record audit trail: " + ex);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error in AuditTrailInterceptor.onFlushDirty", e);
		}
		return true;
	}

	private Integer getUserId() throws Exception {
		Integer userId = userIdThreadLocal.get();
		if (userId == null) {
			// get the system user
			UserService userService = UserService.newServiceStatic(UserService.class, User.class);
			User user = userService.getSystemUser();
			userId = user.getId();
			userIdThreadLocal.set(userId);
		}
		return userId;
	}

	private void recordAuditTrail(BaseEntity e, Field field, Object currentValue, Object newValue) throws Exception {

		EntityManager em = PersistenceManager.getInstance().getEntityManager();

		if (em == null) {
			logger.warn("AuditTrailInterceptor not initialized properly: missing EntityManager");
			return;
		}

		Integer userId = getUserId();

		String fieldName = field != null ? field.getName() : "";

		if (fieldName == null || fieldName.trim().equals("") || fieldName.equals("lastModifiedUserId") || fieldName.equals("lastModifiedDate") || fieldName.equals("creationUserId")
				|| fieldName.equals("creationDate")) {
			// do not log
		} else if ((currentValue == null && (newValue != null && newValue.equals(""))) || (newValue == null && (currentValue != null && currentValue.equals("")))) {
			// ignore difference between null and empty
		} else if (field != null && (field.isAnnotationPresent(ForbidAudit.class) || field.isAnnotationPresent(FieldRestriction.class) || field.isAnnotationPresent(Formula.class)
				|| field.isAnnotationPresent(Transient.class))) {
			// do not audit
		} else if (fieldName.endsWith("Ids")) {
			// do not audit
		} else {
			// logger.debug("AUDIT TRAIL - Date[" + new Date() + "] Id[" + e.getId() + "] User[" + getUser()
			// + "] Entity[" + e.getClass().getSimpleName() + "] Field[" + fieldName + "] currentValue["
			// + currentValue + "] NewValue[" + newValue + "]");

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

						BaseEntity currentValueEntity = null;
						if (currentValue != null) {
							currentValueEntity = (BaseEntity) em.find(listField.getType(), (Integer) currentValue);
							currentStringValue = currentValueEntity.getLabel();
						}

						BaseEntity newValueEntity = null;
						if (newValue != null) {
							newValueEntity = (BaseEntity) em.find(listField.getType(), (Integer) newValue);
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
						currentStringValue = currentValue != null && (Boolean) currentValue ? "Oui" : "Non";
						newStringValue = newValue != null && (Boolean) newValue ? "Oui" : "Non";
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

				// entity name
				String entityClassName = e.getClass().getSimpleName();

				// if this is a proxy object, the entityClassName will contains a underscore (remove it)
				if (entityClassName.indexOf('_') != -1) {
					entityClassName = entityClassName.substring(0, entityClassName.indexOf('_'));
				}

				// save Audit (only if it changed)
				if (!Util.equals(newStringValue, currentStringValue)) {
					AuditTrail auditTrail = new AuditTrail(StringUtils.uncapitalize(entityClassName), e.getId(), fieldName, currentStringValue, newStringValue, userId);
					em.persist(auditTrail);
				}
			} catch (Exception ex) {
				// do not stop because there is a problem with the AuditTrail
				logger.error("Cannot record the audit trail", ex);
			}
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

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		super.afterTransactionCompletion(tx);

		// clean the thread local
		userIdThreadLocal.remove();
		sqlParamMapThreadLocal.remove();
	}
}
