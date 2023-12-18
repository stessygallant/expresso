package com.sgitmanagement.expressoext.base;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;

import com.google.gson.JsonPrimitive;
import com.sgitmanagement.expresso.base.AbstractBaseEntityService;
import com.sgitmanagement.expresso.base.Creatable;
import com.sgitmanagement.expresso.base.RequireApproval;
import com.sgitmanagement.expresso.base.Updatable;
import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.exception.BaseException;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expresso.util.DateUtil;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.modif.RequiredApproval;
import com.sgitmanagement.expressoext.modif.RequiredApprovalService;
import com.sgitmanagement.expressoext.security.AuthorizationHelper;
import com.sgitmanagement.expressoext.security.Resource;
import com.sgitmanagement.expressoext.security.ResourceService;
import com.sgitmanagement.expressoext.security.User;
import com.sgitmanagement.expressoext.security.UserService;
import com.sgitmanagement.expressoext.util.ReportUtil;
import com.sgitmanagement.expressoext.websocket.WebSocketService;

import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.servlet.http.HttpServletResponse;

public class BaseEntityService<E extends BaseEntity> extends AbstractBaseEntityService<E, User, Integer> {
	@Override
	final public boolean isUserInRole(String rolePgmKey) {
		return AuthorizationHelper.isUserInRole(getUser(), rolePgmKey);
	}

	/**
	 * Verify if the user is in the role, but do not include admin user if they are not in role
	 * 
	 * @param rolePgmKey
	 * @return
	 */
	final public boolean isUserInRoleNoAdmin(String rolePgmKey) {
		return AuthorizationHelper.isUserInRole(getUser(), rolePgmKey, false);
	}

	@Override
	final public boolean isUserAdmin() {
		return AuthorizationHelper.isUserAdmin(getUser());
	}

	@Override
	final public boolean isUserAllowed(String action, List<String> resources) {
		return AuthorizationHelper.isUserAllowed(getUser(), action, resources);
	}

