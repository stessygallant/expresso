package com.sgitmanagement.expressoext.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.IUser;
import com.sgitmanagement.expresso.base.PersistenceManager;
import com.sgitmanagement.expresso.util.SystemEnv;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@SuppressWarnings({ "unchecked", "rawtypes" })
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
	static public final String ADMIN_ROLE = "admin";

	private static final Cache<Integer, Set<Privilege>> userPrivilegeCache;
	private static final Cache<Integer, Set<String>> userRoleCache;
	private static final Cache<Integer, Map<String, Set<String>>> userResourceCache;
	private static final Duration CACHE_DURATION = SystemEnv.INSTANCE.isInProduction() ? Duration.TWENTY_MINUTES : Duration.ONE_MINUTE;
	private static final boolean USE_CACHE = !SystemEnv.INSTANCE.isInProduction();

	static {
		// Create cache
		CachingProvider provider = Caching.getCachingProvider();
		CacheManager cacheManager = provider.getCacheManager();
		userPrivilegeCache = (Cache<Integer, Set<Privilege>>) (Object) cacheManager.createCache("userPrivilegeCache",
				new MutableConfiguration<Integer, Set>().setTypes(Integer.class, Set.class).setStoreByValue(false).setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(CACHE_DURATION)));
		userResourceCache = (Cache<Integer, Map<String, Set<String>>>) (Object) cacheManager.createCache("userResourceCache",
				new MutableConfiguration<Integer, Map>().setTypes(Integer.class, Map.class).setStoreByValue(false).setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(CACHE_DURATION)));
		userRoleCache = (Cache<Integer, Set<String>>) (Object) cacheManager.createCache("userRoleCache",
				new MutableConfiguration<Integer, Set>().setTypes(Integer.class, Set.class).setStoreByValue(false).setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(CACHE_DURATION)));
	}

	public static void clearCache() {
		logger.info("Clearing authorization cache");
		userPrivilegeCache.clear();
		userResourceCache.clear();
		userRoleCache.clear();
	}

	public static BasicUser getPublicUser() {
		return getUser(PUBLIC_USERNAME);
	}

	public static BasicUser getSystemUser() {
		return getUser(SYSTEM_USERNAME);
	}

	public static BasicUser getUser(String userName) {
		return getDefaultEntityManager().createQuery("SELECT s FROM BasicUser s WHERE s.userName = :userName", BasicUser.class).setParameter("userName", userName.toLowerCase()).getSingleResult();
	}

	public static boolean isUserInRole(IUser user, String rolePgmKey) {
		return isUserInRole(user, rolePgmKey, true);
	}

	public static boolean isUserInRole(IUser user, String rolePgmKey, boolean includeAdmin) {
		if (USE_CACHE) {
			if (user != null) {
				Set<String> roles = userRoleCache.get(user.getId());
				if (roles == null) {
					synchronized (userRoleCache) {
						roles = userRoleCache.get(user.getId());
						if (roles == null) {
							roles = new HashSet<>();
							String query = "SELECT r.pgm_key " + ROLE_QUERY + "WHERE u.id = :userId";
							EntityManager em = getDefaultEntityManager();
							Query q = em.createNativeQuery(query).setParameter("userId", user.getId());
							for (String role : (List<String>) q.getResultList()) {
								roles.add(role);
							}
							logger.info("Adding authorization cache for roles[" + user.getUserName() + "]");
							userRoleCache.put(user.getId(), roles);
						}
					}
				}

				// assume admin user in all roles
				return roles.contains(rolePgmKey) || (includeAdmin && roles.contains(ADMIN_ROLE));
			} else {
				return false;
			}
		} else {
			// assume admin user in all roles
			String query = "SELECT COUNT(u.id) " + ROLE_QUERY + "WHERE u.id = :userId AND (r.pgm_key = :role" + (includeAdmin ? " OR r.pgm_key = 'admin'" : "") + ")";

			EntityManager em = getDefaultEntityManager();
			Query q = em.createNativeQuery(query).setParameter("userId", user.getId()).setParameter("role", rolePgmKey);
			return (((Number) q.getSingleResult()).intValue() > 0);
		}

	}

	public static boolean isUserAdmin(IUser user) {
		return isUserInRole(user, ADMIN_ROLE);
	}

	public static List<BasicUser> getUsersInRole(String rolePgmKey) {
		String query = "SELECT DISTINCT u.id " + ROLE_QUERY + " INNER JOIN person pr ON pr.id = u.id " + "WHERE r.pgm_key = :role AND pr.deactivation_date IS NULL AND u.termination_date IS NULL";

		EntityManager em = getDefaultEntityManager();
		Query q = em.createNativeQuery(query).setParameter("role", rolePgmKey).setParameter("userId", null);
		List<Integer> userIds = q.getResultList();
		if (userIds.size() > 0) {
			return em.createQuery("SELECT s FROM BasicUser s WHERE s.id IN :userIds", BasicUser.class).setParameter("userIds", userIds).getResultList();
		} else {
			return new ArrayList<>();
		}
	}

	public static Set<Privilege> getPrivileges(IUser user) throws Exception {
		Set<Privilege> userPrivileges = null;

		if (USE_CACHE) {
			if (user != null) {
				userPrivileges = userPrivilegeCache.get(user.getId());
				if (userPrivileges == null) {
					synchronized (userPrivilegeCache) {
						userPrivileges = userPrivilegeCache.get(user.getId());
						if (userPrivileges == null) {
							userPrivileges = new HashSet<>();

							// if order to avoid a few hundreds database calls, we need to load all privileges and remove the
							// one that the user does not have
							List<Privilege> allPrivileges = PrivilegeService.newServiceStatic(PrivilegeService.class, Privilege.class, false).list();

							// create a map with privileges to increase performance for search
							Map<Integer, Privilege> allPrivilegeMap = allPrivileges.stream().collect(Collectors.toMap(Privilege::getId, Function.identity()));

							// get the ids of the privileges that the user has
							String query = "SELECT DISTINCT p.id " + ROLE_QUERY + "INNER JOIN role_privilege rp ON rp.role_id = r.id " + "INNER JOIN privilege p ON p.id = rp.privilege_id "
									+ "WHERE u.id = :userId ";
							EntityManager em = getDefaultEntityManager();
							Query q = em.createNativeQuery(query).setParameter("userId", user.getId());
							List<Integer> userPrivilegeIds = q.getResultList();
							for (Integer id : userPrivilegeIds) {
								userPrivileges.add(allPrivilegeMap.get(id));
							}

							logger.info("Adding authorization cache for privileges[" + user.getUserName() + "]");
							userPrivilegeCache.put(user.getId(), userPrivileges);
						}
					}
				}
			} else {
				userPrivileges = new HashSet<>();
			}
		} else {
			userPrivileges = new HashSet<>();

			// if order to avoid a few hundreds database calls, we need to load all privileges and remove the
			// one that the user does not have
			List<Privilege> allPrivileges = PrivilegeService.newServiceStatic(PrivilegeService.class, Privilege.class, false).list();

			// create a map with privileges to increase performance for search
			Map<Integer, Privilege> allPrivilegeMap = allPrivileges.stream().collect(Collectors.toMap(Privilege::getId, Function.identity()));

			// get the ids of the privileges that the user has
			String query = "SELECT DISTINCT p.id " + ROLE_QUERY + "INNER JOIN role_privilege rp ON rp.role_id = r.id " + "INNER JOIN privilege p ON p.id = rp.privilege_id " + "WHERE u.id = :userId ";
			EntityManager em = getDefaultEntityManager();
			Query q = em.createNativeQuery(query).setParameter("userId", user.getId());
			List<Integer> userPrivilegeIds = q.getResultList();

			for (Integer id : userPrivilegeIds) {
				userPrivileges.add(allPrivilegeMap.get(id));
			}
		}

		return userPrivileges;
	}

	public static List<Application> getApplications(IUser user) {
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
		try {

			// Date startDate = new Date();
			if (USE_CACHE) {
				if (user != null) {
					Map<String, Set<String>> resourceActionMap = userResourceCache.get(user.getId());
					if (resourceActionMap == null) {
						synchronized (userResourceCache) {
							resourceActionMap = userResourceCache.get(user.getId());
							if (resourceActionMap == null) {
								resourceActionMap = new HashMap<>();
								Set<Privilege> privileges = getPrivileges(user);
								for (Privilege privilege : privileges) {
									String resourceSecurityPath = privilege.getResource().getSecurityPath().toLowerCase();
									Set<String> actions = resourceActionMap.get(resourceSecurityPath);
									if (actions == null) {
										actions = new HashSet<>();
										resourceActionMap.put(resourceSecurityPath, actions);
									}
									actions.add(privilege.getAction().getPgmKey());
								}
							}
							logger.info("Adding authorization cache for resources/actions[" + user.getUserName() + "]");
							userResourceCache.put(user.getId(), resourceActionMap);
						}
					}

					String resourceSecurityPath = String.join("/", resources).toLowerCase();
					if (resourceActionMap.containsKey(resourceSecurityPath) && resourceActionMap.get(resourceSecurityPath).contains(action)) {
						allowed = true;
					}
				}
			} else {

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
			}
		} catch (Exception ex) {
			logger.error("Cannot evaluate isUserAllowed", ex);
		}
		// Date endDate = new Date();
		// logger.debug("isUserAllowed time (ms: " + (endDate.getTime() - startDate.getTime()) + ")");
		return allowed;
	}

	final private static EntityManager getDefaultEntityManager() {
		return PersistenceManager.getInstance().getEntityManager(false);
	}

	public static void main(String[] args) throws Exception {
		List<BasicUser> users = AuthorizationHelper.getUsersInRole(ADMIN_ROLE);
		System.out.println(users);

		AuthorizationHelper.getApplications(getSystemUser());

		PersistenceManager.getInstance().commitAndClose();

		System.out.println("Done");
	}
}
