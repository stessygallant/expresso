package com.sgitmanagement.expresso.util;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class DeserializeOnlyIntegerAdapter extends XmlAdapter<Integer, Integer> {

	@Override
	public Integer unmarshal(Integer value) throws Exception {
		return value;
	}

	@Override
	public Integer marshal(Integer value) throws Exception {
		// ignore marshall so you have half duplex
		return null;
	}
}
