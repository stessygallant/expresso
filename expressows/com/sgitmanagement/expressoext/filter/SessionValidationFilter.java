package com.sgitmanagement.expressoext.filter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.IUser;
import com.sgitmanagement.expresso.base.UserManager;
import com.sgitmanagement.expresso.exception.InvalidCredentialsException;
import com.sgitmanagement.expresso.util.ServerTimingUtil;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.security.AuthorizationHelper;
import com.sgitmanagement.expressoext.security.BasicUser;
import com.sgitmanagement.expressoext.security.BlockedIPAddress;
import com.sgitmanagement.expressoext.security.BlockedIPAddressService;

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

	private static Set<String> blockedIpAddresses = null;

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

		ServerTimingUtil.startTiming("SessionValidationFilter");

		// if the user is not logged yet, session is null
		HttpSession session = request.getSession(false);

		if (session == null) {
			// Session is null. Only public call will go through this
			if (isIPAddressValid(session, request, response)) {
				chain.doFilter(request, response);
			}
		} else {
			// user is authenticated
			IUser user;
			if (session.getAttribute("userName") != null) {
				user = getUser(session, request, response);

				// store the user in the request
				UserManager.getInstance().setUser(user);

				// validate the session
				if (isSessionValid(session, request, response, user) && isIPAddressValid(session, request, response)) {
					chain.doFilter(request, response);
				}

			} else {
				// first time only
				user = storeUserInfoInSession(session, request, response);

				if (user == null) {
					invalidateSession(session, request, response, HttpServletResponse.SC_UNAUTHORIZED);
				} else {
					if (isIPAddressValid(session, request, response)) {
						// store the user in the request
						UserManager.getInstance().setUser(user);

						// continue
						chain.doFilter(request, response);
					}
				}
			}
		}
		ServerTimingUtil.endTiming();
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
	private IUser getUser(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
		ServerTimingUtil.startTiming("getUser");
		IUser user = null;

		try {
			// get the user from the session
			user = AuthorizationHelper.getUser((String) session.getAttribute("userName"));

			// if a user is impersonating another user. Only admin user can do it
			IUser impersonatedUser = null;
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
		ServerTimingUtil.endTiming();
		return user;
	}

	/**
	 * 
	 * @param session
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private BasicUser storeUserInfoInSession(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
		BasicUser user = null;

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
							autoCreatableUser.create(authName);
							user = AuthorizationHelper.getUser(authName);
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
	private boolean isSessionValid(HttpSession session, HttpServletRequest request, HttpServletResponse response, IUser user) {
		if (user == null) {
			invalidateSession(session, request, response, HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}

		// verify if the info in the session is the same
		// String userName = (String) session.getAttribute("userName");
		String ipAddress = (String) session.getAttribute("ipAddress");
		String sessionToken = (String) session.getAttribute("sessionToken");
		String requestToken = request.getHeader(HEADER_TOKEN);

		if (requestToken == null) {
			if (request.getMethod().equals("GET")) {
				// for GET method, no need for the token (we cannot set the HTTP header from a link)
				requestToken = sessionToken;
			} else if (request.getContentType() != null && request.getContentType().toLowerCase().indexOf("multipart") != -1) {
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

		String actualIPAddress = Util.getIpAddress(request);
		if (!Util.equals(actualIPAddress, ipAddress) || !Util.equals(sessionToken, requestToken)) {
			logger.debug("Possible hacking detected [" + user.getUserName() + "] [" + ipAddress + " -> " + actualIPAddress + "] [" + sessionToken + " -> " + requestToken + "]");
			// invalidSession(session, request, response,HttpServletResponse.SC_UNAUTHORIZED);
			// return false;
		}
		return true;
	}

	private boolean isIPAddressValid(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
		boolean valid = true;
		try {
			String ipAddress = Util.getIpAddress(request);
			if (Util.isNotNull(ipAddress)) {
				if (blockedIpAddresses == null) {
					synchronized (this.getClass()) {
						if (blockedIpAddresses == null) {
							blockedIpAddresses = new HashSet<>();
							BlockedIPAddressService blockedIPAddressService = BlockedIPAddressService.newServiceStatic(BlockedIPAddressService.class, BlockedIPAddress.class, false);
							List<BlockedIPAddress> blockedIPAddresses = blockedIPAddressService.list(true);
							blockedIpAddresses.addAll(blockedIPAddresses.stream().map(BlockedIPAddress::getIpAddress).collect(Collectors.toSet()));
						}
					}
				}

				// full IP address
				if (blockedIpAddresses.contains(ipAddress)) {
					valid = false;
				}

				// subnet
				if (ipAddress.indexOf('.') != -1) {
					String subnet = ipAddress.substring(0, ipAddress.lastIndexOf('.')) + ".*";
					if (blockedIpAddresses.contains(subnet)) {
						valid = false;
					}
				}
			}
		} catch (Exception ex) {
			logger.warn("Cannot validate IP Address: " + ex);
		}
		return valid;
	}

	private void invalidateSession(HttpSession session, HttpServletRequest request, HttpServletResponse response, int httpCode) {
		try {
			request.logout();
		} catch (Exception ex) {
			// ignore
		}
		try {
			if (session != null) {
				session.invalidate();
			}
		} catch (Exception ex) {
			// ignore
		}
		try {
			response.sendError(httpCode, "");
		} catch (Exception ex) {
			// ignore
		}
	}
}
