package com.sgitmanagement.expresso.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.AbstractBaseEntityService;
import com.sgitmanagement.expresso.base.IEntity;
import com.sgitmanagement.expresso.util.Util;

public enum ResourceEventCentral {
	INSTANCE;

	final private Logger logger = LoggerFactory.getLogger(ResourceEventCentral.class);

	// Resource, EventListener
	private Map<String, List<ResourceEvent<?, ?>>> resourceEventListenerMap = new HashMap<>();

	public enum Event {
		Create, Update, Delete
	};

	private ResourceEventCentral() {
		init();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void init() {
		// build a list of all services listening
		resourceEventListenerMap = new HashMap<>();
		try {
			// get all services with EventListenable
			Set<Class<?>> serviceClasses = new Reflections((Object[]) new String[] { "ca.cezinc.expressoservice", "com.sgitmanagement" }).getTypesAnnotatedWith(ResourceEventListener.class);
			for (Class<?> serviceClass : serviceClasses) {
				// logger.debug("Listening for event service [" + serviceClass.getSimpleName() + "]");

				ResourceEventListener resourceEventListener = serviceClass.getAnnotation(ResourceEventListener.class);
				List<String> resourceNames = new ArrayList<>();
				if (Util.isNotNull(resourceEventListener.resourceName())) {
					resourceNames.add(resourceEventListener.resourceName());
				} else {
					resourceNames.addAll(Arrays.asList(resourceEventListener.resourceNames()));
				}

				for (String resourceName : resourceNames) {
					List<ResourceEvent<?, ?>> eventListeners = resourceEventListenerMap.get(resourceName);
					if (eventListeners == null) {
						eventListeners = new ArrayList<>();
						resourceEventListenerMap.put(resourceName, eventListeners);
					}
					// logger.debug("Adding event listener on [" + resourceName + "] for [" + serviceClass.getSimpleName() + "]");
					String entityClassName = serviceClass.getCanonicalName().substring(0, serviceClass.getCanonicalName().length() - "Service".length());
					Class<?> entityClass = Class.forName(entityClassName);
					eventListeners.add(new ResourceEvent(resourceName, serviceClass, entityClass, resourceEventListener.priority()));
				}
			}
		} catch (Exception ex) {
			logger.error("Cannot init ResourceEventCentral", ex);
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void publishResourceEvent(String resourceName, Event event, IEntity<?> entity) throws Exception {
		// logger.debug("publishResourceEvent [" + resourceName + "] event [" + event.name() + "]");
		List<ResourceEvent<?, ?>> eventListeners = resourceEventListenerMap.get(resourceName);
		if (eventListeners != null) {
			// priority listeners
			for (ResourceEvent<?, ?> eventListener : eventListeners) {
				if (eventListener.isPriority()) {
					((ResourceEventListenable) AbstractBaseEntityService.newServiceStatic(eventListener.getServiceClass(), eventListener.getEntityClass())).listenResourceEvent(eventListener, event,
							entity);
				}
			}

			// other listeners
			for (ResourceEvent<?, ?> eventListener : eventListeners) {
				if (!eventListener.isPriority()) {
					((ResourceEventListenable) AbstractBaseEntityService.newServiceStatic(eventListener.getServiceClass(), eventListener.getEntityClass())).listenResourceEvent(eventListener, event,
							entity);
				}
			}
		}
	}
}
