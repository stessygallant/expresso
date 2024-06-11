package com.sgitmanagement.expressoext.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.directory.api.ldap.codec.controls.search.pagedSearch.PagedResultsFactory;
import org.apache.directory.api.ldap.codec.standalone.StandaloneLdapApiService;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultDone;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.PagedResults;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.util.SystemEnv;

public class ActiveDirectoryLDAPClient {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	static final private int NB_RESULTS_PER_PAGE = 500;

	private LdapConnection ldapConnection;

	public final static ActiveDirectoryLDAPClient INSTANCE = new ActiveDirectoryLDAPClient();

	/**
	 * Private constructor. Only one instance
	 */
	private ActiveDirectoryLDAPClient() {
		super();
	}

	private void reconnect() throws Exception {
		close();
		try {
			this.ldapConnection = getLdapConnection();
		} catch (Exception ex) {
			logger.error("Cannot get a LDAP connection", ex);
			throw ex;
		}
	}

	/**
	 * Get a connection to LDAP
	 * 
	 * @return
	 */
	private LdapConnection getLdapConnection() throws Exception {
		System.setProperty(StandaloneLdapApiService.REQUEST_CONTROLS_LIST, PagedResultsFactory.class.getName());

		LdapConnection ldapConnection = null;
		Properties properties = SystemEnv.INSTANCE.getDefaultProperties();
		String adHost = properties.getProperty("active_directory_host");
		int adPort = Integer.parseInt(properties.getProperty("active_directory_port"));
		String adUser = properties.getProperty("active_directory_user");
		String adPassword = properties.getProperty("active_directory_password");
		boolean ssl = Boolean.parseBoolean(properties.getProperty("active_directory_ssl"));

		ldapConnection = new LdapNetworkConnection(adHost, adPort, ssl);
		ldapConnection.bind(adUser, adPassword);
		return ldapConnection;
	}

	private void closeLdapConnection(LdapConnection ldapConnection) {
		// Cleanup the session
		try {
			ldapConnection.unBind();
		} catch (Exception e) {
		}
		try {
			ldapConnection.close();
		} catch (Exception e) {
		}
	}

	public void close() {
		if (this.ldapConnection != null) {
			closeLdapConnection(this.ldapConnection);
			this.ldapConnection = null;
		}
	}

	public String getDisabledUsersOU() {
		Properties properties = SystemEnv.INSTANCE.getDefaultProperties();
		return properties.getProperty("active_directory_disabled_users_ou");
	}

	public String[] getUsersOU() {
		Properties properties = SystemEnv.INSTANCE.getDefaultProperties();
		String ous = properties.getProperty("active_directory_users_ou");
		if (ous == null) {
			return new String[0];
		} else {
			return ous.split(";");
		}
	}

	public String getBaseOU() {
		Properties properties = SystemEnv.INSTANCE.getDefaultProperties();
		return properties.getProperty("active_directory_base_ou");
	}

	public void addEntry(DefaultEntry entry) throws Exception {
		// if the LDAP connection is dropped, try to reconnect
		try {
			if (!this.ldapConnection.isConnected()) {
				reconnect();
			}
		} catch (Exception e) {
			reconnect();
		}

		this.ldapConnection.add(entry);
	}

	public void updateAttributes(String dn, Modification... modifications) throws Exception {
		// if the LDAP connection is dropped, try to reconnect
		try {
			if (!this.ldapConnection.isConnected()) {
				reconnect();
			}
		} catch (Exception e) {
			reconnect();
		}

		this.ldapConnection.modify(dn, modifications);
	}

	/**
	 * Perform a search in Active Directory
	 * 
	 * @param connection
	 * @param ou
	 * @param searchScope
	 * @param filter
	 * @param attributes
	 * @return
	 * @throws Exception
	 */
	public List<Entry> search(String ou, SearchScope searchScope, String filter, String... attributes) throws Exception {
		// if the LDAP connection is dropped, try to reconnect
		try {
			if (!this.ldapConnection.isConnected()) {
				reconnect();
			}
		} catch (Exception e) {
			reconnect();
		}

		PagedResults pagedSearchControl = new PagedResultsFactory(this.ldapConnection.getCodecService()).newControl();
		pagedSearchControl.setSize(NB_RESULTS_PER_PAGE);

		if (ou != null) {
			if (ou.contains("DC=")) {
				// ok, already a DN
			} else {
				ou += "," + getBaseOU();
			}
		} else {
			ou = getBaseOU();
		}

		// logger.debug("Searching OU[" + ou + "] Filter[" + filter + "]");

		// Loop over all the elements
		List<Entry> entries = new ArrayList<Entry>();
		while (true) {

			SearchCursor searchCursor = null;

			try {
				SearchRequest searchRequest = new SearchRequestImpl();
				searchRequest.setBase(new Dn(ou));
				searchRequest.setFilter(filter);
				searchRequest.setScope(searchScope == null ? SearchScope.SUBTREE : searchScope);
				searchRequest.addAttributes(attributes);
				searchRequest.addControl(pagedSearchControl);

				searchCursor = this.ldapConnection.search(searchRequest);
				while (searchCursor.next()) {
					if (searchCursor.isEntry()) {
						Entry entry = searchCursor.getEntry();
						entries.add(entry);
					}
				}

				SearchResultDone result = searchCursor.getSearchResultDone();
				pagedSearchControl = (PagedResults) result.getControl(PagedResults.OID);

				if (result.getLdapResult().getResultCode() == ResultCodeEnum.UNWILLING_TO_PERFORM) {
					break;
				}
			} finally {
				if (searchCursor != null) {
					searchCursor.close();
				}
			}

			// check if this is over
			if (pagedSearchControl != null) {
				byte[] cookie = pagedSearchControl.getCookie();

				if (Strings.isEmpty(cookie)) {
					// If so, exit the loop
					break;
				}

				// Prepare the next iteration
				pagedSearchControl.setSize(NB_RESULTS_PER_PAGE);
			} else {
				break;
			}
		}

		// System.out.println("GOT: " + entries.size());
		return entries;
	}

	/**
	 * 
	 * @param sid
	 * @return
	 */
	public String decodeSID(byte[] sid) {

		final StringBuilder strSid = new StringBuilder("S-");

		// get byte(0) - revision level
		final int revision = sid[0];
		strSid.append(Integer.toString(revision));

		// next byte byte(1) - count of sub-authorities
		final int countSubAuths = sid[1] & 0xFF;

		// byte(2-7) - 48 bit authority ([Big-Endian])
		long authority = 0;
		// String rid = "";
		for (int i = 2; i <= 7; i++) {
			authority |= ((long) sid[i]) << (8 * (5 - (i - 2)));
		}
		strSid.append("-");
		strSid.append(Long.toHexString(authority));

		// iterate all the sub-auths and then countSubAuths x 32 bit sub authorities ([Little-Endian])
		int offset = 8;
		int size = 4; // 4 bytes for each sub auth
		for (int j = 0; j < countSubAuths; j++) {
			long subAuthority = 0;
			for (int k = 0; k < size; k++) {
				subAuthority |= (long) (sid[offset + k] & 0xFF) << (8 * k);
			}
			// format it
			strSid.append("-");
			strSid.append(subAuthority);
			offset += size;
		}
		return strSid.toString();
	}

	public static void main(String[] args) throws Exception {
		ActiveDirectoryLDAPClient.INSTANCE.getLdapConnection();

		ActiveDirectoryLDAPClient.INSTANCE.close();
		System.out.println("Done");
	}
}
