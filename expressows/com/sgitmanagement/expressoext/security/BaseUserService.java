package com.sgitmanagement.expressoext.security;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;

import com.sgitmanagement.expresso.base.IEntity;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Logic;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expresso.exception.ValidationException;
import com.sgitmanagement.expresso.util.DateUtil;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expresso.util.mail.Mailer;
import com.sgitmanagement.expressoext.util.AuthenticationService;

import jakarta.persistence.NoResultException;

public class BaseUserService<U extends User> extends BasePersonService<U> {
	static public enum PasswordRule {
		weak, strong, strong15, secure
	}

	@Override
	public U create(U user) throws Exception {

		user.setCreationDate(new Date()); // person
		user.setUserCreationDate(new Date()); // user
		user.setCreationUserId(getUser().getId());

		if (user.getPersonId() != null) {
			// make sure there is no user already existing for this id
			U u = get(user.getPersonId());
			if (u != null) {
				// throw new ValidationException("userAlreadyExist");
				// return the user but do not overwrite anything
				logger.debug("User [" + u.getFullLabel() + "] already exists");
				return u;
			} else {
				PersonService personService = newService(PersonService.class, Person.class);
				Person person = personService.get(user.getPersonId());

				// set the username if needed
				String userName = verifyUserName(person, user.getUserName());

				// create the user from the person
				U newUser = create(person, userName);
				newUser.setUserCreationDate(new Date());

				// overwrite some fields for the person
				newUser.setJobTitleId(user.getJobTitleId());
				newUser.setDepartmentId(user.getDepartmentId());
				newUser.setCompanyId(user.getCompanyId());
				newUser.setManagerPersonId(user.getManagerPersonId());
				newUser.setPhoneNumber(user.getPhoneNumber());
				newUser.setEmail(user.getEmail());

				// set new fields
				newUser.setExtKey(user.getExtKey());
				newUser.setLanguage(user.getLanguage());
				newUser.setNote(user.getNote());
				newUser.setLocalAccount(user.isLocalAccount());

				user = newUser;
			}
		} else {
			// set the username if needed
			user.setUserName(verifyUserName(user, user.getUserName()));

			user = super.create(user);

			// make sure that the user does create a user with a job title that it does not managed
			if (!isUserInRole("UserManager.admin")) {
				if (user.getJobTitle() != null && getUser().getJobTitle() != null) {
					if (!getUser().getJobTitle().getManagedJobTitles().contains(user.getJobTitle())) {
						throw new ValidationException("cannotCreateUserWithTitle");
					}
				}
			}
		}

		// always add a U role
		addRole(user.getId(), Role.R.USER.getId());

		// sync role info
		syncRoleInfos(user);

		if (user.getPassword() == null || user.getPassword().length() == 0) {
			if (user.isLocalAccount() && !user.isGenericAccount()) {
				sendWelcomeEmail(user);
			}
		} else {
			setNewPassword(user, user.getPassword(), false);
		}
		return user;
	}

	private String verifyUserName(Person person, String userName) throws Exception {
		if (userName == null) {
			userName = generateUserName(person, "e"); // add a suffix to avoid problem between AD and local username
		} else {
			// make sure the userName is not already taken
			try {
				get(new Filter("userName", userName));
				throw new ValidationException("duplicatedUserName");
			} catch (NoResultException ex) {
				// ok
			}
		}

		return userName;
	}

	/**
	 * Create a user from a person (the person already exists in the database. We cannot use JPA for this as it will try to create the person at the same time
	 *
	 * @param person
	 * @return
	 */
	private U create(Person person, String userName) throws Exception {

		// create the record
		getEntityManager().createNativeQuery("INSERT INTO user (id, username) VALUES (:id, :userName)").setParameter("id", person.getId()).setParameter("userName", userName).executeUpdate();
		getEntityManager().flush();
		// getEntityManager().clear();
		getEntityManager().detach(person);

		// load the new user (which inherits from the Person)
		U user = get(person.getId());

		return user;
	}

