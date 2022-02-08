package com.sgitmanagement.expresso.util;

import org.hibernate.dialect.MySQL57Dialect;

public class MySQL5EnhancedDialect extends MySQL57Dialect {

	public MySQL5EnhancedDialect() {
		super();

		registerKeyword("INTERVAL");
		registerKeyword("DAY");
		registerKeyword("SEPARATOR");
		registerKeyword("GROUP_CONCAT");
		registerKeyword("TIMEDIFF");
		registerKeyword("TIMESTAMPDIFF");
		registerKeyword("MINUTE");

		// if you need to refer label with AS
		registerKeyword("as_param_1");
		registerKeyword("as_param_2");
		registerKeyword("as_param_3");
		registerKeyword("as_param_4");
		registerKeyword("as_param_5");
		registerKeyword("as_param_6");
		registerKeyword("as_param_7");
	}
}
