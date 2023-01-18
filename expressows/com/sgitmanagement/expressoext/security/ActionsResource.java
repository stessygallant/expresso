package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expressoext.base.BaseOptionResource;
import com.sgitmanagement.expressoext.base.BaseOptionService;
import com.sgitmanagement.expressoext.base.BaseOptionsResource;
import com.sgitmanagement.expressoext.security.ActionsResource.ActionResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/{action:(?i)action}")
public class ActionsResource extends BaseOptionsResource<Action, BaseOptionService<Action>, ActionResource> {
	public ActionsResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Action.class, request, response, new ActionResource(request, response));
	}

	static public class ActionResource extends BaseOptionResource<Action, BaseOptionService<Action>> {
		public ActionResource(HttpServletRequest request, HttpServletResponse response) {
			super(Action.class, request, response);
		}
	}
}