package com.sgitmanagement.expressoext.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.AbstractBaseService;
import com.sgitmanagement.expresso.base.PersistenceManager;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class PersistenceManagerFilter implements Filter {
	final private static Logger logger = LoggerFactory.getLogger(PersistenceManagerFilter.class);

	@Override
	public void init(FilterConfig config) throws ServletException {
		logger.info("PersistenceManagerFilter init");
	}

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		try {
			chain.doFilter(req, resp);
			PersistenceManager.getInstance().commit();
		} catch (Exception ex) {
			// Application exception are not caught here
			// Jersey catch them and write it in the response
			// the only error here are from the commit
			logger.error("Error committing", ex);
			PersistenceManager.getInstance().rollback();
		} finally {
			// close all services
			AbstractBaseService.staticCloseServices();
		}
	}
}
