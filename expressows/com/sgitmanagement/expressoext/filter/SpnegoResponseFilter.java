package com.sgitmanagement.expressoext.filter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.codec.binary.Base64;
import org.ietf.jgss.GSSException;
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
	private static final String WWW_NEGOCIATE = "Negotiate";
	private static final String WWW_BASIC = "Basic ";
	private static final String WWW_AUTHORIZATION = "Authorization";

	private boolean debug = false;

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) req;
		HttpServletResponse httpServletResponse = (HttpServletResponse) resp;

		String requestAuthorization = httpServletRequest.getHeader(WWW_AUTHORIZATION);

		try {
			if (debug && requestAuthorization != null) {
				for (String token : Collections.list(httpServletRequest.getHeaders(WWW_AUTHORIZATION))) {
					if (token.length() > 20) {
						token = token.substring(token.length() - 20) + " (=" + token.length() + " chars)";
					}
					logger.info(WWW_AUTHORIZATION + "=" + token);
				}
			}

			// we must create a session (to store the authorization)
			HttpSession httpSession = httpServletRequest.getSession(true);

			// we need to keep the Authorization in the session for direct GET URL (which does not contains the
			// Authorization Header).
			if (requestAuthorization != null && httpSession.getAttribute(WWW_AUTHORIZATION) == null && requestAuthorization.startsWith(WWW_BASIC)) {
				// if a user tries to connect user BASIC AUTH in the internal network, report it (should be Negociate)
				String ip = Util.getIpAddress(httpServletRequest);
				String userAgent = httpServletRequest.getHeader("User-Agent");
				boolean mobile = userAgent.indexOf("Mobile") != -1 || httpServletRequest.getParameter("mobile") != null;
				if (mobile || !Util.isInternalIpAddress(ip)) {
					// ok, they cannot use SSO Kerberos
				} else {
					String userPassBase64 = requestAuthorization.substring(WWW_BASIC.length());
					String userPassDecoded = new String(Base64.decodeBase64(userPassBase64));
					String authUser = userPassDecoded.substring(0, userPassDecoded.indexOf(':'));
					logger.warn("Got a SSO call with Basic Auth for internal user[" + authUser + "] IP[" + ip + "] User-Agent[" + userAgent + "]");
				}
				httpSession.setAttribute(WWW_AUTHORIZATION, requestAuthorization);
			}

			// use the authorization in the session
			if (httpSession.getAttribute(WWW_AUTHORIZATION) != null && requestAuthorization == null) {
				httpServletRequest = new HttpServletRequestWrapper(httpServletRequest) {
					@Override
					public String getHeader(String name) {
						// logger.info("getHeader: " + name);
						if (name.equalsIgnoreCase(WWW_AUTHORIZATION)) {
							return (String) httpSession.getAttribute(WWW_AUTHORIZATION);
						} else {
							return super.getHeader(name);
						}
					}
				};
			}

			// we need to have a final request variable for inner function
			final HttpServletRequest request2 = httpServletRequest;

			httpServletResponse = new HttpServletResponseWrapper(httpServletResponse) {
				@Override
				public void setHeader(String name, String value) {
					// logger.info("setHeader(" + name + "," + value + ")");

					if (isValidHeader(request2, name, value)) {
						super.setHeader(name, value);
					}
				}

				@Override
				public void addHeader(String name, String value) {
					// logger.info("addHeader(" + name + "," + value + ")");

					if (isValidHeader(request2, name, value)) {
						super.addHeader(name, value);
					}
				}
			};

			try {
				chain.doFilter(request2, httpServletResponse);
			} catch (UnsupportedOperationException ex) {
				// got a NTLM token
				logger.warn("UnsupportedOperationException: " + ex.getMessage());
				httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				httpServletResponse.addHeader(WWW_AUTHENTICATE_HEADER, WWW_NEGOCIATE);
			}

			if (debug) {
				logger.info("SpneGo status: " + httpServletResponse.getStatus());
				for (String headerName : httpServletResponse.getHeaderNames()) {
					Collection<String> headerValues = httpServletResponse.getHeaders(headerName);
					if (headerValues != null && headerValues.size() > 0) {
						for (String headerValue : headerValues) {
							logger.info("RESPONSE header [" + headerName + "=" + headerValue + "]");
						}
					}
				}
			}

			int httpCode = httpServletResponse.getStatus();
			if (httpCode == HttpServletResponse.SC_UNAUTHORIZED) {
				// it may take 3 calls before getting the Kerberos token
				if (debug) {
					logger.info("Replying UNAUTHORIZED: " + httpServletRequest.getPathInfo() + ": " + httpServletRequest.getHeader("Cookie") + ":" + httpServletRequest.getHeader(WWW_AUTHORIZATION));
				}

				// DO NOT do this: it will remove the authorization for the Basic Auth
				// if login failed, remove the WWW_AUTHORIZATION from the session
				// try {
				// httpSession.removeAttribute(WWW_AUTHORIZATION);
				// httpSession.invalidate();
				// } catch (IllegalStateException ex) {
				// may happen when logout: Session already invalidated
				// ignore
				// }

				// if user is not authenticated, it will return
				// WWW-Authenticate: Negotiate
			}
		} catch (Exception ex) {
			if (ex instanceof ServletException && ex.getCause() != null && ex.getCause() instanceof GSSException) {
				httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				logger.warn("SpnegoResponseFilter Kerberos error: " + ex);
			} else {
				throw ex;
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

	private boolean isValidHeader(HttpServletRequest httpServletRequest, String name, String value) {
		if (name.equalsIgnoreCase(WWW_AUTHENTICATE_HEADER)) {

			// we do not want to return a negociate message if it is not on the local network
			// (we want to avoid the browser credentials popup window)
			// Negotiate may trigger that the browser will try to start NTLM
			// restrict the possibility to the internal network

			String ip = Util.getIpAddress(httpServletRequest);
			boolean mobile = httpServletRequest.getHeader("User-Agent").indexOf("Mobile") != -1;

			if (mobile || !Util.isInternalIpAddress(ip)) {
				if (debug) {
					logger.info("Remove header: Not on Termont network [" + name + "=" + value + "]");
				}
				return false;
			}
		}

		// Never return Basic realm="DOMAIN.COM". This will trigger a basic Auth
		if (value != null && value.toUpperCase().startsWith("BASIC")) {
			return false;
		}

		// by default, header is valid
		return true;
	}

}
