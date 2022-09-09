package com.sgitmanagement.expresso.base;

public interface IDocument extends IEntity<Integer> {
	public String getName();

	public String getResourceName();

	public Integer getResourceId();
}
