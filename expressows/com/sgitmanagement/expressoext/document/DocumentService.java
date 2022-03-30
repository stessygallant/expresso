package com.sgitmanagement.expressoext.document;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;

import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expresso.util.DateUtil;
import com.sgitmanagement.expressoext.base.BaseFileService;
import com.sgitmanagement.expressoext.security.Resource;
import com.sgitmanagement.expressoext.security.ResourceService;

import jakarta.servlet.http.HttpServletResponse;

public class DocumentService extends BaseFileService<Document> {

	@Override
	protected Document merge(Document document, Map<String, String> params) throws Exception {
		document.setDescription(params.get("description"));
		document.setResourceId(Integer.parseInt(params.get("resourceId")));
		document.setResourceName(params.get("resourceName"));

		// optional
		if (params.get("documentTypeId") != null) {
			document.setDocumentTypeId(Integer.parseInt(params.get("documentTypeId")));
		}
		document.setFromDate(DateUtil.parseDate(params.get("fromDate")));
		document.setToDate(DateUtil.parseDate(params.get("toDate")));

		return merge(document);
	}

	@Override
	protected String getFolder(Document document) {
		return document.getResourceName() + File.separator + document.getResourceId();
	}

	public String getAbsoluteFolder(Document document) {
		return getAbsoluteFolder(document.getResourceName(), document.getResourceId());
	}

	public String getAbsoluteFolder(String resourceName, Integer resourceId) {
		return rootPath + File.separator + resourceName + File.separator + resourceId;
	}

	private void verifyUserPrivileges(Document document) throws ForbiddenException {
		if (document == null) {
			logger.error("Cannot validate document privileges: document is null");
			throw new ForbiddenException();
		} else {
			try {
				Resource resource = newService(ResourceService.class, Resource.class).get(document.getResourceName());

				// the permission read must be allowed on getResourceSecurityPath/document
				super.verifyUserPrivileges("read", resource.getSecurityPath() + "/document");

				// verify if the user can read this resource
				super.verifyUserPrivileges("read", resource.getSecurityPath(), resource.getName(), document.getResourceId());
			} catch (ForbiddenException ex) {
				throw ex;
			} catch (Exception ex) {
				logger.error("Cannot validate document privileges", ex);
				throw new ForbiddenException();
			}
		}
	}

	@Override
	public void downloadFile(HttpServletResponse response, Document document) throws Exception {
		// verify if the user has the privilege to read this resource
		verifyUserPrivileges(document);
		super.downloadFile(response, document);
	}

	@Override
	public Document get(Query query) throws Exception {
		if (query == null) {
			return null;
		} else {
			List<Document> documents = super.list(query);
			Document document;
			if (documents.size() == 1) {
				document = documents.get(0);
				verifyUserPrivileges(document);
				return document;
			} else {
				throw new NoResultException();
			}
		}
	}

	@Override
	public List<Document> list(Query query) throws Exception {
		// a user can view a document if it can view the resource
		if (query.getFilter("id") != null) {
			Integer id = Integer.parseInt("" + query.getFilter("id").getValue());
			List<Document> documents = new ArrayList<>();
			Document document = get(id);
			verifyUserPrivileges(document);
			documents.add(document);
			return documents;
		} else if (query.getFilter("resourceName") != null) {
			String resourceName = (String) query.getFilter("resourceName").getValue();
			Resource resource = newService(ResourceService.class, Resource.class).get(resourceName);
			verifyUserPrivileges("read", resource.getSecurityPath() + "/document");
			return super.list(query);
		} else {
			return new ArrayList<>();
		}
	}

	/**
	 * 
	 * @param resourceName
	 * @param resourceId
	 * @return
	 * @throws Exception
	 */
	public List<Document> list(String resourceName, Integer resourceId) throws Exception {
		Query query = new Query();
		query.addFilter(new Filter("resourceName", resourceName));
		query.addFilter(new Filter("resourceId", resourceId));
		return list(query);
	}

	@Override
	public Document create(Document document) throws Exception {
		Resource resource = newService(ResourceService.class, Resource.class).get(document.getResourceName());
		verifyUserPrivileges("create", resource.getSecurityPath() + "/document");
		return super.create(document);
	}

	@Override
	public void delete(Integer id) throws Exception {
		Document document = get(id);
		// user can only delete its own document
		if (!getUser().getId().equals(document.getCreationUserId()) && !isUserAdmin()) {
			throw new ForbiddenException("User [" + getUser().getUserName() + "] is not allowed to delete the document from another user for [" + document.getResourceName() + "]");
		}
		super.delete(id);
	}

	@Override
	public void verifyActionRestrictions(String action, Document document) {
		boolean allowed = false;
		switch (action) {
		case "delete":
		case "update":
			if (document != null) {
				verifyUserPrivileges(document);
				allowed = true;
			}
			break;
		}

		if (!allowed) {
			throw new ForbiddenException();
		}
	}
}
