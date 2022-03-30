package com.sgitmanagement.expressoext.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.IUser;
import com.sgitmanagement.expresso.base.PersistenceManager;

public class AuthorizationHelper {
	final private static Logger logger = LoggerFactory.getLogger(AuthorizationHelper.class);

	final private static String ROLE_QUERY = " FROM user u "//
			+ "INNER JOIN ( "//
			+ "	SELECT ur.user_id, ur.role_id "//
			+ "	FROM user_role ur  "//
			+ "	WHERE ur.user_id = :userId OR :userId IS NULL "//
			+ " "//
			+ "	UNION ALL "//
			+ " "//
			+ "	SELECT u.id AS user_id, deptrole.role_id "//
			+ "	FROM user u "//
			+ "	INNER JOIN person p ON p.id = u.id "//
			+ "	INNER JOIN department dept ON dept.id = p.department_id AND dept.deactivation_date IS NULL "//
			+ "	INNER JOIN department_role deptrole ON deptrole.department_id = dept.id "//
			+ "	WHERE u.id = :userId OR :userId IS NULL "//
			+ " "//
			+ "	UNION ALL "//
			+ " "//
			+ "	SELECT u.id AS user_id, jobtitlerole.role_id "//
			+ "	FROM user u "//
			+ "	INNER JOIN person p ON p.id = u.id "//
			+ "	INNER JOIN job_title jobtitle ON jobtitle.id = p.job_title_id AND jobtitle.deactivation_date IS NULL "//
			+ "	INNER JOIN job_title_role jobtitlerole ON jobtitlerole.job_title_id = jobtitle.id "//
			+ "	WHERE u.id = :userId OR :userId IS NULL "//
			+ " "//
			+ "	UNION ALL "//
			+ " "//
			+ "	SELECT u.id AS user_id, jobtyperole.role_id "//
			+ "	FROM user u "//
			+ "	INNER JOIN person p ON p.id = u.id "//
			+ "	INNER JOIN job_title jobtitle ON jobtitle.id = p.job_title_id AND jobtitle.deactivation_date IS NULL "//
			+ "	INNER JOIN job_type jobtype ON jobtype.id = jobtitle.job_type_id AND jobtype.deactivation_date IS NULL "//
			+ "	INNER JOIN job_type_role jobtyperole ON jobtyperole.job_type_id = jobtype.id "//
			+ "	WHERE u.id = :userId OR :userId IS NULL "//
			+ " "//
			+ ") rr ON rr.user_id = u.id  "//
			+ "INNER JOIN role r ON r.deactivation_date IS NULL AND r.id = rr.role_id ";

	static public final String SYSTEM_USERNAME = "system";
	static public final String PUBLIC_USERNAME = "public";

	public static User getPublicUser() {
		return getUser(PUBLIC_USERNAME);
	}

	public static User getSystemUser() {
		return getUser(SYSTEM_USERNAME);
	}

	public static User getUser(String userName) {
		// this will only select the attributes on the user class
		// then it will send request to load the EAGER ManytoOne relation
		return getDefaultEntityManager().createQuery("SELECT s FROM User s WHERE s.userName = :userName", User.class).setParameter("userName", userName.toLowerCase()).getSingleResult();
	}

	@SuppressWarnings("unchecked")
	public static List<User> getUsersInRole(String rolePgmKey) {
		String query = "SELECT DISTINCT u.id " + ROLE_QUERY + " INNER JOIN person pr ON pr.id = u.id " + "WHERE r.pgm_key = :role AND pr.deactivation_date IS NULL AND u.termination_date IS NULL";

		EntityManager em = getDefaultEntityManager();
		Query q = em.createNativeQuery(query).setParameter("role", rolePgmKey).setParameter("userId", null);
		List<Integer> userIds = q.getResultList();
		if (userIds.size() > 0) {
			return em.createQuery("SELECT s FROM User s WHERE s.id IN :userIds", User.class).setParameter("userIds", userIds).getResultList();
		} else {
			return new ArrayList<>();
		}
	}

	public static boolean isUserInRole(User user, String rolePgmKey) {
		return isUserInRole(user, rolePgmKey, true);
	}

	public static boolean isUserInRole(User user, String rolePgmKey, boolean includeAdmin) {
		// assume admin user in all roles
		String query = "SELECT COUNT(u.id) " + ROLE_QUERY + "WHERE u.id = :userId AND (r.pgm_key = :role" + (includeAdmin ? " OR r.pgm_key = 'admin'" : "") + ")";

		EntityManager em = getDefaultEntityManager();
		Query q = em.createNativeQuery(query).setParameter("userId", user.getId()).setParameter("role", rolePgmKey);
		return (((Number) q.getSingleResult()).intValue() > 0);
	}

