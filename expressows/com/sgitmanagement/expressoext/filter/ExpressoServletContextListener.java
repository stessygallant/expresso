package com.sgitmanagement.expressoext.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.util.Mailer;
import com.sgitmanagement.expresso.util.SystemEnv;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class ExpressoServletContextListener implements ServletContextListener {
	final private static Logger logger = LoggerFactory.getLogger(ExpressoSessionListener.class);

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		// init SystemEnv
		SystemEnv.INSTANCE.getEnv();

		logger.info("ServletContextListener started");
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {

		// terminate any service, etc
		Mailer.INSTANCE.close();

		logger.info("ServletContextListener destroyed");
	}
}