package com.sgitmanagement.expressoext.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.UserManager;
import com.sgitmanagement.expresso.exception.InvalidCredentialsException;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.security.AuthorizationHelper;
import com.sgitmanagement.expressoext.security.User;

import jakarta.persistence.NoResultException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class SessionValidationFilter implements Filter {
	final private static Logger logger = LoggerFactory.getLogger(SessionValidationFilter.class);

	final private static String HEADER_TOKEN = "X-Session-Token";
	final private static String HEADER_IMPERSONATE = "X-Impersonate-User";
	private AutoCreatableUser autoCreatableUser = null;

	@Override
	public void init(FilterConfig config) throws ServletException {
		logger.info("SessionValidationFilter init");

		String autoCreateClassString = config.getInitParameter("autoCreateUserClass");
		if (autoCreateClassString != null) {
			try {
				@SuppressWarnings("unchecked")
				Class<AutoCreatableUser> autoCreatableUserClass = (Class<AutoCreatableUser>) Class.forName(autoCreateClassString);
				autoCreatableUser = autoCreatableUserClass.getDeclaredConstructor().newInstance();
			} catch (Exception ex) {
				logger.error("Cannot intantiate [" + autoCreateClassString + "]", ex);
			}
		}
	}

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		// if the user is not logged yet, session is null
		HttpSession session = request.getSession(false);

		if (session == null) {
			// Session is null. Only public call will go through this
			chain.doFilter(request, response);
		} else {
			// user is authenticated
			User user;
			if (session.getAttribute("userName") != null) {
				user = getUser(session, request, response);

				// store the user in the request
				UserManager.getInstance().setUser(user);

				// validate the session (NOT for PROD for now)
				if (user != null && (SystemEnv.INSTANCE.isInProduction() || isSessionValid(session, request, response, user))) {
					chain.doFilter(request, response);
				} else {
					try {
						session.invalidate();
						request.logout();
					} catch (Exception ex) {
						// ignore
					}
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalidSession");
				}

			} else {
				// first time only
				user = storeUserInfoInSession(session, request, response);

				if (user == null) {
					try {
						session.invalidate();
					} catch (Exception ex) {
						// ignore
					}
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				} else {
					// store the user in the request
					UserManager.getInstance().setUser(user);

					// continue
					chain.doFilter(request, response);
				}
			}
		}
	}

	/**
	 * 
	 * @param session
	 * @param request
	 * @param response
	 * @param em
	 * @return
	 * @throws Exception
	 * @throws InvalidCredentialsException
	 */
	private User getUser(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
		User user = null;

		try {
			// get the user from the session
			user = AuthorizationHelper.getUser((String) session.getAttribute("userName"));

			// if a user is impersonating another user. Only admin user can do it
			User impersonatedUser = null;
			if (AuthorizationHelper.isUserAdmin(user) && request.getHeader(HEADER_IMPERSONATE) != null) {
				String impersonatedUserName = request.getHeader(HEADER_IMPERSONATE);
				try {
					impersonatedUser = AuthorizationHelper.getUser(impersonatedUserName);
				} catch (NoResultException e) {
					logger.warn("User [" + impersonatedUserName + "] not found in database");
				}
			}

			user = impersonatedUser != null ? impersonatedUser : user;
		} catch (Exception ex) {
			logger.warn("Error getting user: " + ex);
		}

		return user;
	}

	/**
	 * 
	 * @param session
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private User storeUserInfoInSession(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
		User user = null;

		try {
			// first retrieve the user
			String authName = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;
			if (authName != null && authName.contains("@")) {
				// remove the domain
				authName = authName.substring(0, authName.indexOf("@"));
			}

			if (authName != null) {
				// load the user
				try {
					user = AuthorizationHelper.getUser(authName);
				} catch (NoResultException ex) {
					logger.info("Cannot find user with userName [" + authName + "]");

					if (autoCreatableUser != null) {
						try {
							user = autoCreatableUser.create(authName);
						} catch (Exception ex1) {
							logger.warn("Cannot create user with userName [" + authName + "]: " + ex1);
						}
					}
				}
			}

			if (user != null) {
				logger.debug("Storing user [" + user.getUserName() + "] in session [" + session.getId() + "] IP[" + Util.getIpAddress(request) + "]");

				session.setAttribute("userName", user.getUserName());
				session.setAttribute("ipAddress", Util.getIpAddress(request));

				// generate a token to validate the request
				String sessionToken = Util.generateRandomToken(64);
				session.setAttribute("sessionToken", sessionToken);

				// store in the response the token
				response.addHeader(HEADER_TOKEN, sessionToken);
			}
		} catch (Exception ex) {
			logger.error("Error storing user: " + ex);
		}
		return user;
	}

	/**
	 * 
	 * @param session
	 * @param request
	 * @param response
	 * @param user
	 * @return
	 */
	private boolean isSessionValid(HttpSession session, HttpServletRequest request, HttpServletResponse response, User user) {
		boolean valid = true;

		// verify if the info in the session is the same
		// String userName = (String) session.getAttribute("userName");
		String ipAddress = (String) session.getAttribute("ipAddress");
		String sessionToken = (String) session.getAttribute("sessionToken");
		String requestToken = request.getHeader(HEADER_TOKEN);

		if (requestToken == null) {
			if (request.getMethod().equals("GET")) {
				// for GET method, no need for the token (we cannot set the HTTP header from a link)
				requestToken = sessionToken;
			} else if (request.getContentType().toLowerCase().indexOf("multipart") != -1) {
				// multipart request contains the token inside the body
				// it will be validated by the multipart parser
				requestToken = sessionToken;
			} else {
				// report will validate it
				if (request.getRequestURI().endsWith("/print") || request.getRequestURI().endsWith("/report")) {
					requestToken = sessionToken;
				}
			}
		}

		if (!Util.equals(Util.getIpAddress(request), ipAddress) || !Util.equals(sessionToken, requestToken)) {
			// valid = false;
			logger.warn("Possible hacking detected [" + user.getUserName() + "] [" + ipAddress + " -> " + Util.getIpAddress(request) + "] [" + sessionToken + " -> " + requestToken + "]");
		}
		return valid;
	}
}