	public static boolean isUserAdmin(User user) {
		return isUserInRole(user, "admin");
	}

	@SuppressWarnings("unchecked")
	public static List<Privilege> getPrivileges(User user) throws Exception {
		// if order to avoid a few undreds database calls, we need to load all privileges and remove the
		// one that the user does not have
		List<Privilege> allPrivileges = PrivilegeService.newServiceStatic(PrivilegeService.class, Privilege.class).list();

		// create a map with privileges to increase performance for search
		Map<Integer, Privilege> allPrivilegeMap = new HashMap<>();
		for (Privilege p : allPrivileges) {
			allPrivilegeMap.put(p.getId(), p);
		}

		// get the ids of the privileges that the user has
		String query = "SELECT DISTINCT p.id " + ROLE_QUERY + "INNER JOIN role_privilege rp ON rp.role_id = r.id " + "INNER JOIN privilege p ON p.id = rp.privilege_id " + "WHERE u.id = :userId ";
		EntityManager em = getDefaultEntityManager();
		Query q = em.createNativeQuery(query).setParameter("userId", user.getId());
		List<Integer> userPrivilegeIds = q.getResultList();

		List<Privilege> userPrivilegeList = new ArrayList<>();
		for (Integer id : userPrivilegeIds) {
			userPrivilegeList.add(allPrivilegeMap.get(id));
		}

		return userPrivilegeList;
	}

	@SuppressWarnings("unchecked")
	public static List<Application> getApplications(User user) {
		String query = "SELECT app.id " + ROLE_QUERY + "INNER JOIN role_application ra ON ra.role_id = r.id " + "INNER JOIN application app ON app.id = ra.application_id "
				+ "WHERE u.id = :userId AND app.deactivation_date IS NULL";

		EntityManager em = getDefaultEntityManager();
		Query q = em.createNativeQuery(query).setParameter("userId", user.getId());
		List<Integer> appIds = q.getResultList();

		if (appIds.size() > 0) {
			return em.createQuery("select a from Application a where a.id in :appIds", Application.class).setParameter("appIds", appIds).getResultList();
		} else {
			return new ArrayList<>();
		}
	}

	public static boolean isUserAllowed(IUser user, String action, List<String> resources) {
		boolean allowed = false;

		// Date startDate = new Date();
		// logger.debug(user.getUsername() + ": " + action + " ON " + resources);
		String sqlSelect = "SELECT COUNT(p.id) " + ROLE_QUERY + "INNER JOIN role_privilege rp ON rp.role_id = r.id  " + "INNER JOIN privilege p ON p.id = rp.privilege_id "
				+ "INNER JOIN action a ON a.id = p.action_id  " + "INNER JOIN resource res0 ON res0.id = p.resource_id ";

		String sqlWhere = "WHERE u.id = :userId AND a.pgm_key = :action AND res0.path = :res0 ";

		int i;
		for (i = 1; i < resources.size(); i++) {
			sqlSelect += "INNER JOIN resource res" + i + " ON res" + i + ".id = res" + (i - 1) + ".master_resource_id ";
			sqlWhere += "AND res" + i + ".path = :res" + i + " ";
		}

		// the top resource must be the master resource
		sqlWhere += " AND res" + (i - 1) + ".master_resource_id IS NULL ";

		EntityManager em = getDefaultEntityManager();

		try {
			Query q = em.createNativeQuery(sqlSelect + sqlWhere);
			// System.out.println(sqlSelect + sqlWhere + ":" + (user != null ? user.getId() : null) + ":" + action);
			q.setParameter("userId", user != null ? user.getId() : null).setParameter("action", action);

			i = resources.size() - 1;
			for (String resource : resources) {
				q.setParameter("res" + i, resource);
				i--;
			}

			if (((Number) q.getSingleResult()).intValue() > 0) {
				allowed = true;
			}
		} catch (Exception e) {
			logger.error("Cannot evaluate isUserAllowed", e);
		}

		// Date endDate = new Date();
		// logger.debug("isUserAllowed time (ms: " + (endDate.getTime() - startDate.getTime()) + ")");
		return allowed;
	}

	final private static EntityManager getDefaultEntityManager() {
		return PersistenceManager.getInstance().getEntityManager(false);
	}

	public static void main(String[] args) throws Exception {
		List<User> users = AuthorizationHelper.getUsersInRole("admin");
		System.out.println(users);

		AuthorizationHelper.getApplications(getSystemUser());

		PersistenceManager.getInstance().commitAndClose();

		System.out.println("Done");
	}
}
