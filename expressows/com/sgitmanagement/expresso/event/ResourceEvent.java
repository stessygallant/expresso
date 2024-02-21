package com.sgitmanagement.expresso.event;

import com.sgitmanagement.expresso.base.AbstractBaseEntityService;
import com.sgitmanagement.expresso.base.IEntity;

@SuppressWarnings("rawtypes")
public class ResourceEvent<S extends AbstractBaseEntityService & ResourceEventListenable, E extends IEntity<Integer>> {
	private String resourceName;

	private Class<S> serviceClass;
	private Class<E> entityClass;

	public ResourceEvent() {
		super();
	}

	public ResourceEvent(String resourceName, Class<S> serviceClass, Class<E> entityClass) {
		this();
		this.resourceName = resourceName;
		this.serviceClass = serviceClass;
		this.entityClass = entityClass;
	}

	public Class<S> getServiceClass() {
		return serviceClass;
	}

	public void setServiceClass(Class<S> serviceClass) {
		this.serviceClass = serviceClass;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public Class<E> getEntityClass() {
		return entityClass;
	}

	public void setEntityClass(Class<E> entityClass) {
		this.entityClass = entityClass;
	}
}