	@Override
	public U update(U user) throws Exception {
		U prevUser = get(user.getId());

		// when we update the account, we reset the failed attempts
		user.setNbrFailedAttempts(0);

		if (user.getPassword() == null || user.getPassword().length() == 0) {
			// if the password is not set, set it to the same
			user.setPassword(prevUser.getPassword());
		} else {
			user = setNewPassword(user, user.getPassword(), false);
		}

		// make sure that if a user is not terminated that the person is not deactivated
		if (user.getTerminationDate() == null && user.getDeactivationDate() != null) {
			user.setDeactivationDate(null);
		}

		if (!isUserInRole("UserManager.admin")) {
			// a user cannot update all fields
			// rewrite protected data
			user.setCompanyId(prevUser.getCompanyId());
			user.setDepartmentId(prevUser.getDepartmentId());
			user.setJobTitleId(prevUser.getJobTitleId());
			user.setManagerPersonId(prevUser.getManagerPersonId());
			user.setUserName(prevUser.getUserName());

			user.setLocalAccount(prevUser.isLocalAccount());
			user.setExtKey(prevUser.getExtKey());
			user.setNbrFailedAttempts(prevUser.getNbrFailedAttempts());
			user.setPasswordExpirationDate(prevUser.getPasswordExpirationDate());

			user.setTerminationDate(prevUser.getTerminationDate());
			user.setDeactivationDate(prevUser.getDeactivationDate());
			user.setUserCreationDate(prevUser.getUserCreationDate());
			user.setCreationDate(prevUser.getCreationDate());
			user.setLastVisitDate(prevUser.getLastVisitDate());
		}
		user = super.update(user);

		// sync role info
		syncRoleInfos(user);

		return user;
	}

	@Override
	public U deactivate(U user) throws Exception {
		user.setDeactivationDate(new Date());
		user.setTerminationDate(new Date());
		return super.update(user);
	}

	public void addRole(Integer id, Integer roleId) throws Exception {
		// we need to load the complete object because the mapping is not on the id
		U user = get(id);
		Role role = getEntityManager().find(Role.class, roleId);

		// only admin can add reserved role
		if (role.isSystemRole() && roleId != Role.R.USER.getId() && !isUserAdmin()) {
			throw new ForbiddenException();
		}

		user.getRoles().add(role);

		// add the role info to the user
		addRoleInfo(user, role);
	}

	public void removeRole(Integer id, Integer roleId) throws Exception {
		U user = get(id);
		Role role = getEntityManager().find(Role.class, roleId);

		user.getRoles().remove(role);

		// remove the role info to the user
		removeRoleInfo(user, role);
	}

	public Set<Role> getRoles(Integer id) {
		U user = get(id);
		return user.getRoles();
	}

	public Set<Role> getAllRoles(Integer id) {
		U user = get(id);
		Set<Role> roles = new HashSet<>();

		roles.addAll(user.getRoles());

		if (user.getJobTitle() != null) {
			roles.addAll(user.getJobTitle().getRoles());
		}

		if (user.getDepartment() != null) {
			roles.addAll(user.getDepartment().getRoles());
		}

		if (user.getJobTitle() != null && user.getJobTitle().getJobType() != null) {
			roles.addAll(user.getJobTitle().getJobType().getRoles());
		}

		return roles;
	}

	private String generateUserName(Person person, String suffix) throws Exception {

		// generate base username
		String firstName = person.getFirstName() != null && person.getFirstName().length() > 1 ? person.getFirstName() : " ";
		String lastName = person.getLastName() != null ? person.getLastName() : "";
		String userName = (firstName.substring(0, 1) + lastName).trim();

		// remove any invalid characters and replace accents
		userName = Util.stripAccents(Util.purgeInvalidCharacters(userName)).toLowerCase();

		// now remove any non characters
		userName = userName.replaceAll("[^A-Za-z]", "");

		String availableUsername = userName;
		int increment = 1;
		while (true) {
			try {
				if (suffix != null) {
					availableUsername += "-" + suffix;
				}

				get(new Filter("userName", Operator.eq, availableUsername));

				// we got one, try the next
				availableUsername = userName + increment++;
			} catch (NoResultException e) {
				break;
			}
		}

		return availableUsername;
	}

