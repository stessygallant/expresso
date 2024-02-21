package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expresso.base.AbstractBaseEntityService;
import com.sgitmanagement.expresso.exception.ValidationException;
import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.security.ResourcesResource.ResourceResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;

@Path("/resource")
public class ResourcesResource extends BaseEntitiesResource<Resource, ResourceService, ResourceResource> {
	public ResourcesResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Resource.class, request, response, new ResourceResource(request, response), ResourceService.class);
	}

	static public class ResourceResource extends BaseEntityResource<Resource, ResourceService> {
		public ResourceResource(HttpServletRequest request, HttpServletResponse response) {
			super(Resource.class, request, response);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void publish(MultivaluedMap<String, String> formParams) throws Exception {
			Resource resource = getService().get(getId());
			AbstractBaseEntityService resourceService = getService().newService(resource.getName());
			if (resourceService == null) {
				throw new ValidationException("serviceNotFound", "resourceName", resource.getName());
			}

			// com.sgitmanagement.termont.it.tablet becomes sherpa.applications.it.tabletmanager
			String namespace = resourceService.getClass().getPackageName();
			namespace = namespace.replace("com.sgitmanagement.", "");
			namespace = namespace.replace("termont", "sherpa.applications");
			namespace += "manager";

			resourceService.generateResourceManager(namespace);
		}
	}
}