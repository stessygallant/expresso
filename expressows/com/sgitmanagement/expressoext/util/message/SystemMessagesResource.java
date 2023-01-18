package com.sgitmanagement.expressoext.util.message;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.util.message.SystemMessagesResource.SystemMessageResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/systemMessage")
public class SystemMessagesResource extends BaseEntitiesResource<SystemMessage, SystemMessageService, SystemMessageResource> {
	public SystemMessagesResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(SystemMessage.class, request, response, new SystemMessageResource(request, response), SystemMessageService.class);
	}

	static public class SystemMessageResource extends BaseEntityResource<SystemMessage, SystemMessageService> {
		public SystemMessageResource(HttpServletRequest request, HttpServletResponse response) {
			super(SystemMessage.class, request, response);
		}
	}
}
