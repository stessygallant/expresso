package com.sgitmanagement.expresso.base;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;

public interface Updatable {
	public Integer getLastModifiedUserId();

	public void setLastModifiedUserId(Integer lastModifiedUserId);

	public Date getLastModifiedDate();

	public void setLastModifiedDate(Date lastModifiedDate);

	public IUser getLastModifiedUser();

	@XmlElement
	public String getLastModifiedUserFullName();
}
