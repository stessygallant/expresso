package com.sgitmanagement.expressoext.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.PersistenceManager;
import com.sgitmanagement.expresso.base.UserManager;
import com.sgitmanagement.expresso.exception.BaseException;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.security.AuthorizationHelper;
import com.sgitmanagement.expressoext.security.User;
import com.sgitmanagement.expressoext.util.AuthenticationService;

import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class EmailAuthentificationFilter implements Filter {
	final private static Logger logger = LoggerFactory.getLogger(EmailAuthentificationFilter.class);

	private static final String EMAIL_TOKEN_VALIDATED = "emailTokenValidated";
	private static final String BYPASS_EMAIL_TOKEN_ROLE = "bypassEmailAuthentication";

	@Override
	public void init(FilterConfig config) throws ServletException {
		logger.info("EmailAuthentificationFilter init");
	}

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		HttpSession session = request.getSession();
		EntityManager entityManager = PersistenceManager.getInstance().getEntityManager();

		if (session.getAttribute(EMAIL_TOKEN_VALIDATED) != null) {
			chain.doFilter(request, response);
		} else {
			boolean valid = false;
			try {
				AuthenticationService authenticationService = AuthenticationService.newServiceStatic(AuthenticationService.class);

				// get the user
				User user = (User) UserManager.getInstance().getUser();
				String action = Util.getParameterValue(request, "action");

				// bypass if the user has the role
				if (AuthorizationHelper.isUserInRole(user, BYPASS_EMAIL_TOKEN_ROLE, false)) {
					session.setAttribute(EMAIL_TOKEN_VALIDATED, true);
					valid = true;
				} else if (action != null && action.equals("logout")) {
					// let logout passed
					valid = true;
				} else {
					String emailToken = request.getParameter("emailToken");
					if (emailToken != null) {
						// validate the token
						authenticationService.validateSecurityToken(user, emailToken.trim());
						session.setAttribute(EMAIL_TOKEN_VALIDATED, true);
						valid = true;
					} else {
						// send an email with a token
						PersistenceManager.getInstance().startTransaction(entityManager);
						authenticationService.sendEmailTokenMail(user);
						response.setStatus(424); // Failed Dependency
					}
				}

			} catch (BaseException ex) {
				logger.debug("" + ex);
				response.setStatus(ex.getCode());
			} catch (Exception ex) {
				logger.error("Got unexpected exception", ex);
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}

			if (valid) {
				chain.doFilter(request, response);
			}
		}
	}

}
