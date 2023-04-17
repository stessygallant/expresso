package com.sgitmanagement.expressoext.ldap;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.message.SearchScope;

import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expressoext.base.BaseService;
import com.sgitmanagement.expressoext.util.MainUtil;

public class ActiveDirectoryUserService extends BaseService {
	private final static String[] LDAP_ATTRIBUTES = new String[] { "mail", "telephoneNumber", "Department", "Manager", "Title", "Name", "sAMAccountName", "description", "givenName", "sn",
			"displayName", "homeDrive", "homeDirectory", "employeeId", "employeeNumber", "employeeType", "postalCode", "st", "employeeNumber", "streetAddress", "l", "company", "c", "countryCode",
			"extensionAttribute1", "extensionAttribute2", "extensionAttribute3", "extensionAttribute4", "extensionAttribute5", "extensionAttribute6", "extensionAttribute7", "extensionAttribute8",
			"extensionAttribute9", "extensionAttribute10", "extensionAttribute11", "extensionAttribute12", "extensionAttribute13", "extensionAttribute14", "extensionAttribute15", "userPrincipalName",
			"userAccountControl",
			// lastLogon: last login date on THIS controller (not replicated)
			// lastLogonTimestamp: last login date (replicated, but only if more than 14 days)
			// the only reliable strategy to get the real last login: get lastLogon on EACH controller
			"lastLogonTimeStamp" };

	public ActiveDirectoryUser get(String userName) {
		return get(userName, ActiveDirectoryLDAPClient.INSTANCE.getUsersOU());
	}

	public ActiveDirectoryUser get(String userName, String ou) {
		try {
			List<org.apache.directory.api.ldap.model.entry.Entry> entries = ActiveDirectoryLDAPClient.INSTANCE.search(ou, SearchScope.SUBTREE,
					"(&(objectClass=person)(sAMAccountName=" + userName + "))", LDAP_ATTRIBUTES);
			ActiveDirectoryUser activeDirectoryUser = parseEntry(entries.get(0));
			return activeDirectoryUser;
		} catch (Exception e) {
			return null;
		}
	}

	public ActiveDirectoryUser getFromDescription(String description) throws Exception {
		try {
			List<org.apache.directory.api.ldap.model.entry.Entry> entries = ActiveDirectoryLDAPClient.INSTANCE.search(null, SearchScope.SUBTREE,
					"(&(objectClass=person)(description=" + description + "))", LDAP_ATTRIBUTES);
			ActiveDirectoryUser activeDirectoryUser = parseEntry(entries.get(0));
			return activeDirectoryUser;
		} catch (Exception e) {
			return null;
		}
	}

	public ActiveDirectoryUser getFromDN(String dn) throws Exception {
		List<org.apache.directory.api.ldap.model.entry.Entry> entries = ActiveDirectoryLDAPClient.INSTANCE.search(dn, SearchScope.SUBTREE, "(objectClass=person)", LDAP_ATTRIBUTES);
		if (entries.isEmpty()) {
			return null;
		} else {
			ActiveDirectoryUser activeDirectoryUser = parseEntry(entries.get(0));
			return activeDirectoryUser;
		}
	}

	/**
	 * Return the list of ActiveDirectoryUser that match the search "term"
	 * 
	 * @param term
	 * @return
	 * @throws Exception
	 */
	public List<ActiveDirectoryUser> search(Query query, String term) throws Exception {
		List<ActiveDirectoryUser> activeDirectoryUsers = new ArrayList<>();
		String[] ous = null;
		if (query != null && query.getFilter("ou") != null) {
			ous = new String[] { (String) query.getFilter("ou").getValue() };
		} else {
			ous = new String[] { ActiveDirectoryLDAPClient.INSTANCE.getUsersOU() };
		}

		for (String ou : ous) {
			List<org.apache.directory.api.ldap.model.entry.Entry> entries = ActiveDirectoryLDAPClient.INSTANCE.search(ou, SearchScope.SUBTREE, "(&(objectClass=person)(name=*" + term + "*))",
					LDAP_ATTRIBUTES);

			for (org.apache.directory.api.ldap.model.entry.Entry entry : entries) {
				activeDirectoryUsers.add(parseEntry(entry));
			}
		}

		return activeDirectoryUsers;
	}

