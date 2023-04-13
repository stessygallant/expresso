package com.sgitmanagement.expressoext.util;

import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expresso.util.mail.Mailer;
import com.sgitmanagement.expressoext.ldap.ActiveDirectoryLDAPClient;

public class MainUtil {
	public static void close() {
		// close all services
		Util.closeCurrentThreadInfo();

		// terminate any service, etc
		Mailer.INSTANCE.close();

		ActiveDirectoryLDAPClient.INSTANCE.close();

		System.out.println("Done");
	}
}