package com.sgitmanagement.expresso.dto;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class SearchResult<E> {
	private Set<E> data;
	private String searchText;
	private boolean tooManyResults;

	public SearchResult(String searchText, Set<E> data, boolean tooManyResults) {
		super();
		this.searchText = searchText;
		this.data = data;
		this.tooManyResults = tooManyResults;
	}

	public SearchResult(String searchText) {
		this(searchText, new LinkedHashSet<>(), false);
	}

	public SearchResult() {
		this(null);
	}

	public Set<E> getData() {
		return data;
	}

	public void setData(Set<E> data) {
		this.data = data;
	}

	public boolean isTooManyResults() {
		return tooManyResults;
	}

	public void setTooManyResults(boolean tooManyResults) {
		this.tooManyResults = tooManyResults;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}

}
