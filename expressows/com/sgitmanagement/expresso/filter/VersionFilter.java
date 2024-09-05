package com.sgitmanagement.expresso.filter;

import java.io.IOException;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.util.Util;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet Filter implementation class SecurityFilter
 */
public class VersionFilter implements Filter {
	final private static Logger logger = LoggerFactory.getLogger(VersionFilter.class);
	private static String currentVersion;
	private final static int SC_UPGRADE_NEEDED = 426;
	private boolean debug = false;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpServletResponse httpServletResponse = (HttpServletResponse) response;

		if (debug) {
			logger.info("VersionFilter - HTTP Header");
			Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				logger.info(" Header [" + headerName + "=" + httpServletRequest.getHeader(headerName) + "]");
			}
		}

		String requestVersion = httpServletRequest.getHeader("X-Version");

		// keep only the main and major version (remove minor)
		// remove the last digits of the version ###.###[.###]
		if (requestVersion != null) {
			if (requestVersion.lastIndexOf('.') != -1) {
				requestVersion = requestVersion.substring(0, requestVersion.lastIndexOf('.'));
			} else {
				requestVersion = null;
			}
		}

		if (requestVersion == null || requestVersion.equals(currentVersion)) {
			try {
				chain.doFilter(request, response);
			} finally {
				// As the VersionFilter is the first filter, the last command must be to close all
				Util.closeCurrentThreadInfo();
			}
		} else {
			logger.debug("Upgrade needed");
			httpServletResponse.setStatus(SC_UPGRADE_NEEDED);
		}
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		currentVersion = config.getInitParameter("version");
	}

	@Override
	public void destroy() {
	}
}