	public void syncRoleInfos(U user) {
		flushAndRefresh(user);

		// add all missing user infos
		Set<Role> roles = getAllRoles(user.getId());
		for (Role role : roles) {
			for (RoleInfo roleInfo : role.getRoleInfos()) {
				// verify if the user has this role info
				if (user.getUserInfo(roleInfo.getId()) == null) {
					addRoleInfo(user, roleInfo);
				}
			}
		}

		// remove all unlinked user infos
		for (UserInfo userInfo : user.getUserInfos()) {
			// do not take into account new ones
			if (userInfo.getId() != null && userInfo.getRoleInfo() != null && userInfo.getRoleInfo().getRole() != null) {
				if (!roles.contains(userInfo.getRoleInfo().getRole())) {
					// need to remove it
					getEntityManager().remove(userInfo);
				}
			}
		}
	}

	public void addRoleInfo(U user, Role role) {
		for (RoleInfo roleInfo : role.getRoleInfos()) {
			addRoleInfo(user, roleInfo);
		}
	}

	public void addRoleInfo(U user, RoleInfo roleInfo) {
		// when we add a role to a user, we must also add default value for roleInfo
		UserInfo userInfo = new UserInfo(user.getId(), roleInfo.getId());
		switch (roleInfo.getInfoType()) {
		case "text":
			userInfo.setTextValue(roleInfo.getDefaultText());
			break;
		case "number":
			userInfo.setNumberValue(roleInfo.getDefaultNumber());
			break;
		case "date":
			userInfo.setDateValue(roleInfo.getDefaultDate());
			break;
		case "string":
		default:
			userInfo.setStringValue(roleInfo.getDefaultString());
			break;
		}

		user.getUserInfos().add(userInfo);
	}

	public void removeRoleInfo(U user, Role role) {
		// remove all user infos related to the role
		for (UserInfo userInfo : user.getUserInfos()) {
			if (userInfo.getRoleInfo().getRoleId().equals(role.getId())) {
				getEntityManager().remove(userInfo);
			}
		}
	}

