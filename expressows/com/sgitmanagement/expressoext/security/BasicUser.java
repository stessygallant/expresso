package com.sgitmanagement.expressoext.security;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.sgitmanagement.expresso.base.IUser;
import com.sgitmanagement.expresso.util.JAXBDateAdapter;

@Entity
@Table(name = "user")
public class BasicUser extends BasicPerson implements IUser {

	@Column(name = "ext_key")
	private String extKey;

	@Column(name = "username")
	private String userName;

	@Temporal(TemporalType.DATE)
	@Column(name = "termination_date")
	private Date terminationDate;

	@Override
	@XmlElement
	public String getUserName() {
		return this.userName;
	}

	@XmlElement
	public String getExtKey() {
		return extKey;
	}

	public Date getTerminationDate() {
		return terminationDate;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setTerminationDate(Date terminationDate) {
		this.terminationDate = terminationDate;
	}
}