	/**
	 * Return the list of ActiveDirectoryUser
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<ActiveDirectoryUser> list(Query query) throws Exception {
		List<ActiveDirectoryUser> activeDirectoryUsers = new ArrayList<>();

		if (query != null && query.getFilter("id") != null) {
			ActiveDirectoryUser activeDirectoryUser = getFromDN("" + query.getFilter("id").getValue());
			if (activeDirectoryUser != null) {
				activeDirectoryUsers.add(activeDirectoryUser);
			}
		} else {

			String[] ous = null;
			if (query != null && query.getFilter("ou") != null) {
				ous = new String[] { (String) query.getFilter("ou").getValue() };
			} else {
				ous = new String[] { ActiveDirectoryLDAPClient.INSTANCE.getUsersOU() };
			}

			for (String ou : ous) {
				List<org.apache.directory.api.ldap.model.entry.Entry> entries;

				if (query.activeOnly()) {
					entries = ActiveDirectoryLDAPClient.INSTANCE.search(ou, SearchScope.SUBTREE, "(&(objectClass=person)(!(userAccountControl:1.2.840.113556.1.4.803:=2)))", LDAP_ATTRIBUTES);
				} else {
					entries = ActiveDirectoryLDAPClient.INSTANCE.search(ou, SearchScope.SUBTREE, "(objectClass=person)", LDAP_ATTRIBUTES);
				}

				for (org.apache.directory.api.ldap.model.entry.Entry entry : entries) {
					activeDirectoryUsers.add(parseEntry(entry));
				}
			}
		}

		return activeDirectoryUsers;
	}

	/**
	 * Build an ActiveDirectoryUser from an AD entry
	 * 
	 * @param entry
	 * @return
	 */
	private ActiveDirectoryUser parseEntry(org.apache.directory.api.ldap.model.entry.Entry entry) throws Exception {
		String dn = entry.getDn().getName();

		ActiveDirectoryUser activeDirectoryUser = new ActiveDirectoryUser();
		activeDirectoryUser.setDn(dn);

		if (entry.get("sAMAccountName") != null) {
			activeDirectoryUser.setsAMAccountName(entry.get("sAMAccountName").getString());
		}
		if (entry.get("Name") != null) {
			activeDirectoryUser.setName(entry.get("Name").getString());
		}
		if (entry.get("mail") != null) {
			activeDirectoryUser.setMail(entry.get("mail").getString());
		}
		if (entry.get("telephoneNumber") != null) {
			activeDirectoryUser.setTelephoneNumber(entry.get("telephoneNumber").getString());
		}
		if (entry.get("Department") != null) {
			activeDirectoryUser.setDepartment(entry.get("Department").getString());
		}
		if (entry.get("Manager") != null) {
			activeDirectoryUser.setManager(entry.get("Manager").getString());
		}
		if (entry.get("Title") != null) {
			activeDirectoryUser.setTitle(entry.get("Title").getString());
		}
		if (entry.get("description") != null) {
			activeDirectoryUser.setDescription(entry.get("description").getString());
		}
		if (entry.get("givenName") != null) {
			activeDirectoryUser.setFirstName(entry.get("givenName").getString());
		}
		if (entry.get("displayName") != null) {
			activeDirectoryUser.setDisplayName(entry.get("displayName").getString());
		}
		if (entry.get("sn") != null) {
			activeDirectoryUser.setLastName(entry.get("sn").getString());
		}
		if (entry.get("homeDrive") != null) {
			activeDirectoryUser.setHomeDrive(entry.get("homeDrive").getString());
		}
		if (entry.get("homeDirectory") != null) {
			activeDirectoryUser.setHomeDirectory(entry.get("homeDirectory").getString());
		}
		if (entry.get("employeeId") != null) {
			activeDirectoryUser.setEmployeeId(entry.get("employeeId").getString());
		}
		if (entry.get("employeeNumber") != null) {
			activeDirectoryUser.setEmployeeNumber(entry.get("employeeNumber").getString());
		}
		if (entry.get("employeeType") != null) {
			activeDirectoryUser.setEmployeeType(entry.get("employeeType").getString());
		}
		if (entry.get("postalCode") != null) {
			activeDirectoryUser.setPostalCode(entry.get("postalCode").getString());
		}
		if (entry.get("st") != null) {
			activeDirectoryUser.setState(entry.get("st").getString());
		}
		if (entry.get("streetAddress") != null) {
			activeDirectoryUser.setAddress(entry.get("streetAddress").getString());
		}
		if (entry.get("l") != null) {
			activeDirectoryUser.setCity(entry.get("l").getString());
		}
		if (entry.get("company") != null) {
			activeDirectoryUser.setCompany(entry.get("company").getString());
		}
		if (entry.get("c") != null) {
			activeDirectoryUser.setCountryCode(entry.get("c").getString());
		}
		if (entry.get("countryCode") != null) {
			activeDirectoryUser.setCountryNumber(entry.get("countryCode").getString());
		}
		if (entry.get("co") != null) {
			activeDirectoryUser.setCountry(entry.get("co").getString());
		}
		if (entry.get("lastLogonTimeStamp") != null) {
			String lastLogonString = entry.get("lastLogonTimeStamp").getString();
			// Active Directory stores date/time values as the number of 100-nanosecond intervals that have elapsed since the 0 hour on
			// January 1, 1601 until the date/time that is being stored.
			long lastLogon = (Long.parseLong(lastLogonString) / 10000L) - 11644473600000L;
			Date lastLogonDate = new Date(lastLogon);
			activeDirectoryUser.setLastLogonDate(lastLogonDate);
		}
		if (entry.get("extensionAttribute1") != null) {
			activeDirectoryUser.setExtensionAttribute1(entry.get("extensionAttribute1").getString());
		}

		if (entry.get("extensionAttribute2") != null) {
			activeDirectoryUser.setExtensionAttribute2(entry.get("extensionAttribute2").getString());
		}

		if (entry.get("extensionAttribute3") != null) {
			activeDirectoryUser.setExtensionAttribute3(entry.get("extensionAttribute3").getString());
		}

		if (entry.get("extensionAttribute4") != null) {
			activeDirectoryUser.setExtensionAttribute4(entry.get("extensionAttribute4").getString());
		}

		if (entry.get("extensionAttribute5") != null) {
			activeDirectoryUser.setExtensionAttribute5(entry.get("extensionAttribute5").getString());
		}

		if (entry.get("extensionAttribute6") != null) {
			activeDirectoryUser.setExtensionAttribute6(entry.get("extensionAttribute6").getString());
		}

		if (entry.get("extensionAttribute7") != null) {
			activeDirectoryUser.setExtensionAttribute7(entry.get("extensionAttribute7").getString());
		}

		if (entry.get("extensionAttribute8") != null) {
			activeDirectoryUser.setExtensionAttribute8(entry.get("extensionAttribute8").getString());
		}

		if (entry.get("extensionAttribute9") != null) {
			activeDirectoryUser.setExtensionAttribute9(entry.get("extensionAttribute9").getString());
		}

		if (entry.get("extensionAttribute10") != null) {
			activeDirectoryUser.setExtensionAttribute10(entry.get("extensionAttribute10").getString());
		}

		if (entry.get("extensionAttribute11") != null) {
			activeDirectoryUser.setExtensionAttribute11(entry.get("extensionAttribute11").getString());
		}

		if (entry.get("extensionAttribute12") != null) {
			activeDirectoryUser.setExtensionAttribute12(entry.get("extensionAttribute12").getString());
		}

		if (entry.get("extensionAttribute13") != null) {
			activeDirectoryUser.setExtensionAttribute13(entry.get("extensionAttribute13").getString());
		}

		if (entry.get("extensionAttribute14") != null) {
			activeDirectoryUser.setExtensionAttribute14(entry.get("extensionAttribute14").getString());
		}

		if (entry.get("extensionAttribute15") != null) {
			activeDirectoryUser.setExtensionAttribute15(entry.get("extensionAttribute15").getString());
		}

		if (entry.get("userPrincipalName") != null) {
			activeDirectoryUser.setUserPrincipalName(entry.get("userPrincipalName").getString());
		}
		if (entry.get("userAccountControl") != null) {
			boolean disabled = ((Long.parseLong(entry.get("userAccountControl").getString()) & 2) != 0);
			if (disabled) {
				activeDirectoryUser.setActive(false);
			} else {
				activeDirectoryUser.setActive(true);
			}
		}

		return activeDirectoryUser;

	}

