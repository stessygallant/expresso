package com.sgitmanagement.expressoext.filter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

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
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.servlet.http.HttpSession;

/**
 * We need to remove the BASIC auth support from Kerberos (it triggers the browser native Basic Auth window)
 *
 */
public class SpnegoResponseFilter implements Filter {
	final private static Logger logger = LoggerFactory.getLogger(SpnegoResponseFilter.class);

	private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
	// private static final String WWW_NEGOCIATE = "Negotiate";
	private static final String WWW_AUTHORIZATION = "Authorization";

	private boolean debug = false;

	private boolean isValidHeader(HttpServletRequest request, HttpServletResponse response, String name, String value) {
		if (name.equalsIgnoreCase(WWW_AUTHENTICATE_HEADER)) {

			// we do not want to return a negociate message if it is not on the local network
			// (we want to avoid the browser credentials popup window)
			// Negotiate may trigger that the browser will try to start NTLM
			// restrict the possibility to the internal network

			String ip = Util.getIpAddress(request);
			boolean mobile = request.getHeader("User-Agent").indexOf("Mobile") != -1;

			if (mobile || !Util.isInternalIpAddress(ip)) {
				if (debug) {
					logger.info("Remove header: Not on Termont network [" + name + "=" + value + "]");
				}
				return false;
			}
		}

		// by default, header is valid
		return true;
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		if (debug) {
			for (String headerName : Collections.list(request.getHeaderNames())) {
				Collection<String> headerValues = Collections.list(request.getHeaders(headerName));
				if (headerValues != null && headerValues.size() > 0) {
					for (String headerValue : headerValues) {
						logger.info("REQUEST header [" + headerName + "=" + headerValue + "]");
					}
				}
			}
		}

		// we must create a session (to store the authorization)
		HttpSession session = request.getSession(true);

		// we need to keep the Authorization in the session for direct GET URL (which does not contains the
		// Authorization Header).
		if (request.getHeader(WWW_AUTHORIZATION) != null && session.getAttribute(WWW_AUTHORIZATION) == null && request.getHeader(WWW_AUTHORIZATION).startsWith("Basic")) {
			session.setAttribute(WWW_AUTHORIZATION, request.getHeader(WWW_AUTHORIZATION));
		}

		// use the authorization in the session
		if (session.getAttribute(WWW_AUTHORIZATION) != null && request.getHeader(WWW_AUTHORIZATION) == null) {
			request = new HttpServletRequestWrapper(request) {
				@Override
				public String getHeader(String name) {
					// System.out.println("getHeader: " + name);
					if (name.equalsIgnoreCase(WWW_AUTHORIZATION)) {
						return (String) session.getAttribute(WWW_AUTHORIZATION);
					} else {
						return super.getHeader(name);
					}
				}
			};
		}

		// if (request.getHeader(WWW_AUTHORIZATION) != null) {
		// logger.info("WWW_AUTHORIZATION: [" + request.getHeader(WWW_AUTHORIZATION) + "]");
		// }

		// we need to have a final request variable for inner function
		final HttpServletRequest request2 = request;
		chain.doFilter(request2, new HttpServletResponseWrapper(response) {
			@Override
			public void setHeader(String name, String value) {
				// logger.info("setHeader(" + name + "," + value + ")");

				if (isValidHeader(request2, response, name, value)) {
					super.setHeader(name, value);
				}
			}

			@Override
			public void addHeader(String name, String value) {
				// logger.info("addHeader(" + name + "," + value + ")");

				if (isValidHeader(request2, response, name, value)) {
					super.addHeader(name, value);
				}
			}
		});

		if (debug) {
			logger.info("SpneGo status: " + response.getStatus());

			for (String headerName : response.getHeaderNames()) {
				Collection<String> headerValues = response.getHeaders(headerName);
				if (headerValues != null && headerValues.size() > 0) {
					for (String headerValue : headerValues) {
						logger.info("RESPONSE header [" + headerName + "=" + headerValue + "]");
					}
				}
			}
		}
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		logger.info("SpnegoResponseFilter init");
	}

	@Override
	public void destroy() {
	}
}
