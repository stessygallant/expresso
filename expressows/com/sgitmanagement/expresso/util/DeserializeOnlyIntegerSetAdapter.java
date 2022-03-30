package com.sgitmanagement.expresso.util;

import java.util.HashSet;
import java.util.Set;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class DeserializeOnlyIntegerSetAdapter extends XmlAdapter<Set<Integer>, Set<Integer>> {

	@Override
	public Set<Integer> unmarshal(Set<Integer> value) throws Exception {
		if (value == null) {
			return new HashSet<>();
		} else {
			return value;
		}
	}

	@Override
	public Set<Integer> marshal(Set<Integer> value) throws Exception {
		// ignore marshall so you have half duplex
		return null;
	}
}
