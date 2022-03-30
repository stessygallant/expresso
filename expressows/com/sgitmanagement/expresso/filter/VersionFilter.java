package com.sgitmanagement.expresso.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet Filter implementation class SecurityFilter
 */
public class VersionFilter implements Filter {
	final private static Logger logger = LoggerFactory.getLogger(VersionFilter.class);
	private static String currentVersion;
	private final static int SC_UPGRADE_NEEDED = 426;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;

		// logger.info("HTTP Header");
		// Enumeration<String> headerNames = req.getHeaderNames();
		// while (headerNames.hasMoreElements()) {
		// String headerName = headerNames.nextElement();
		// logger.info(" Header [" + headerName + "=" + req.getHeader(headerName) + "]");
		// }

		String requestVersion = req.getHeader("X-Version");

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
			chain.doFilter(request, response);
		} else {
			logger.debug("Upgrade needed");
			HttpServletResponse resp = (HttpServletResponse) response;
			resp.setStatus(SC_UPGRADE_NEEDED);
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
