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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		try {
			// continue
			chain.doFilter(request, response);
		} finally {

			// close all services
			AbstractBaseService.closeServices();

			// close the connection
			PersistenceManager.getInstance().commitAndClose();
		}
	}
}
