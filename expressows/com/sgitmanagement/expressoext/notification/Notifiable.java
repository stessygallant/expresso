package com.sgitmanagement.expressoext.notification;

import java.util.List;

import com.sgitmanagement.expressoext.security.User;

public interface Notifiable {

	public String[] getNotificationServiceDescriptions() throws Exception;

	public List<Notification> getNotifications(User user) throws Exception;

	public void performNotificationAction(User user, String action, Notification notification) throws Exception;
}
