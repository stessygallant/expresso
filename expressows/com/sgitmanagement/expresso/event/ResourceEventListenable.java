package com.sgitmanagement.expresso.event;

import com.sgitmanagement.expresso.base.IEntity;
import com.sgitmanagement.expresso.event.ResourceEventCentral.Event;

public interface ResourceEventListenable {
	public void listenResourceEvent(ResourceEvent<?, ?> eventListener, Event event, IEntity<?> entity) throws Exception;
}
