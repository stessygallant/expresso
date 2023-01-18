package com.sgitmanagement.expressoext.ldap;

import java.util.ArrayList;
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

import com.sgitmanagement.expresso.base.PersistenceManager;
import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expressoext.base.BaseService;

public class ActiveDirectoryGroupService extends BaseService {
	private final static String[] LDAP_ATTRIBUTES = new String[] { "name", "description", "info", "managedBy", "extensionAttribute1", "member" };

	public ActiveDirectoryGroup get(String name) {
		try {
			List<org.apache.directory.api.ldap.model.entry.Entry> entries = ActiveDirectoryLDAPClient.INSTANCE.search(null, SearchScope.SUBTREE, "(&(objectClass=group)(name=" + name + "))",
					LDAP_ATTRIBUTES);
			ActiveDirectoryGroup activeDirectoryGroup = parseEntry(entries.get(0));
			return activeDirectoryGroup;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Get the list members for the group
	 * 
	 * @param groupName
	 * @return
	 */
	public List<ActiveDirectoryUser> getMembers(String groupName) throws Exception {
		List<ActiveDirectoryUser> members = new ArrayList<>();

		ActiveDirectoryUserService activeDirectoryUserService = newService(ActiveDirectoryUserService.class);

		// get the list of members for the group
		List<String> memberDNs = new ArrayList<>();
		getMemberDNs(groupName, true, memberDNs);

		// then for each DN, get the AD user
		for (String memberDN : memberDNs) {
			members.add(activeDirectoryUserService.getFromDN(memberDN));
		}

		return members;
	}

	/**
	 * Get the list members for the group
	 * 
	 * @param name
	 * @return
	 */
	public Set<String> getMemberDNs(String groupName) throws Exception {

		// get the list of members for the group
		List<String> memberDNs = new ArrayList<>();
		getMemberDNs(groupName, true, memberDNs);

		Set<String> memberDNSet = new HashSet<>();
		memberDNSet.addAll(memberDNs);
		return memberDNSet;
	}

	/**
	 * 
	 * @param groupName
	 * @param recursive
	 * @param memberDNs
	 * @throws Exception
	 */
	private void getMemberDNs(String groupName, boolean recursive, List<String> memberDNs) throws Exception {
		getMemberDNs(groupName, recursive, memberDNs, new HashSet<>());
	}

	/**
	 * 
	 * @param groupName
	 * @param recursive
	 * @param memberDNs
	 * @param groupNames
	 * @throws Exception
	 */
	private void getMemberDNs(String groupName, boolean recursive, List<String> memberDNs, Set<String> groupNames) throws Exception {

		// make sure that we have the name
		groupName = getGroupNameFromDn(groupName);
		groupNames.add(groupName);

		List<org.apache.directory.api.ldap.model.entry.Entry> entries = ActiveDirectoryLDAPClient.INSTANCE.search(null, SearchScope.SUBTREE, "(&(objectClass=group)(name=" + groupName + "))",
				new String[] { "member" });

		// we must find only 1 entry
		if (entries != null && entries.size() == 1) {
			org.apache.directory.api.ldap.model.entry.Entry entry = entries.get(0);
			Attribute attr = entry.get("member");
			if (attr != null) {
				for (Value v : attr) {
					String memberDn = v.getString();
					if (recursive && (memberDn.startsWith("CN=G-") || memberDn.startsWith("CN=\\#"))) {
						// get the list of members for this group
						String subGroupName = getGroupNameFromDn(memberDn);

						// do not parse twice the same group
						if (!groupNames.contains(subGroupName)) {
							getMemberDNs(subGroupName, recursive, memberDNs, groupNames);
						}
					} else {
						memberDNs.add(memberDn);
					}
				}
			}
		}
	}

	/**
	 * Return the list of ActiveDirectoryGroup that match the search "term"
	 * 
	 * @param term
	 * @return
	 * @throws Exception
	 */
	public List<ActiveDirectoryGroup> search(Query query, String term) throws Exception {
		List<ActiveDirectoryGroup> activeDirectoryGroups = new ArrayList<>();

		String ou = null;
		if (query.getFilter("ou") != null) {
			ou = (String) query.getFilter("ou").getValue();
		}

		List<org.apache.directory.api.ldap.model.entry.Entry> entries = ActiveDirectoryLDAPClient.INSTANCE.search(ou, SearchScope.SUBTREE,
				(term != null && term.length() > 0 ? "(&(objectClass=group)(name=*" + term + "*))" : "(objectClass=group)"), LDAP_ATTRIBUTES);

		for (org.apache.directory.api.ldap.model.entry.Entry entry : entries) {
			activeDirectoryGroups.add(parseEntry(entry));
		}

		return activeDirectoryGroups;
	}

	/**
	 * Return the list of ActiveDirectoryGroup
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<ActiveDirectoryGroup> list(Query query) throws Exception {
		List<ActiveDirectoryGroup> activeDirectoryGroups = new ArrayList<>();

		String ou = null;
		SearchScope searchScope = SearchScope.SUBTREE;
		String groupFilter = "(&(objectClass=group)";

		// apply filter
		if (query != null && query.getFilter("id") != null) {
			ActiveDirectoryGroup activeDirectoryGroup = get((String) query.getFilter("id").getValue());
			activeDirectoryGroups.add(activeDirectoryGroup);
			return activeDirectoryGroups;
		} else {
			if (query != null && query.getFilter() != null && query.getFilter().getFilters() != null) {

				for (Filter f : query.getFilter().getFilters()) {
					if (f.getField().equals("ou")) {
						ou = (String) f.getValue();
					} else if (f.getField().equals("searchScope")) {
						searchScope = ((String) f.getValue()).equals("subtree") ? SearchScope.SUBTREE : SearchScope.ONELEVEL;
					} else {
						String value = f.getOperator().equals(Filter.Operator.eq) ? "" + f.getValue() : "*" + f.getValue() + "*";
						groupFilter += "(" + f.getField() + "=" + value + ")";
					}
				}
			} else if (query != null && query.getFilter() != null) {
				Filter f = query.getFilter();
				if (f.getField().equals("ou")) {
					ou = (String) f.getValue();
				} else if (f.getField().equals("searchScope")) {
					searchScope = ((String) f.getValue()).equals("subtree") ? SearchScope.SUBTREE : SearchScope.ONELEVEL;
				} else {
					String value = f.getOperator().equals(Filter.Operator.eq) ? "" + f.getValue() : "*" + f.getValue() + "*";
					groupFilter += "(" + f.getField() + "=" + value + ")";
				}
			}

			groupFilter += ")";

			List<org.apache.directory.api.ldap.model.entry.Entry> entries = ActiveDirectoryLDAPClient.INSTANCE.search(ou, searchScope, groupFilter, LDAP_ATTRIBUTES);

			for (org.apache.directory.api.ldap.model.entry.Entry entry : entries) {
				activeDirectoryGroups.add(parseEntry(entry));
			}

			return activeDirectoryGroups;
		}
	}

	public ActiveDirectoryGroup getFromDN(String dn, ActiveDirectoryLDAPClient activeDirectoryLDAPClient) throws Exception {
		List<org.apache.directory.api.ldap.model.entry.Entry> entries = activeDirectoryLDAPClient.search(dn, SearchScope.SUBTREE, "(objectClass=group)", LDAP_ATTRIBUTES);
		ActiveDirectoryGroup activeDirectoryGroup = parseEntry(entries.get(0));
		return activeDirectoryGroup;
	}

	/**
	 * Build an ActiveDirectoryGroup from an AD entry
	 * 
	 * @param entry
	 * @return
	 */
	private ActiveDirectoryGroup parseEntry(org.apache.directory.api.ldap.model.entry.Entry entry) throws Exception {
		String dn = entry.getDn().getName();

		ActiveDirectoryGroup activeDirectoryGroup = new ActiveDirectoryGroup();
		activeDirectoryGroup.setDn(dn);

		String name = entry.get("name").getString();
		if (name.startsWith("CN=\\#")) {
			name = "CN=#" + name.substring("CN=\\#".length());
		}
		activeDirectoryGroup.setName(name);

		if (entry.get("description") != null) {
			activeDirectoryGroup.setDescription(entry.get("description").getString());
		}
		if (entry.get("info") != null) {
			activeDirectoryGroup.setInfo(entry.get("info").getString());
		}
		if (entry.get("managedBy") != null) {
			activeDirectoryGroup.setManagedBy(entry.get("managedBy").getString());
		}
		if (entry.get("extensionAttribute1") != null) {
			activeDirectoryGroup.setExtensionAttribute1(entry.get("extensionAttribute1").getString());
		}

		if (entry.get("member") != null) {
			Attribute attr = entry.get("member");
			Set<String> memberDNs = new HashSet<>();
			for (Value v : attr) {
				String memberDn = v.getString();
				memberDNs.add(memberDn);
			}
			activeDirectoryGroup.setMemberDNs(memberDNs);
		}
		return activeDirectoryGroup;
	}

	private String getGroupNameFromDn(String dn) {
		if (dn != null && dn.startsWith("CN=G-") && dn.indexOf(",OU") != -1) {
			return dn.substring(3, dn.indexOf(",OU"));
		} else if (dn != null && dn.startsWith("CN=\\#") && dn.indexOf(",OU") != -1) {
			return dn.substring(4, dn.indexOf(",OU"));
		} else {
			return dn;
		}
	}

	public boolean userInGroup(String username, String group) throws Exception {
		List<ActiveDirectoryUser> members = getMembers(group);
		for (ActiveDirectoryUser activeDirectoryUser : members) {
			if (activeDirectoryUser.getsAMAccountName().toLowerCase().equals(username.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	public ActiveDirectoryGroup merge(ActiveDirectoryGroup activeDirectoryGroup) throws Exception {
		// verify if the group already exists in AD
		if (get(activeDirectoryGroup.getName()) != null) {
			return update(activeDirectoryGroup);
		} else {
			return create(activeDirectoryGroup);
		}
	}

	public ActiveDirectoryGroup create(ActiveDirectoryGroup activeDirectoryGroup) throws Exception {
		logger.debug("Adding " + activeDirectoryGroup);

		ActiveDirectoryLDAPClient activeDirectoryLDAPClient = ActiveDirectoryLDAPClient.INSTANCE;

		DefaultEntry entryGroup = new DefaultEntry(
				// DN
				activeDirectoryGroup.getDn(),
				// ObjectClass
				"ObjectClass: top", "ObjectClass: group",
				// CN
				"cn: " + activeDirectoryGroup.getName(),
				// sAMAccountName
				"sAMAccountName: " + activeDirectoryGroup.getName(),
				// Attributes
				"name: " + activeDirectoryGroup.getName());

		activeDirectoryLDAPClient.addEntry(entryGroup);

		activeDirectoryGroup = update(activeDirectoryGroup);

		return activeDirectoryGroup;
	}

	/**
	 * Add a member to a group. If the member already exist, a LdapEntryAlreadyExistsException will be thrown
	 * 
	 * @param dn       DN of the group
	 * @param memberDn
	 * @throws Exception
	 */
	public void addMember(String dn, String memberDn) throws Exception {
		// make sure it is a DN
		memberDn = getDn(memberDn);

		// add the member to the group
		ActiveDirectoryLDAPClient.INSTANCE.updateAttributes(dn, new DefaultModification(ModificationOperation.ADD_ATTRIBUTE, "member", memberDn));
	}

	public void setMembers(String dn, Set<String> policyPendingUsernames) throws Exception {

		clearMembers(dn);

		List<String> fullDnList = new ArrayList<>();
		for (String memberDn : policyPendingUsernames) {
			// make sure it is a DN
			fullDnList.add(getDn(memberDn));
		}

		String[] array = fullDnList.toArray(new String[0]);

		// add the member to the group
		ActiveDirectoryLDAPClient.INSTANCE.updateAttributes(dn, new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "member", array));
	}

	/**
	 * Remove a member of a group.
	 * 
	 * @param dn       DN of the group
	 * @param memberDn
	 * @throws Exception
	 */
	public void removeMember(String dn, String memberDn) throws Exception {

		// make sure it is a DN
		dn = getDn(dn);
		memberDn = getDn(memberDn);

		// add the member to the group
		ActiveDirectoryLDAPClient.INSTANCE.updateAttributes(dn, new DefaultModification(ModificationOperation.REMOVE_ATTRIBUTE, "member", memberDn));
	}

	/**
	 * Clear members of a group.
	 * 
	 * @param dn DN of the group
	 * @throws Exception
	 */
	public void clearMembers(String dn) throws Exception {
		dn = getDn(dn);
		ActiveDirectoryLDAPClient.INSTANCE.updateAttributes(dn, new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "member", new String[] {}));
	}

	public ActiveDirectoryGroup update(ActiveDirectoryGroup activeDirectoryGroup) throws Exception {
		logger.debug("Updating " + activeDirectoryGroup);

		List<Modification> modifications = new ArrayList<>();

		// DESCRIPTION
		modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "description", activeDirectoryGroup.getDescription()));

		// MANAGE BY
		modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "managedBy", activeDirectoryGroup.getManagedBy()));

		// INFO
		String info = activeDirectoryGroup.getInfo();
		if (info != null) {
			// make sure that \n is \r\n
			info = info.replaceAll("(\r\n|\n)", "\r\n");
		} else {
			// AD does not support null
			info = "";
		}
		modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "info", info));

		// EXTENSION ATTRIBUTE1
		String extAttribute1 = activeDirectoryGroup.getExtensionAttribute1();
		if (extAttribute1 != null && extAttribute1.length() > 0) {
			// AD does not support null
			modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "extensionAttribute1", extAttribute1));
		}

		ActiveDirectoryLDAPClient.INSTANCE.updateAttributes(activeDirectoryGroup.getDn(), modifications.toArray(new Modification[0]));

		return activeDirectoryGroup;
	}

	/**
	 * Get the DN from a string.
	 * 
	 * @param s could be a group name, a user name or a DN
	 * @return
	 * @throws Exception
	 */
	public String getDn(String s) throws Exception {
		String dn;
		if (s.startsWith("G-") || s.startsWith("#")) {
			// this is a CN for a group
			ActiveDirectoryGroup activeDirectoryGroup = get(s);
			if (activeDirectoryGroup == null) {
				throw new Exception("Group does not exist [" + s + "]");
			}
			dn = activeDirectoryGroup.getDn();
		} else if (s.startsWith("CN=")) {
			// OK, this is a DN
			dn = s;
		} else {
			// the must be a userName
			ActiveDirectoryUser activeDirectoryUser = newService(ActiveDirectoryUserService.class).get(s);
			if (activeDirectoryUser == null) {
				throw new Exception("User does not exist [" + s + "]");
			}
			dn = activeDirectoryUser.getDn();
		}
		return dn;
	}

	/**
	 * Return true if the name is the name of a group
	 * 
	 * @param name
	 * @return
	 */
	public boolean isGroup(String name) {
		return (name.startsWith("G-") || name.startsWith("#") || name.startsWith("CN=G-") || name.startsWith("CN=\\#"));
	}

	public static void main(String[] args) throws Exception {
		ActiveDirectoryGroupService service = newServiceStatic(ActiveDirectoryGroupService.class);

		List<ActiveDirectoryGroup> activeDirectoryGroups = service.list(new Query());
		for (ActiveDirectoryGroup activeDirectoryGroup : activeDirectoryGroups) {
			System.out.println(activeDirectoryGroup);
		}

		ActiveDirectoryLDAPClient.INSTANCE.close();

		PersistenceManager.getInstance().commitAndClose();

		System.out.println("Done");
	}
}
