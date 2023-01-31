package com.sgitmanagement.expressoext.notification;

import java.util.Collection;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.notification.NotificationsResource.NotificationResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

@Path("/notification")
public class NotificationsResource extends BaseEntitiesResource<Notification, NotificationService, NotificationResource> {
	public NotificationsResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(Notification.class, request, response, new NotificationResource(request, response), NotificationService.class);
	}

	@GET
	@Path("mine")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<Notification> getMyNotifications() throws Exception {
		// because notification are cached, we need a transaction
		try {
			getPersistenceManager().startTransaction(getEntityManager());

			return getService().getMyNotifications();
		} catch (Exception ex) {
			getPersistenceManager().rollback(getEntityManager());
			throw ex;
		} finally {
			getPersistenceManager().commit(getEntityManager());
		}
	}

	@GET
	@Path("service")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<String> getNotificationServiceDescriptions() throws Exception {
		return getService().getNotificationServiceDescriptions();
	}

	static public class NotificationResource extends BaseEntityResource<Notification, NotificationService> {
		public NotificationResource(HttpServletRequest request, HttpServletResponse response) {
			super(Notification.class, request, response);
		}

		public Notification execute(MultivaluedMap<String, String> formParams) throws Exception {
			String action = formParams.getFirst("actionSelected");
			Notification notification = getService().get(getId());
			return getService().performAction(action, notification);
		}
	}
}
