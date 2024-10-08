package com.sgitmanagement.expressoext.document;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.sgitmanagement.expresso.base.AbstractBaseEntityService;
import com.sgitmanagement.expresso.base.DocumentUploadNotification;
import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expressoext.base.BaseFileService;
import com.sgitmanagement.expressoext.security.Resource;
import com.sgitmanagement.expressoext.security.ResourceService;
import com.sgitmanagement.expressoext.util.MainUtil;

import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.servlet.http.HttpServletResponse;

public class DocumentService extends BaseFileService<Document> {

	public static void main(String[] args) throws Exception {
		DocumentService service = newServiceStatic(DocumentService.class, Document.class);
		// service.list();

		service.get("containerDamageItem", 438);

		MainUtil.close();
	}

	@Override
	protected Document merge(Document document, Map<String, String> params) throws Exception {
		document.setDescription(params.get("description"));
		document.setResourceId(Integer.parseInt(params.get("resourceId")));
		document.setResourceName(params.get("resourceName"));

		// optional
		if (params.get("documentTypeId") != null) {
			document.setDocumentTypeId(Integer.parseInt(params.get("documentTypeId")));
		}
		if (params.get("documentTypePgmKey") != null) {
			document.setDocumentTypeId(newService(DocumentTypeService.class, DocumentType.class)
					.get(new Query().addFilter(new Filter("resourceName", params.get("resourceName"))).addFilter(new Filter("pgmKey", params.get("documentTypePgmKey")))).getId());
		}

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

	private void verifyUserPrivileges(Document document, String action) throws ForbiddenException {
		if (document == null) {
			// logger.error("Cannot validate document privileges: document is null");
			// throw new ForbiddenException();
		} else {
			try {
				// verify if the user can perform this action on this resource
				Resource resource = newService(ResourceService.class, Resource.class).get(document.getResourceName());
				super.verifyUserPrivileges(action, resource.getSecurityPath() + "/document", document.getResourceName(), document.getResourceId());
			} catch (ForbiddenException ex) {
				throw ex;
			} catch (Exception ex) {
				logger.warn("Cannot validate document privileges: " + document.getId() + " for user [" + getUser().getUserName() + "]: " + ex);
				throw new ForbiddenException();
			}
		}
	}

	@Override
	public void downloadFile(HttpServletResponse response, Document document, boolean thumbnail) throws Exception {
		// verify if the user has the privilege to read this resource
		verifyUserPrivileges(document, "read");
		super.downloadFile(response, document, thumbnail);
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
				verifyUserPrivileges(document, "read");
				return document;
			} else if (documents.size() > 1) {
				throw new NonUniqueResultException();
			} else {
				throw new NoResultException();
			}
		}
	}

	@Override
	public List<Document> list(Query query) throws Exception {
		// a user can view a document if it can view the resource
		if (query.getFilter("id") != null) {
			if (query.getFilter("id").getIntValue() != -1) {
				Integer id = query.getFilter("id").getIntValue();
				List<Document> documents = new ArrayList<>();
				Document document = get(id);
				verifyUserPrivileges(document, "read");
				documents.add(document);
				return documents;
			} else {
				return new ArrayList<>();
			}
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

	public Document get(String resourceName, Integer resourceId) throws Exception {
		Query query = new Query();
		query.addFilter(new Filter("resourceName", resourceName));
		query.addFilter(new Filter("resourceId", resourceId));
		return get(query);
	}

	@Override
	public Document create(Document document) throws Exception {
		Resource resource = newService(ResourceService.class, Resource.class).get(document.getResourceName());
		verifyUserPrivileges("create", resource.getSecurityPath() + "/document");
		document = super.create(document);

		// notify the resource that there is a new document
		try {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			AbstractBaseEntityService service = newService(document.getResourceName());
			if (service != null && service instanceof DocumentUploadNotification) {
				((DocumentUploadNotification) service).documentUploaded(document.getResourceId(), document.getId());
			}
		} catch (Exception ex) {
			logger.warn("Cannot notify for new document", ex);
		}

		return document;
	}

	@Override
	public void delete(Integer id) throws Exception {
		Document document = get(id);
		Resource resource = newService(ResourceService.class, Resource.class).get(document.getResourceName());
		verifyUserPrivileges("delete", resource.getSecurityPath() + "/document");
		super.delete(id);
	}

	@Override
	public void verifyActionRestrictions(String action, Document document) {
		boolean allowed = false;
		if (document != null) {
			switch (action) {
			case "delete":
				verifyUserPrivileges(document, "delete");
				// if (getUser().getId().equals(document.getCreationUserId()) || isUserAdmin()) {
				allowed = true;
				// }
				break;

			case "update":
				verifyUserPrivileges(document, "update");
				allowed = true;
				break;
			}
		}

		if (!allowed) {
			throw new ForbiddenException();
		}
	}

	/**
	 * 
	 * @param sourceResourceName
	 * @param sourceResourceId
	 * @param targetResourceName
	 * @param targetResourceId
	 * @throws Exception
	 */
	public void copyDocuments(String sourceResourceName, Integer sourceResourceId, String targetResourceName, Integer targetResourceId) throws Exception {
		Filter filter = new Filter();
		filter.addFilter(new Filter("resourceName", sourceResourceName));
		filter.addFilter(new Filter("resourceId", sourceResourceId));
		List<Document> documents = list(filter);
		if (!documents.isEmpty()) {
			for (Document document : documents) {
				// create the record
				getEntityManager().detach(document);
				document.setId(null);
				document.setResourceName(targetResourceName);
				document.setResourceId(targetResourceId);
				create(document);
			}

			// then copy the documents from the directory
			FileUtils.copyDirectory(new File(getAbsoluteFolder(sourceResourceName, sourceResourceId)), new File(getAbsoluteFolder(targetResourceName, targetResourceId)));
		}
	}

	/**
	 * 
	 * @param file
	 * @param targetResourceName
	 * @param targetResourceId
	 * @throws Exception
	 */
	public void addDocument(File file, String targetResourceName, Integer targetResourceId) throws Exception {
		Document document = new Document();
		document.setResourceName(targetResourceName);
		document.setResourceId(targetResourceId);
		document.setFileName(file.getName());
		create(document);

		// then copy the document
		FileUtils.copyFile(file, new File(getAbsoluteFolder(targetResourceName, targetResourceId) + File.separator + file.getName()));
	}
}
