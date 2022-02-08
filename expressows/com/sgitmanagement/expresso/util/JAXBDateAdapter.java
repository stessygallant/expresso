package com.sgitmanagement.expresso.util;

import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class JAXBDateAdapter extends XmlAdapter<String, Date> {
	@Override
	public Date unmarshal(String v) throws Exception {
		return DateUtil.parseDate(v);
	}

	@Override
	public String marshal(Date v) throws Exception {
		// 1970-01-01 is the default date when no date
		if (v != null) {
			// v contains the timezone, verify up to 2 days
			if (v.getTime() < (1000 * 60 * 60 * 24 * 2) && v.getTime() >= 0) {
				return DateUtil.formatDate(v, DateUtil.TIME_FORMAT_TL.get());
			} else {
				return DateUtil.formatDateTime(v);
			}
		} else {
			return null;
		}
	}
}