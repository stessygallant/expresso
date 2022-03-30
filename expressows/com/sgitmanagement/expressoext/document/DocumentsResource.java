package com.sgitmanagement.expressoext.document;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

import com.sgitmanagement.expressoext.base.BaseFileResource;
import com.sgitmanagement.expressoext.base.BaseFilesResource;
import com.sgitmanagement.expressoext.document.DocumentsResource.DocumentResource;

@Path("/document")
public class DocumentsResource extends BaseFilesResource<Document, DocumentService, DocumentResource> {

	public DocumentsResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Document.class, request, response, new DocumentResource(request, response), DocumentService.class);
	}

	static public class DocumentResource extends BaseFileResource<Document, DocumentService> {

		public DocumentResource(HttpServletRequest request, HttpServletResponse response) {
			super(Document.class, request, response);
		}
	}
}