	final public boolean isUserAllowed(String action, String resourceName) {
		try {
			Resource resource = newService(ResourceService.class, Resource.class).get(resourceName);
			return AuthorizationHelper.isUserAllowed(getUser(), action, Arrays.asList(resource.getSecurityPath().split("/")));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	final public List<User> getUsersInRole(String rolePgmKey) {
		return AuthorizationHelper.getUsersInRole(rolePgmKey);
	}

	@Override
	final public User getUser(String userName) {
		return AuthorizationHelper.getUser(userName);
	}

	@Override
	final public User getSystemUser() {
		return AuthorizationHelper.getSystemUser();
	}

	@Override
	final public User getPublicUser() {
		return AuthorizationHelper.getPublicUser();
	}

	/**
	 *
	 * @param action
	 * @param resourceSecurityPath
	 */
	final protected void verifyUserPrivileges(String action, String resourceSecurityPath) throws ForbiddenException {
		// verify if the user has the privilege to read/update this resource
		if (!AuthorizationHelper.isUserAllowed(getUser(), action, Arrays.asList(resourceSecurityPath.split("/")))) {
			throw new ForbiddenException("User [" + getUser().getUserName() + "] is not allowed to [" + action + "] the resourceSecurityPath [" + resourceSecurityPath + "]");
		}
	}

	public void print(E e) throws Exception {
		// must be implemented by the subclass
	}

	/**
	 *
	 * @param paramMap
	 * @param response
	 * @throws Exception
	 */
	public void download(E e, Map<String, String> reportParams, HttpServletResponse httpServletResponse) throws Exception {
		String reportName = this.getTypeOfE().getSimpleName().toLowerCase();
		String fileName = this.getTypeOfE().getSimpleName();
		String resourceName = this.getResourceName();

		reportParams.put("id", "" + e.getId());

		// backward compatibility
		reportParams.put(resourceName + "Id", "" + e.getId());
		reportParams.put(resourceName + "Ids", "" + e.getId());
		reportParams.put(resourceName.toLowerCase() + "Id", "" + e.getId());
		reportParams.put(resourceName.toLowerCase() + "Ids", "" + e.getId());

		ReportUtil.INSTANCE.executeReport(getUser(), reportName, fileName, reportParams, httpServletResponse, httpServletResponse.getOutputStream());
	}

	@Override
	public Integer convertId(String id) throws Exception {
		try {
			return Integer.parseInt(id);
		} catch (NumberFormatException e) {
			throw new BaseException(HttpServletResponse.SC_NOT_FOUND, "ID [" + id + "] is not a valid id for entity [" + getTypeOfE().getSimpleName() + "]");
		}
	}

	/**
	 * 
	 * @param source
	 * @param dest
	 * @param field
	 * @throws Exception
	 */
	@Override
	protected void createUpdateApprobationRequired(E e, Field field, Object currentValue, Object newValue) throws Exception {
		String currentStringValue = null;
		String newStringValue = null;
		Integer newValueReferenceId = null;

		// if the field is a reference, get the reference
		if (field.getName().endsWith("Id")) {
			String referenceName = field.getName().substring(0, field.getName().length() - 2);
			newValueReferenceId = (Integer) newValue;

			// get the field associated with it
			Field referenceField = Util.getField(e, referenceName);
			if (referenceField != null) {
				BaseEntity currentValueEntity = null;
				if (currentValue != null) {
					currentValueEntity = (BaseEntity) getEntityManager().find(referenceField.getType(), (Integer) currentValue);
					currentStringValue = currentValueEntity.getLabel();
				}

				BaseEntity newValueEntity = null;
				if (newValue != null) {
					newValueEntity = (BaseEntity) getEntityManager().find(referenceField.getType(), (Integer) newValue);
					if (newValueEntity != null) {
						newStringValue = newValueEntity.getLabel();
					} else {
						logger.warn("Cannot find [" + referenceField.getType() + " with id [" + newValue + "]");
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
					logger.error("Date with no annotation? [" + field.getName() + "]");
				}
			} else {
				currentStringValue = currentValue != null ? "" + currentValue : null;
				newStringValue = newValue != null ? "" + newValue : null;
			}
		}

		// entity name
		String entityClassName = e.getClass().getSimpleName();

		// if this is a proxy object, the entityClassName will contains a underscore (remove it)
		if (entityClassName.indexOf('_') != -1) {
			entityClassName = entityClassName.substring(0, entityClassName.indexOf('_'));
		}

		// create the modification
		String resourceDescription = BeanUtils.getProperty(e, e.getClass().getAnnotation(RequireApproval.class).descriptionFieldName());
		String additionnalInfo = null;
		String additionalInfoFieldName = Util.nullifyIfNeeded(e.getClass().getAnnotation(RequireApproval.class).additionalInfoFieldName());
		if (additionalInfoFieldName != null) {
			additionnalInfo = BeanUtils.getProperty(e, additionalInfoFieldName);
		}
		newService(RequiredApprovalService.class, RequiredApproval.class).create(
				new RequiredApproval(getResourceName(), e.getId(), getResourceNo(e), resourceDescription, field.getName(), currentStringValue, newStringValue, newValueReferenceId, additionnalInfo));
	}

	/**
	 * 
	 * @param creatable
	 * @return
	 */
	public boolean isLastModifiedBySystem(Creatable creatable) {
		if (creatable instanceof Updatable) {
			Updatable updatable = (Updatable) creatable;
			if (updatable.getLastModifiedUser() != null) {
				return updatable.getLastModifiedUser().getUserName().equals(AuthorizationHelper.SYSTEM_USERNAME);
			} else {
				// no update. use creatable
				return creatable.getCreationUser().getUserName().equals(AuthorizationHelper.SYSTEM_USERNAME);
			}
		} else {
			// creatable only
			return creatable.getCreationUser().getUserName().equals(AuthorizationHelper.SYSTEM_USERNAME);
		}
	}

	public Set<String> getEmailsForJobTitle(String jobTitlePgmKey) throws Exception {
		return newService(UserService.class, User.class).list(new Query().setActiveOnly(true).addFilter(new Filter("jobTitle.pgmKey", jobTitlePgmKey))).stream().map(User::getEmail)
				.collect(Collectors.toSet());
	}

	public Set<String> getEmailsForRole(String rolePgmKey) throws Exception {
		return AuthorizationHelper.getUsersInRole(rolePgmKey).stream().map(User::getEmail).collect(Collectors.toSet());
	}

	public void refreshWebSocket() throws Exception {
		// notify if anyone listening
		newServiceStatic(WebSocketService.class).broadcast(getResourceSecurityPath(), new JsonPrimitive("refresh"));
	}
}
