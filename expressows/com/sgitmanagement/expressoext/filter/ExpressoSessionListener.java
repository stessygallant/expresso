package com.sgitmanagement.expressoext.filter;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class ExpressoSessionListener implements HttpSessionListener {
	final private static Logger logger = LoggerFactory.getLogger(ExpressoSessionListener.class);

	@Override
	public void sessionCreated(HttpSessionEvent sessionEvent) {
		HttpSession httpSession = sessionEvent.getSession();
		logger.debug("Creating new session: " + httpSession.getId() + (httpSession.getAttribute("userId") != null ? " (" + httpSession.getAttribute("userId") + ")" : ""));
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent sessionEvent) {
		logger.debug("Destroying session: " + sessionEvent.getSession().getId());
	}
}