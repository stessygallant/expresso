package com.sgitmanagement.expresso.base;

import javax.xml.bind.annotation.XmlElement;

public interface IUser extends IEntity<Integer> {

	@XmlElement
	public String getFullName();

	@XmlElement
	public String getUserName();
}