	/**
	 * Get the list of group which the user is member of (starting from the base DN)
	 * 
	 * @param name
	 * @return
	 */
	public Set<String> getGroupMemberOf(String userName) throws Exception {
		return getGroupMemberOf(null, userName);
	}

	/**
	 * Get the list of group which the user is member of (starting from the OU in parameter)
	 * 
	 * @param ou
	 * @param name
	 * @return
	 */
	public Set<String> getGroupMemberOf(String ou, String userName) throws Exception {
		Set<String> groups = new HashSet<>();
		getMemberOfDNs(ou, "(&(objectClass=person)(sAMAccountName=" + userName + "))", true, groups);
		return groups;
	}

	/**
	 * 
	 * @param ou
	 * @param filter
	 * @param recursive
	 * @param groupNames
	 * @throws Exception
	 */
	private void getMemberOfDNs(String ou, String filter, boolean recursive, Set<String> groupNames) throws Exception {
		List<org.apache.directory.api.ldap.model.entry.Entry> entries = ActiveDirectoryLDAPClient.INSTANCE.search(ou, SearchScope.SUBTREE, filter, new String[] { "memberOf" });

		// we must find only 1 entry
		for (org.apache.directory.api.ldap.model.entry.Entry entry : entries) {
			Attribute attr = entry.get("memberOf");
			if (attr != null) {
				for (Value v : attr) {
					String groupMemberOfDn = v.getString();

					// Remove the TEST group in production
					if (!SystemEnv.INSTANCE.isInProduction() || !groupMemberOfDn.toLowerCase().contains("ou=test,")) {
						String groupMemberOfName = groupMemberOfDn.substring(3, groupMemberOfDn.indexOf(",OU"));
						if (groupMemberOfName.startsWith("\\#")) {
							groupMemberOfName = groupMemberOfName.substring(1);
						}

						if (!groupNames.contains(groupMemberOfName)) {
							groupNames.add(groupMemberOfName);
							if (recursive) {
								getMemberOfDNs(groupMemberOfDn, "(objectClass=group)", recursive, groupNames);
							}
						}
					}
				}
			}
		}
	}

