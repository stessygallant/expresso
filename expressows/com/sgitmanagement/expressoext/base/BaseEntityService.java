package com.sgitmanagement.expressoext.base;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import com.sgitmanagement.expresso.base.AbstractBaseEntityService;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expressoext.document.Document;
import com.sgitmanagement.expressoext.security.AuthorizationHelper;
import com.sgitmanagement.expressoext.security.Resource;
import com.sgitmanagement.expressoext.security.ResourceService;
import com.sgitmanagement.expressoext.security.User;
import com.sgitmanagement.expressoext.util.ReportUtil;

public class BaseEntityService<E extends BaseEntity> extends AbstractBaseEntityService<E, User, Integer> {
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

	/**
	 * 
	 * @param action
	 * @param resourceSecurityPath
	 * @param resourceName
	 * @param resourceId
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	final protected void verifyUserPrivileges(String action, String resourceSecurityPath, String resourceName, Integer resourceId) throws ForbiddenException {
		if (isUserAdmin()) {
			// ok
		} else {
			// verify if the user is allowed to read the resource
			verifyUserPrivileges("read", resourceSecurityPath);

			// the user can read the resource. verify if it can read THIS resource
			if (resourceId != null && resourceId != 0 && resourceId != -1)
				try {
					AbstractBaseEntityService service = newService(resourceName);
					service.get(new Filter("id", resourceId));
				} catch (Exception ex) {
					throw new ForbiddenException(
							"User [" + getUser().getUserName() + "] is not allowed to [" + action + "] the resourceSecurityPath [" + resourceSecurityPath + "] resourceId [" + resourceId + "]");
				}
		}
	}

	/**
	 * 
	 * @param action
	 * @param resourceName
	 * @param resourceId
	 * @throws ForbiddenException
	 */
	final protected void verifyUserPrivileges(String action, String resourceName, Integer resourceId) throws ForbiddenException {
		try {
			Resource resource = newService(ResourceService.class, Resource.class).get(resourceName);
			verifyUserPrivileges(action, resource.getSecurityPath(), resource.getName(), resourceId);
		} catch (ForbiddenException ex) {
			throw ex;
		} catch (Exception ex) {
			logger.error("Cannot validate document privileges", ex);
			throw new ForbiddenException();
		}
	}

	/**
	 *
	 * @param paramMap
	 * @param response
	 * @throws Exception
	 */
	public void print(Map<String, String> paramMap, HttpServletResponse response) throws Exception {
		String reportName = this.getTypeOfE().getSimpleName().toLowerCase();
		String fileName = this.getTypeOfE().getSimpleName();
		ReportUtil.INSTANCE.executeReport(getUser(), reportName, fileName, paramMap, response, response.getOutputStream());
	}

	@Override
	public Integer convertId(String id) {
		try {
			return Integer.parseInt(id);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Verify if the user can upload a document for this resource
	 *
	 * @param resourceId
	 * @throws Exception
	 */
	public void verifyDocumentUploadRestrictions(Integer resourceId) throws Exception {
		// TODO by default a user can upload a document if he can update the resource
		// try {
		// verifyActionRestrictions("update", get(resourceId));
		// } catch (ForbiddenException ex) {
		// throw new ForbiddenException("userNotAllowedToUploadDocument");
		// }
	}

	/**
	 * Verify if the user can download the document from the resource
	 * 
	 * @param document
	 * @param resourceId
	 * @throws Exception
	 */
	public void verifyDocumentDownloadRestrictions(Document document, Integer resourceId) throws Exception {
		// TODO by default, a user can download the document if he can read the resource
		// if (!isUserAllowed("read")) {
		// throw new ForbiddenException("userNotAllowedToDownloadDocument");
		// }
	}
}
