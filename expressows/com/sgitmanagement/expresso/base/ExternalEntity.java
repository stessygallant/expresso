package com.sgitmanagement.expresso.base;

public interface ExternalEntity<I> extends IEntity<I> {

	public void setExtKey(String extKey);

	public String getExtKey();
}