	public ActiveDirectoryUser merge(ActiveDirectoryUser activeDirectoryUser) throws Exception {
		// verify if the group already exists in AD
		if (get(activeDirectoryUser.getName()) != null) {
			return update(activeDirectoryUser);
		} else {
			return create(activeDirectoryUser);
		}
	}

	public ActiveDirectoryUser create(ActiveDirectoryUser activeDirectoryUser) throws Exception {
		logger.debug("Adding " + activeDirectoryUser);

		DefaultEntry entryGroup = new DefaultEntry(
				// DN
				activeDirectoryUser.getDn(),
				// ObjectClass
				"ObjectClass: top", "ObjectClass: user",
				// CN
				"cn: " + activeDirectoryUser.getName(),
				// CN
				"sAMAccountName: " + activeDirectoryUser.getsAMAccountName(), "userAccountControl: 544");

		ActiveDirectoryLDAPClient.INSTANCE.addEntry(entryGroup);
		activeDirectoryUser = update(activeDirectoryUser);
		return activeDirectoryUser;
	}

	public ActiveDirectoryUser update(ActiveDirectoryUser activeDirectoryUser) throws Exception {
		logger.debug("Updating " + activeDirectoryUser);

		List<Modification> modifications = new ArrayList<>();

		// first name
		if (activeDirectoryUser.getFirstName() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "givenName", activeDirectoryUser.getFirstName()));
		}

		// last name
		if (activeDirectoryUser.getLastName() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "sn", activeDirectoryUser.getLastName()));
		}

		// password
		if (activeDirectoryUser.getEncodedPassword() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "unicodePwd", activeDirectoryUser.getEncodedPassword()));
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "pwdLastSet", "0"));
		}

		// country
		if (activeDirectoryUser.getCountry() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "co", activeDirectoryUser.getCountry()));
		}

		// country code
		if (activeDirectoryUser.getCountryCode() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "c", activeDirectoryUser.getCountryCode()));
		}

		// country number
		if (activeDirectoryUser.getCountryNumber() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "countryCode", activeDirectoryUser.getCountryNumber()));
		}

		// city
		if (activeDirectoryUser.getCity() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "l", activeDirectoryUser.getCity()));
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "physicalDeliveryOfficeName", activeDirectoryUser.getCity()));
		}

		// address
		if (activeDirectoryUser.getAddress() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "streetAddress", activeDirectoryUser.getAddress()));
		}

		// state
		if (activeDirectoryUser.getState() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "st", activeDirectoryUser.getState()));
		}

		// postal code
		if (activeDirectoryUser.getPostalCode() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "postalCode", activeDirectoryUser.getPostalCode()));
		}

		// description
		if (activeDirectoryUser.getDescription() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "description", activeDirectoryUser.getDescription()));
		}

		// mail
		if (activeDirectoryUser.getMail() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "mail", activeDirectoryUser.getMail()));
		}

		// telephoneNumber
		if (activeDirectoryUser.getTelephoneNumber() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "telephoneNumber", activeDirectoryUser.getTelephoneNumber()));
		}

		// Department
		if (activeDirectoryUser.getDepartment() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "department", activeDirectoryUser.getDepartment()));
		}

		// Manager
		if (activeDirectoryUser.getManager() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "manager", activeDirectoryUser.getManager()));
		}

		// Title
		if (activeDirectoryUser.getTitle() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "title", activeDirectoryUser.getTitle()));
		}

		// Company
		if (activeDirectoryUser.getCompany() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "company", activeDirectoryUser.getCompany()));
		}

		// Display Name
		if (activeDirectoryUser.getDisplayName() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "displayName", activeDirectoryUser.getDisplayName()));
		}

		// userPrincipalName
		if (activeDirectoryUser.getUserPrincipalName() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "userPrincipalName", activeDirectoryUser.getUserPrincipalName()));
		}

		// homeDrive
		if (activeDirectoryUser.getHomeDrive() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "homeDrive", activeDirectoryUser.getHomeDrive()));
		}

		// homeDirectory
		if (activeDirectoryUser.getHomeDirectory() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "homeDirectory", activeDirectoryUser.getHomeDirectory()));
		}

		// employeeId
		if (activeDirectoryUser.getEmployeeId() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "employeeId", activeDirectoryUser.getEmployeeId()));
		}

		// employeeNumber
		if (activeDirectoryUser.getEmployeeNumber() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "employeeNumber", activeDirectoryUser.getEmployeeNumber()));
		}

		// employeeType
		if (activeDirectoryUser.getEmployeeType() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "employeeType", activeDirectoryUser.getEmployeeType()));
		}

		// extensionAttribute 1 to 15
		if (activeDirectoryUser.getExtensionAttribute1() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute1", activeDirectoryUser.getExtensionAttribute1()));
		}

		if (activeDirectoryUser.getExtensionAttribute2() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute2", activeDirectoryUser.getExtensionAttribute2()));
		}

		if (activeDirectoryUser.getExtensionAttribute3() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute3", activeDirectoryUser.getExtensionAttribute3()));
		}

		if (activeDirectoryUser.getExtensionAttribute4() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute4", activeDirectoryUser.getExtensionAttribute4()));
		}

		if (activeDirectoryUser.getExtensionAttribute5() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute5", activeDirectoryUser.getExtensionAttribute5()));
		}

		if (activeDirectoryUser.getExtensionAttribute6() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute6", activeDirectoryUser.getExtensionAttribute6()));
		}

		if (activeDirectoryUser.getExtensionAttribute7() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute7", activeDirectoryUser.getExtensionAttribute7()));
		}

		if (activeDirectoryUser.getExtensionAttribute8() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute8", activeDirectoryUser.getExtensionAttribute8()));
		}

		if (activeDirectoryUser.getExtensionAttribute9() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute9", activeDirectoryUser.getExtensionAttribute9()));
		}

		if (activeDirectoryUser.getExtensionAttribute10() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute10", activeDirectoryUser.getExtensionAttribute10()));
		}

		if (activeDirectoryUser.getExtensionAttribute11() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute11", activeDirectoryUser.getExtensionAttribute11()));
		}

		if (activeDirectoryUser.getExtensionAttribute12() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute12", activeDirectoryUser.getExtensionAttribute12()));
		}

		if (activeDirectoryUser.getExtensionAttribute13() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute13", activeDirectoryUser.getExtensionAttribute13()));
		}

		if (activeDirectoryUser.getExtensionAttribute14() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute14", activeDirectoryUser.getExtensionAttribute1()));
		}

		if (activeDirectoryUser.getExtensionAttribute15() != null) {
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute15", activeDirectoryUser.getExtensionAttribute1()));
		}

		ActiveDirectoryLDAPClient.INSTANCE.updateAttributes(activeDirectoryUser.getDn(), modifications.toArray(new Modification[modifications.size()]));

		return activeDirectoryUser;
	}

	public static void main(String[] args) throws Exception {
		ActiveDirectoryUserService service = newServiceStatic(ActiveDirectoryUserService.class);

		List<ActiveDirectoryUser> activeDirectoryUsers = service.list(new Query().setActiveOnly(true));
		for (ActiveDirectoryUser activeDirectoryUser : activeDirectoryUsers) {
			System.out.println(activeDirectoryUser);
		}

		MainUtil.close();
	}
}