	public U setNewPassword(U user, String newPassword, boolean update) throws Exception {
		// validate the password
		if (!user.isLocalAccount() && !isUserAdmin()) {
			// cannot change password if it is not a local account
			throw new ValidationException("notLocalAccount");
		}

		PasswordRule passwordRule = PasswordRule.valueOf(SystemEnv.INSTANCE.getDefaultProperties().getProperty("password_rule", PasswordRule.secure.name()));
		boolean passwordExpiration = Boolean.parseBoolean(SystemEnv.INSTANCE.getDefaultProperties().getProperty("password_expiration", "false"));

		if (isUserAdmin()) {
			// allow to set any password
		} else {
			switch (passwordRule) {
			case weak:
				// always valid
				break;

			case strong:
				if (newPassword.length() < 8) {
					throw new ValidationException("invalidNewStrongPassword");
				}
				break;

			case strong15:
				if (newPassword.length() < 15) {
					throw new ValidationException("invalidNewStrong15Password");
				}
				break;

			case secure:
			default:
				// ^ # start-of-string
				// (?=.*[0-9]) # a digit must occur at least once
				// (?=.*[a-z]) # a lower case letter must occur at least once
				// (?=.*[A-Z]) # an upper case letter must occur at least once
				// (?=.*[!@#$%^&+=]) # a special character must occur at least once
				// // (?=\S+$) # no whitespace allowed in the entire string
				// .{8,} # anything, at least eight places though
				// $ # end-of-string
				String secureValidationRegEx = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&-+=_]).{8,}$";

				if (!newPassword.matches(secureValidationRegEx)) {
					throw new ValidationException("invalidNewSecurePassword");
				}

				break;
			}
		}

		// if successful, set the new password (hash the password)
		user.setPassword(Util.hashPassword(newPassword));
		// logger.debug("Set new password to user [" + user.getUserName() + "] to [" + newPassword + "]");

		// unlock anything in the account
		unlockAccount(user);

		// if the rule is expiration, set an expiration date in 90 days
		if (passwordExpiration) {
			user.setPasswordExpirationDate(DateUtils.addDays(new Date(), 90));
		} else {
			user.setPasswordExpirationDate(null);
		}

		if (update) {
			return super.update(user);
		} else {
			return user;
		}
	}

	private void unlockAccount(U user) throws Exception {
		user.setNbrFailedAttempts(0);
		user.setDeactivationDate(null);
		user.setTerminationDate(null);
		user.setLastVisitDate(new Date());
		if (!user.isLocalAccount()) {
			user.setPasswordExpirationDate(null);
		}
	}

	private void sendMail(U user, String template) throws Exception {
		Map<String, String> params = new HashMap<>();

		params.put("fullName", user.getFullName());
		params.put("firstName", user.getFirstName());
		params.put("lastName", user.getLastName());
		params.put("username", user.getUserName());

		// we need to regenerate a new password
		String password = resetPassword(user);
		params.put("password", password);

		params.put("url", SystemEnv.INSTANCE.getDefaultProperties().getProperty("base_url"));

		Mailer.INSTANCE.sendMail(user.getEmail(), template, params);
	}

	private String resetPassword(U user) throws Exception {
		String password = Util.generateRandomPassword();

		// hash the password
		user.setPassword(Util.hashPassword(password));

		// user need to reset it
		user.setPasswordExpirationDate(DateUtils.addDays(new Date(), -1));

		// send the actual clear password for email
		return password;
	}

	public List<Privilege> getPrivileges(Integer userId) throws Exception {
		return AuthorizationHelper.getPrivileges(get(userId));
	}

	public List<Application> getAllApplications(Integer userId) throws Exception {
		return AuthorizationHelper.getApplications(get(userId));
	}

	@SuppressWarnings("unchecked")
	public List<U> getUsersInRole(Integer roleId) throws Exception {
		// get all users from this role
		return (List<U>) AuthorizationHelper.getUsersInRole(newService(RoleService.class, Role.class).get(roleId).getPgmKey());
	}

	public void sendWelcomeEmail(U user) throws Exception {
		if (user.isLocalAccount() && !user.isGenericAccount()) {
			sendMail(user, "user-welcome");
		}
	}

	public U blockAccount(U user) throws Exception {
		if (user.getDeactivationDate() != null && !user.isGenericAccount()) {
			logger.warn("Account blocked [" + user.getUserName() + "]");
			user.setTerminationDate(new Date());
			user.setNote("Deactivated because too many logins attempts on " + new Date());
			user = super.update(user);
		}
		return user;
	}

	public U unlock(U user) throws Exception {
		// unlock anything in the account
		unlockAccount(user);

		if (user.isLocalAccount() && !user.isGenericAccount()) {
			newService(AuthenticationService.class).sendForgetPasswordTokenMail(user);
		}
		return super.update(user);
	}

	@Override
	protected Filter getActiveOnlyFilter() throws Exception {
		Filter filter = new Filter(Logic.or);
		filter.addFilter(new Filter("terminationDate", null));
		filter.addFilter(new Filter("terminationDate", Operator.gt, new Date()));
		return filter;
	}

	@Override
	public void process(String section) throws Exception {
		List<U> users = list();

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, -4);
		Date monthsAgo = calendar.getTime();
		for (U user : users) {

			if (user.getTerminationDate() == null) {
				// if inactive for more than n months, disable the account
				if ((user.getLastVisitDate() == null && user.getUserCreationDate() != null && user.getUserCreationDate().before(monthsAgo))
						|| (user.getLastVisitDate() != null && user.getLastVisitDate().before(monthsAgo))) {
					if (!AuthorizationHelper.isUserAdmin(user) && !user.isGenericAccount()) {
						logger.info("Disabling account [" + user.getUserName() + "] Created[" + DateUtil.formatDate(user.getCreationDate()) + "] Last[" + DateUtil.formatDate(user.getLastVisitDate())
								+ "] Title[" + (user.getJobTitle() != null ? user.getJobTitle().getDescription() : "") + "] Compagnie ["
								+ (user.getCompany() != null ? user.getCompany().getName() : "") + "]");
						user.setTerminationDate(new Date());
					}
				}
			}
		}
	}

	@Override
	protected Filter getSearchFilter(String term) {
		Filter filter = new Filter(Logic.or);
		filter.addFilter(new Filter("userName", Operator.contains, term));
		filter.addFilter(new Filter("fullName", Operator.contains, term));
		return filter;
	}

	/**
	 * Try to get a user from a string
	 *
	 * @param userString
	 * @return
	 * @throws Exception
	 */
	public U parseUser(String userString) throws Exception {
		userString = userString != null ? (userString.trim().length() == 0 ? null : userString.trim()) : null;
		U user = null;

		if (userString != null) {
			try {
				if (userString == null || userString.equals("")) {
					logger.warn("userString is empty");
				} else if (userString.contains(" ") && !userString.contains(",")) {

					// search by first name and last name
					String[] name = userString.split(" ");

					Filter filter = new Filter();
					filter.addFilter(new Filter("firstName", name[0].trim()));
					filter.addFilter(new Filter("lastName", name[1].trim()));
					user = this.get(filter);
				} else if (userString.contains(",")) {

					String[] name = userString.split(",");

					Filter filter = new Filter();
					filter.addFilter(new Filter("firstName", name[1].trim()));
					filter.addFilter(new Filter("lastName", name[0].trim()));

					user = this.get(filter);
				} else {
					try {
						// search by username
						user = this.get(new Filter("userName", Operator.eq, userString));
					} catch (NoResultException e) {
						try {
							// search by contains ext_key
							user = this.get(new Filter("extKey", Operator.eq, userString));
						} catch (NoResultException e2) {
							// search by username
							user = this.get(new Filter("userName", Operator.contains, userString));
						}
					}
				}
			} catch (Exception e) {
				logger.debug("Could not parse [" + userString + "]: " + e);
			}
		}
		return user;
	}

	private boolean canUpdateUser(U user) {
		boolean allowed = false;
		if (user != null) {
			// a standard user can modify itself
			if (getUser().getId().equals(user.getId()) && !user.isGenericAccount()) {
				allowed = true;
				// logger.debug("Allow standard user");
			}

			// only admin can modify an admin user
			else if (AuthorizationHelper.isUserInRole(user, "admin")) {
				if (isUserInRole("admin")) {
					allowed = true;
					// logger.debug("Allow dmin user");
				}
			}

			// only admin can modify generic account
			else if (user.isGenericAccount()) {
				if (isUserInRole("UserManager.admin")) {
					allowed = true;
					// logger.debug("Allowed UserManager.admin user on generic account");
				}
			}

			// user admin can manage any other user
			else if (isUserInRole("UserManager.admin")) {
				allowed = true;
				// logger.debug("Allowed UserManager.admin");
			}

			// user manager can manage only user in their managed job title
			else if (isUserInRole("UserManager.user")) {
				if (user.getJobTitle() != null && getUser().getJobTitle() != null) {
					if (getUser().getJobTitle().getManagedJobTitles().contains(user.getJobTitle())) {
						allowed = true;
						// logger.debug("Allowed managed user");
					}
				}
			}
		}
		return allowed;
	}

	@Override
	public void verifyActionRestrictions(String action, U user) {
		boolean allowed = false;
		switch (action) {

		case "send": // welcome
			if (user != null && canUpdateUser(user) && user.isLocalAccount() && !user.isGenericAccount()) {
				allowed = true;
			}
			break;

		case "duplicate":
		case "update":
		case "delete":
			allowed = canUpdateUser(user);
			break;

		case "deactivate":
			allowed = canUpdateUser(user) && user != null && (user.getDeactivationDate() == null || user.getTerminationDate() == null);
			break;

		case "unlock":
			if (canUpdateUser(user) && !user.isGenericAccount() /* && user.isLocalAccount() */) {
				// (user.getTerminationDate() != null || user.getNbrFailedAttempts() > 0
				// || user.getDeactivationDate() != null)
				allowed = true;
			}
			break;
		}

		if (!allowed) {
			throw new ForbiddenException();
		}
	}

	@Override
	public void verifyCreationRestrictions(U user, IEntity<?> parentEntity) throws Exception {
		super.verifyCreationRestrictions(user, parentEntity);

		if (isUserInRole("UserManager.admin")) {
			// ok
		} else {
			// only if its role manage other role
			if (getUser().getJobTitle() != null && getUser().getJobTitle().getManagedJobTitles() != null) {
				// ok
			} else {
				throw new ForbiddenException();
			}
		}
	}
}
