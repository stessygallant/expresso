package com.sgitmanagement.expressoext.security;

import org.hibernate.annotations.Formula;

import com.sgitmanagement.expressoext.base.BaseDeactivableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class BasePerson extends BaseDeactivableEntity {

	@Column(name = "email")
	private String email;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "last_name")
	private String lastName;

	@Formula(value = "CONCAT(first_name, \" \", last_name)")
	private String fullName;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		if (fullName.indexOf(' ') != -1) {
			setFirstName(fullName.substring(0, fullName.indexOf(' ')));
			setLastName(fullName.substring(fullName.indexOf(' ')));
		} else {
			setFirstName("");
			setLastName(fullName);
		}
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Override
	public String getLabel() {
		return getFullName();
	}

	@Override
	public String toString() {
		return "BasePerson [email=" + email + ", fullName=" + fullName + "]";
	}
}