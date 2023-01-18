package com.sgitmanagement.expressoext.document;

import com.sgitmanagement.expressoext.base.BaseOptionResource;
import com.sgitmanagement.expressoext.base.BaseOptionsResource;
import com.sgitmanagement.expressoext.document.DocumentTypesResource.DocumentTypeResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("documentType")
public class DocumentTypesResource extends BaseOptionsResource<DocumentType, DocumentTypeService, DocumentTypeResource> {

	public DocumentTypesResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(DocumentType.class, request, response, new DocumentTypeResource(request, response), DocumentTypeService.class);
	}

	static public class DocumentTypeResource extends BaseOptionResource<DocumentType, DocumentTypeService> {
		public DocumentTypeResource(HttpServletRequest request, HttpServletResponse response) {
			super(DocumentType.class, request, response);
		}
	}
}
