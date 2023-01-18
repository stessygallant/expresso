package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.security.UserPreferencesResource.UserPreferenceResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;

public class UserPreferencesResource extends BaseEntitiesResource<UserPreference, UserPreferenceService, UserPreferenceResource> {
	public UserPreferencesResource(@Context HttpServletRequest request, @Context HttpServletResponse response, Integer parentId) {
		super(UserPreference.class, request, response, new UserPreferenceResource(request, response), UserPreferenceService.class, parentId);
	}

	static public class UserPreferenceResource extends BaseEntityResource<UserPreference, UserPreferenceService> {
		public UserPreferenceResource(HttpServletRequest request, HttpServletResponse response) {
			super(UserPreference.class, request, response);
		}
	}
}