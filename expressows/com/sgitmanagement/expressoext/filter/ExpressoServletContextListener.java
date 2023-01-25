package com.sgitmanagement.expressoext.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.PooledDataSource;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.mail.Mailer;
import com.sgitmanagement.expressoext.ldap.ActiveDirectoryLDAPClient;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class ExpressoServletContextListener implements ServletContextListener {
	private static Logger logger;

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		// init SystemEnv
		SystemEnv.INSTANCE.getEnv();

		logger = LoggerFactory.getLogger(ExpressoSessionListener.class);
		logger.info("ServletContextListener started");
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {

		// terminate any service, etc
		Mailer.INSTANCE.close();
		ActiveDirectoryLDAPClient.INSTANCE.close();

		// close connection pool
		for (Object o : C3P0Registry.getPooledDataSources()) {
			try {
				PooledDataSource pooledDataSource = (PooledDataSource) o;
				logger.info("Closing C3P0 PooledDataSource: " + pooledDataSource.getDataSourceName() + " " + pooledDataSource.getAllUsers() + " Connections: "
						+ pooledDataSource.getNumConnectionsAllUsers());
				pooledDataSource.close();
			} catch (Exception ex) {
				logger.warn("Cannot close C3P0 PooledDataSource: " + ex);
			}
		}

		logger.info("ServletContextListener destroyed");
	}
}