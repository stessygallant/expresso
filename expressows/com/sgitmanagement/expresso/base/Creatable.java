package com.sgitmanagement.expresso.base;

import java.util.Date;

public interface Creatable {
	public Integer getCreationUserId();

	public void setCreationUserId(Integer creationUserId);

	public Date getCreationDate();

	public void setCreationDate(Date creationDate);

	public IUser getCreationUser();

	public String getCreationUserFullName();
}
