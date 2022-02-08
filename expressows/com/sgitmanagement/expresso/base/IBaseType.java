package com.sgitmanagement.expresso.base;

public interface IBaseType extends IEntity<Integer>, Deactivable {
	public String getDescription();

	public void setDescription(String description);

	public String getPgmKey();

	public void setPgmKey(String pgmKey);

	public Integer getSortOrder();

	public void setSortOrder(Integer sortOrder);

}
