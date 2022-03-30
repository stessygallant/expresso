package com.sgitmanagement.expressoext.filter;

import org.glassfish.jersey.server.ResourceConfig;

public class JerseyApplication extends ResourceConfig {

	public JerseyApplication() {
		packages("com.sgitmanagement");
		packages("ca.cezinc.portal");

		// NOTE: this only works on the main resource (does not work for sub resource)
		// register(new TransactionInterceptorBinder());
	}
}
