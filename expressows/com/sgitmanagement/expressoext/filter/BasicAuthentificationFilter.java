package com.sgitmanagement.expressoext.filter;

import java.io.IOException;
import java.security.Principal;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.PersistenceManager;
import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.util.ServerTimingUtil;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.security.User;
import com.sgitmanagement.expressoext.security.UserService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class BasicAuthentificationFilter implements Filter {
	final private static Logger logger = LoggerFactory.getLogger(BasicAuthentificationFilter.class);

	private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
	private static final String WWW_AUTHORIZATION = "Authorization";
	private static final String BASIC_PREFIX = "Basic ";

	@Override
	public void init(FilterConfig config) throws ServletException {
		logger.info("BasicAuthentificationFilter init");
	}

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) req;
		HttpServletResponse httpServletResponse = (HttpServletResponse) resp;

		ServerTimingUtil.startTiming("BasicAuthentificationFilter");

		// do not create a session if not
		HttpSession httpSession = httpServletRequest.getSession(false);

		// if there is no valid session, authenticate the user again
		if (httpSession != null && httpSession.getAttribute(WWW_AUTHORIZATION) != null) {
			// logger.debug("Session is active [" + httpSession.getId() + "]");
			chain.doFilter(httpServletRequest, httpServletResponse);
		} else {
			String authUser = null;

			// get username and password from the Authorization header
			String authHeader = httpServletRequest.getHeader(WWW_AUTHORIZATION);
			// logger.debug("authHeader [" + authHeader + "]");

			if (authHeader == null || !authHeader.startsWith(BASIC_PREFIX)) {
				// this will prompt the basic auth dialog on the browser
				// setBasicAuthRequired(response);
				setBasicAuthFailed(httpServletResponse);
			} else {
				try {
					String userPassBase64 = authHeader.substring(BASIC_PREFIX.length());
					String userPassDecoded = new String(Base64.decodeBase64(userPassBase64));
					// logger.debug("userPassDecoded [" + userPassDecoded + "]");

					// Finally userPassDecoded must contain readable "username:password"
					if (!userPassDecoded.contains(":")) {
						setBasicAuthRequired(httpServletResponse);
					} else {
						authUser = userPassDecoded.substring(0, userPassDecoded.indexOf(':'));
						String authPass = Util.nullifyIfNeeded(userPassDecoded.substring(userPassDecoded.indexOf(':') + 1));

						// of the authPass is null, verify if it is an autologin user
						if (authPass == null) {
							// only on internal network
							if (Util.isInternalIpAddress(Util.getIpAddress(httpServletRequest))) {
								authPass = SystemEnv.INSTANCE.getDefaultProperties().getProperty("autologinuser_" + authUser);
								// logger.debug("Got password for [" + authUser + "]: [" + authPass + "]");
							}
						}

						EntityManager em = PersistenceManager.getInstance().getEntityManager();
						try {
							UserService userService = UserService.newServiceStatic(UserService.class, User.class);
							User user = userService.get(new Query.Filter("userName", authUser));

							// verify password
							String hashedPassword = Util.hashPassword(authPass);
							if (user.getPassword() != null && user.getPassword().equals(hashedPassword)) {
								logger.info("Authenticated [" + authUser + "] from IP [" + Util.getIpAddress(httpServletRequest) + "] URL[" + httpServletRequest.getRequestURI() + "] Query["
										+ httpServletRequest.getQueryString() + "]");

								// we must create a session (to store the authorization)
								httpSession = httpServletRequest.getSession(true);
								httpSession.setAttribute(WWW_AUTHORIZATION, authHeader);

							} else {
								logger.warn("Authentication failed for [" + authUser + "] from IP [" + Util.getIpAddress(httpServletRequest) + "]");

								// if there is no local password, there is no risk
								if (user.isLocalAccount() && user.getPassword() != null && !user.isGenericAccount()) {
									// logger.info("Found the user [" + authUser + "]:" + user.getNbrFailedAttempts());
									user.setNbrFailedAttempts(user.getNbrFailedAttempts() + 1);
								}
								setBasicAuthFailed(httpServletResponse);
							}
						} catch (NoResultException ex1) {
							// not a valid username
							setBasicAuthFailed(httpServletResponse);
						} finally {
							// close the connection
							PersistenceManager.getInstance().commitAndClose(em);
						}
					}
				} catch (Exception e) {
					setBasicAuthFailed(httpServletResponse);
				}

				if (httpSession != null && httpSession.getAttribute(WWW_AUTHORIZATION) != null) {
					chain.doFilter(new ExpressoHttpServletRequestWrapper(authUser, httpServletRequest), httpServletResponse);
				}
			}
		}
		ServerTimingUtil.endTiming();
	}

	/**
	 * This method will trigger a challenge to the browser. By default, the browser will display a dialog to enter username and password
	 *
	 * @param response
	 */
	private void setBasicAuthRequired(HttpServletResponse response) {
		response.setHeader(WWW_AUTHENTICATE_HEADER, "Basic realm=\"Local\"");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	}

	/**
	 * This method will send an authorization failed to the browser (no Basic Auth window displayed)
	 *
	 * @param response
	 */
	private void setBasicAuthFailed(HttpServletResponse response) {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	}

	/**
	 *
	 *
	 */
	public class ExpressoHttpServletRequestWrapper extends HttpServletRequestWrapper {
		private String userName;

		public ExpressoHttpServletRequestWrapper(String userName, HttpServletRequest request) {
			super(request);
			this.userName = userName;
		}

		@Override
		public boolean isUserInRole(String role) {
			return false;
		}

		@Override
		public Principal getUserPrincipal() {
			return new Principal() {
				@Override
				public String getName() {
					return userName;
				}
			};
		}
	}
}
