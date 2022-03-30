package com.sgitmanagement.expresso.util;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class DeserializeOnlyStringAdapter extends XmlAdapter<String, String> {

	@Override
	public String unmarshal(String value) throws Exception {
		return value;
	}

	@Override
	public String marshal(String value) throws Exception {
		// ignore marshall so you have half duplex
		return null;
	}
}
