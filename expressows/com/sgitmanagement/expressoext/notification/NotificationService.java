package com.sgitmanagement.expressoext.notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;

import org.reflections.Reflections;

import com.sgitmanagement.expresso.base.UserManager;
import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expresso.dto.Query.Sort;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseEntityService;
import com.sgitmanagement.expressoext.security.User;
import com.sgitmanagement.expressoext.security.UserService;
import com.sgitmanagement.expressoext.util.MainUtil;

import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;

public class NotificationService extends BaseEntityService<Notification> {
	private static final Set<Class<? extends Notifiable>> notifiableServiceClasses;
	private static final Map<String, Object> keyLocks = new ConcurrentHashMap<>();

	private static final Cache<String, Object> notificationsCache;
	private static final boolean USE_CACHE = false;

	static {
		// get the list of notification service
		Reflections reflections = new Reflections("com.sgitmanagement");
		notifiableServiceClasses = reflections.getSubTypesOf(Notifiable.class);

		// Create cache
		CachingProvider provider = Caching.getCachingProvider();
		CacheManager cacheManager = provider.getCacheManager();

		notificationsCache = cacheManager.createCache("NotificationServiceCache",
				new MutableConfiguration<String, Object>().setTypes(String.class, Object.class).setStoreByValue(false).setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_MINUTE)));
	}

	@Override
	protected Filter getSearchFilter(String term) {
		Filter filter = new Filter(Logic.or);
		filter.addFilter(new Filter("resourceName", Operator.contains, term));
		filter.addFilter(new Filter("resourceExtKey", Operator.contains, term));
		filter.addFilter(new Filter("resourceTitle", Operator.contains, term));
		filter.addFilter(new Filter("description", Operator.contains, term));
		return filter;
	}

	@Override
	protected Filter getActiveOnlyFilter() throws Exception {
		Filter filter = new Filter();
		filter.addFilter(getDeactivableFilter());
		filter.addFilter(new Filter("performedAction", Operator.isNull));
		return filter;
	}

	@Override
	protected Filter getRestrictionsFilter() throws Exception {
		if (isUserAdmin()) {
			return null;
		} else {
			// user can see only notification for itself
			return new Filter("notifiedUserId", getUser().getId());
		}
	}

	@Override
	protected Sort[] getDefaultQuerySort() {
		return new Query.Sort[] { new Query.Sort("resourceTitle", Query.Sort.Direction.asc) };
	}

	@Override
	public void verifyActionRestrictions(String action, Notification notification) throws Exception {
		boolean allowed = false;
		if (notification != null) {
			switch (action) {
			case "update":
			case "delete":
			case "execute":
			default:
				// if (notification.getPerformedAction() == null || isUserAdmin()) {
				allowed = true;
				// }
				break;
			}
		}

		if (!allowed) {
			throw new ForbiddenException();
		}
	}

	/**
	 * Get a list of all available services
	 * 
	 * @return
	 * @throws Exception
	 */
	public Collection<String> getNotificationServiceDescriptions() throws Exception {
		List<String> serviceDescriptions = new ArrayList<>();
		for (Class<? extends Notifiable> notifiableServiceClass : notifiableServiceClasses) {
			String notifiableServiceClassName = notifiableServiceClass.getName();
			Notifiable notifiableService = getNotifiableService(notifiableServiceClassName);
			serviceDescriptions.addAll(Arrays.asList(notifiableService.getNotificationServiceDescriptions()));
		}

		return serviceDescriptions;
	}

	/**
	 * Get all the notifications from all providers for this user. Providers are responsible for delegation
	 * 
	 * @return
	 * @throws Exception
	 */
	public Collection<Notification> getMyNotifications() throws Exception {
		Collection<Notification> notifications = null;

		final String key = getUser().getUserName();
		if (USE_CACHE) {
			// create a list with a lock because we do not want to retrieve the same notifications for
			// the same user at the same time (this will cause a constraint error in the database)
			if (notificationsCache.get(key) == null) {
				synchronized (keyLocks.computeIfAbsent(key, k -> k)) {
					if (notificationsCache.get(key) == null) {
						notifications = retrieveNotifications(getUser());

						// do not keep the notifications in the cache (only the key)
						notificationsCache.put(key, key);
					}
				}
			}

			if (notifications == null) {
				// get the notifications from the notification table
				// do not use the ones in the cache because they would be outdated
				notifications = list(new Query().setActiveOnly(true).addFilter(new Filter("notifiedUserId", getUser().getId())));
			}
		} else {
			synchronized (keyLocks.computeIfAbsent(key, k -> k)) {
				notifications = retrieveNotifications(getUser());
			}
		}

		return notifications;
	}

	/**
	 * 
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private Collection<Notification> retrieveNotifications(User user) throws Exception {
		logger.debug("Retrieving notifications for user [" + user.getUserName() + "]");
		Date startTime = new Date();

		Collection<Notification> notifications = Collections.synchronizedCollection(new ArrayList<>());

		// build a list of executors
		ExecutorService taskExecutor = Executors.newFixedThreadPool(notifiableServiceClasses.size());

		// for each provider, get the notifications
		for (Class<? extends Notifiable> notifiableServiceClass : notifiableServiceClasses) {
			final String notifiableServiceClassName = notifiableServiceClass.getName();

			taskExecutor.execute(new Runnable() {
				@Override
				public void run() {
					// retrieve the notifications for the service
					NotificationService notificationService = newServiceStatic(NotificationService.class, Notification.class, user);
					try {

						// logger.debug("Getting notification from [" + notifiableServiceClassName + "]");
						// get the service with a new EntityManager (EntityManager are not thread safe)
						Notifiable notifiableService = notificationService.getNotifiableService(notifiableServiceClassName);

						// do not use retrieveNotifications from the main thread, it needs to use the new session to
						// avoid unsafe use of the session org.hibernate.AssertionFailure
						notifications.addAll(notificationService.retrieveNotifications(notifiableService, user));

						// logger.debug("Got notification from [" + notifiableServiceClassName + "]");
					} catch (Exception ex) {
						logger.error("Cannot get the notifications from [" + notifiableServiceClassName + "]", ex);
					} finally {
						logger.debug("Closing [" + notifiableServiceClassName + "]");
						Util.closeCurrentThreadInfo();
					}
				}
			});
		}

		// wait until all competed
		taskExecutor.shutdown();
		taskExecutor.awaitTermination(300, TimeUnit.SECONDS);

		Date endTime = new Date();
		logger.debug(String.format("Retrieved %d notifications from %d services in %.1f seconds", notifications.size(), notifiableServiceClasses.size(),
				(endTime.getTime() - startTime.getTime()) / 1000.0));

		// build a map with all notifications ID
		Set<Integer> notificationIds = new HashSet<>();
		for (Notification notification : notifications) {
			notificationIds.add(notification.getId());
		}

		// deactivate all notifications that are not longer available
		Query notifiedUserQuery = new Query().setActiveOnly(true).addFilter(new Filter("notifiedUserId", getUser().getId()));
		for (Notification notification : list(notifiedUserQuery)) {
			// if the notification if not part of the current list, deactivate it
			if (!notificationIds.contains(notification.getId())) {
				notification.setDeactivationDate(new Date());
			}
		}

		// return the notifications
		return notifications;
	}

	/**
	 * 
	 * @param notifiableService
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private List<Notification> retrieveNotifications(Notifiable notifiableService, User user) throws Exception {
		List<Notification> notifications = new ArrayList<>();

		// get the notifications
		logger.debug("retrieveNotifications: " + notifiableService.getClass().getSimpleName());
		List<Notification> notificationsFromService = notifiableService.getNotifications(user);

		// complete/merge notifications
		for (Notification notification : notificationsFromService) {
			// complete the notification
			if (notification.getUserId() == null) {
				notification.setUserId(user.getId());
			}
			notification.setNotifiedUserId(user.getId());// notified to the current user
			notification.setNotifiableServiceClassName(notifiableService.getClass().getName());

			// find the notification if already registered
			Filter filter = new Filter();
			filter.addFilter(new Filter("resourceName", notification.getResourceName()));
			filter.addFilter(new Filter("resourceId", notification.getResourceId()));
			filter.addFilter(new Filter("resourceExtKey", notification.getResourceExtKey()));
			filter.addFilter(new Filter("userId", notification.getUserId()));
			filter.addFilter(new Filter("notifiedUserId", notification.getNotifiedUserId()));
			filter.addFilter(new Filter("resourceStatusPgmKey", notification.getResourceStatusPgmKey()));

			try {
				Notification foundNotification = get(filter);
				if (foundNotification.getDeactivationDate() == null) {
					// // max 3 month?
					// if (foundNotification.getCreationDate().before(DateUtil.addDays(new Date(), -90))) {
					// foundNotification.setDeactivationDate(new Date());
					// foundNotification.setNotes(notification.getNotes() + ". Cette notification a été annulée automatiquement après 3 mois");
					// merge(foundNotification);
					// notification = null;
					// } else {
					// update the foundNotification with the notification
					foundNotification.setResourceTitle(notification.getResourceTitle());
					foundNotification.setServiceDescription(notification.getServiceDescription());
					foundNotification.setDescription(notification.getDescription());
					foundNotification.setLastModifiedDate(notification.getLastModifiedDate());
					foundNotification.setLastModifiedUserId(notification.getLastModifiedUserId());
					foundNotification.setRequestedDate(notification.getRequestedDate());
					foundNotification.setRequesterUserId(notification.getRequesterUserId());
					foundNotification.setResourceUrl(notification.getResourceUrl());
					foundNotification.setUserId(notification.getUserId());
					foundNotification.setAvailableActions(notification.getAvailableActions());
					foundNotification.setNotes(notification.getNotes());
					notification = foundNotification;
					// }
				} else {
					// notification has been deactivated, ignore the new notification
					notification = null;
				}

			} catch (NoResultException ex) {
				// ok a new one
			}

			if (notification != null) {
				// Make sure that the column width are respected
				if (notification.getDescription() != null && notification.getDescription().length() > 2000) {
					notification.setDescription(notification.getDescription().substring(0, 2000));
				}

				if (notification.getResourceTitle() != null && notification.getResourceTitle().length() > 255) {
					notification.setResourceTitle(notification.getResourceTitle().substring(0, 255));
				}
				if (notification.getResourceName() != null && notification.getResourceName().length() > 200) {
					notification.setResourceName(notification.getResourceName().substring(0, 200));
				}

				// save the notification
				merge(notification);

				// then add it to the complete list
				notifications.add(notification);
			}
		}

		return notifications;
	}

	/**
	 * 
	 * @param resourceName
	 * @param resourceId
	 * @param userId
	 * @param action
	 * @return
	 * @throws Exception
	 */
	public Notification performAction(String resourceName, Integer resourceId, Integer userId, String action) throws Exception {
		// find the notification
		Query query = new Query().setActiveOnly(true);
		query.addFilter(new Filter("resourceName", resourceName));
		query.addFilter(new Filter("resourceId", resourceId));
		query.addFilter(new Filter("notifiedUserId", userId));
		try {
			Notification notification = get(query);
			return performAction(action, notification);
		} catch (NoResultException ex) {
			logger.warn("Cannot find the notifications [" + resourceName + ":" + resourceId + ":" + userId + "]");
			return null;
		} catch (NonUniqueResultException ex) {
			logger.warn("Found multiples notifications [" + resourceName + ":" + resourceId + ":" + userId);
			return null;
		}
	}

	/**
	 * 
	 * @param action
	 * @param notification
	 * @throws Exception
	 */
	public Notification performAction(String action, Notification notification) throws Exception {
		Notifiable notifiableService = getNotifiableService(notification.getNotifiableServiceClassName());

		// update the notification
		notification.setPerformedAction(action);
		notification.setPerformedActionDate(new Date());
		notification.setPerformedActionUserId(getUser().getId());
		notification.setDeactivationDate(new Date());
		update(notification);

		notifiableService.performNotificationAction(getUser(), action, notification);
		return notification;
	}

	/**
	 * Utility method to get the service class from the string
	 * 
	 * @param serviceClassName
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Notifiable getNotifiableService(String serviceClassName) throws Exception {
		String resourceClassName = serviceClassName.substring(0, serviceClassName.length() - "Service".length());

		Notifiable notifiable;
		Class serviceClass = Class.forName(serviceClassName);
		try {
			Class resourceClass = Class.forName(resourceClassName);
			notifiable = (Notifiable) newService(serviceClass, resourceClass);
		} catch (ClassNotFoundException ex) {
			// service not based on a resource
			notifiable = (Notifiable) newService(serviceClass);
		}
		return notifiable;
	}

	public static void main(String[] args) throws Exception {
		Thread[] threads = new Thread[10];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						final NotificationService service = newServiceStatic(NotificationService.class, Notification.class);
						UserManager.getInstance().setUser(service.newService(UserService.class, User.class).get(1));
						service.getMyNotifications();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						Util.closeCurrentThreadInfo();
					}
				}
			});
		}

		for (int i = 0; i < threads.length; i++) {
			threads[i].start();
		}

		for (int i = 0; i < threads.length; i++) {
			threads[i].join();
		}
		MainUtil.close();
	}

}
