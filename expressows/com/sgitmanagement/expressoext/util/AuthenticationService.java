package com.sgitmanagement.expressoext.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.RandomStringUtils;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.exception.BaseException;
import com.sgitmanagement.expresso.exception.InvalidCredentialsException;
import com.sgitmanagement.expresso.exception.PasswordExpirationException;
import com.sgitmanagement.expresso.exception.ValidationException;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.mail.Mailer;
import com.sgitmanagement.expressoext.base.BaseService;
import com.sgitmanagement.expressoext.security.AuthorizationHelper;
import com.sgitmanagement.expressoext.security.BasicUser;
import com.sgitmanagement.expressoext.security.BasicUserService;
import com.sgitmanagement.expressoext.security.User;
import com.sgitmanagement.expressoext.security.UserService;

import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class AuthenticationService extends BaseService {
	private static final Map<String, Object> keyLocks = new ConcurrentHashMap<>();

	/**
	 * 
	 * @throws Exception
	 */
	public void login(User user) throws Exception {
		UserService userService = newService(UserService.class, User.class);

		String key = user.getUserName();
		synchronized (keyLocks.computeIfAbsent(key, k -> k)) {
			// validate the password expiration date
			if (user.getPasswordExpirationDate() != null && user.getPasswordExpirationDate().before(new Date())) {
				// CANNOT invalidateSession();
				throw new PasswordExpirationException();
			}

			// is the user is terminated, do not authorize login
			if (user.getTerminationDate() != null) {
				invalidateSession();
				throw new BaseException(HttpServletResponse.SC_UNAUTHORIZED, "userTerminated");
			}

			if (user.getNbrFailedAttempts() > 3) {
				userService.blockAccount(user);
				commit();
				invalidateSession();
				throw new BaseException(423, "accountBlocked");
			}

			// record the last visit date
			user.setLastVisitDate(new Date());
			user.setNbrFailedAttempts(0);
			commit();
		}
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void logout() throws Exception {
		invalidateSession();
		throw new InvalidCredentialsException("Logout");
	}

	/**
	 * 
	 * @throws Exception
	 */
	private void invalidateSession() throws Exception {
		// Invalidate current HTTP session.
		// Will call JAAS LoginModule logout() method
		HttpSession session = getRequest().getSession(false);
		if (session != null) {
			session.invalidate();
		}
		getRequest().logout();
	}

	/**
	 * 
	 * @param user
	 * @param securityTokenNo
	 * @throws Exception
	 */
	public void validateSecurityToken(User user, String securityTokenNo, boolean deleteWhenValid) throws Exception {
		SecurityTokenService securityTokenService = newService(SecurityTokenService.class, SecurityToken.class);
		SecurityToken securityToken = securityTokenService.get(user.getUserName(), securityTokenNo);
		if (securityToken != null) {
			if (deleteWhenValid) {
				securityTokenService.delete(securityToken.getId());
			}
		} else {
			throw new BaseException(HttpServletResponse.SC_UNAUTHORIZED, "invalidSecurityToken");
		}
	}

	/**
	 * 
	 * @param userName
	 * @throws Exception
	 */
	public void resetPassword(String userName) throws Exception {
		try {
			UserService userService = newService(UserService.class, User.class);
			Filter filter = new Filter("userName", userName);

			User user = userService.get(filter);
			if (!user.isLocalAccount()) {
				// cannot change password if it is not a local account
				throw new ValidationException("notLocalAccount");
			}

			if (user.getTerminationDate() != null) {
				// cannot change password if the account is blocked
				throw new ValidationException("accountBlocked");
			}

			if (user.isGenericAccount()) {
				// cannot change password if it is not a local account
				throw new ValidationException("cannotResetGenericAccountPassword");
			}

			sendForgetPasswordTokenMail(user);

		} catch (NoResultException e) {
			// ignore. do not send message to the user
			// throw new ValidationException("invalidUsername");
		} catch (ValidationException e) {
			// ignore. do not send message to the user
			logger.warn("Cannot reset password: " + e);
		}
	}

	/**
	 * 
	 * @param userName
	 * @param newPassword
	 * @param securityTokenNo
	 * @return
	 * @throws Exception
	 */
	public void setNewPassword(String userName, String newPassword, String securityTokenNo) throws Exception {
		UserService userService = newService(UserService.class, User.class);
		try {
			User user;
			if (userName != null && securityTokenNo != null) {
				// reset is done with a security token by email
				user = userService.get(new Filter("userName", userName));

				// first, make sure the token is valid
				try {
					// then delete the token
					validateSecurityToken(user, securityTokenNo, true);
				} catch (Exception ex) {
					throw new BaseException(424, "invalidSecurityToken");// Failed Dependency
				}
			} else {
				// reset is done because the password is expired
				user = getUser().getExtended();
				if (user.isGenericAccount() || !user.isLocalAccount() || user.getUserName().equals(AuthorizationHelper.PUBLIC_USERNAME)
						|| user.getUserName().equals(AuthorizationHelper.SYSTEM_USERNAME)) {
					throw new InvalidCredentialsException("invalidUser");
				}
			}

			// if no exception, set the new password (we must use the user to invoke the service, otherwise any user
			// can update any password)
			newService(UserService.class, User.class, newService(BasicUserService.class, BasicUser.class).get(user.getId())).setNewPassword(user, newPassword, true);
		} catch (NoResultException ex) {
			throw new InvalidCredentialsException("invalidUserName");
		}
	}

	/**
	 * Send an email with a link to the web site (the link include the security token)
	 * 
	 * @param user
	 * @throws Exception
	 */
	public void sendForgetPasswordTokenMail(User user) throws Exception {
		SecurityTokenService securityTokenService = newService(SecurityTokenService.class, SecurityToken.class);
		SecurityToken securityToken = securityTokenService.createNew(user);
		Map<String, String> params = new HashMap<>();
		params.put("fullName", user.getFullName());
		params.put("url", SystemEnv.INSTANCE.getDefaultProperties().getProperty("base_url") + "?securityToken=" + securityToken.getSecurityTokenNo() + "&userName=" + user.getUserName());

		Mailer.INSTANCE.sendMail(user.getEmail(), "user-token-password", params);
	}

	/**
	 * Send an email with a security token
	 * 
	 * @param userName
	 * @throws Exception
	 */
	public void sendEmailTokenMail(User user) throws Exception {
		SecurityTokenService securityTokenService = newService(SecurityTokenService.class, SecurityToken.class);
		String securityTokenNo = RandomStringUtils.random(6, "0123456789");
		securityTokenService.createNew(user, securityTokenNo);
		Map<String, String> params = new HashMap<>();
		params.put("securityTokenNo", securityTokenNo);

		Mailer.INSTANCE.sendMail(user.getEmail(), "user-email-token", params);
	}

	/**
	 * Switch user (use by Maintenance Schedule Viewer to switch to the supervisor on duty)
	 * 
	 * @param userName
	 * @param securityTokenNo
	 */
	public void switchUser(String userName, String securityTokenNo) throws Exception {
		if (userName != null) {
			UserService userService = newService(UserService.class, User.class);
			User user = userService.get(new Filter("userName", userName));

			// first, make sure the token is valid
			try {
				// then delete the token
				validateSecurityToken(user, securityTokenNo, true);
			} catch (Exception ex) {
				throw new BaseException(424, "invalidSecurityToken");// Failed Dependency
			}

			// at this point, both the security token and the userName are validated
			// switch the user from the session
			logger.info("Switching session user from [" + getUser().getUserName() + "] to [" + userName + "]");
			getRequest().getSession().setAttribute("originalUserName", getUser().getUserName());
			getRequest().getSession().setAttribute("userName", userName);
		} else {
			// back to the original user
			if (getRequest().getSession().getAttribute("originalUserName") != null) {
				getRequest().getSession().setAttribute("userName", getRequest().getSession().getAttribute("originalUserName"));
			}
		}
	}

}
