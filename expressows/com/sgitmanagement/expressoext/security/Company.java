package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expresso.base.ExternalEntity;
import com.sgitmanagement.expressoext.base.BaseUpdatableDeactivableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "company")
public class Company extends BaseUpdatableDeactivableEntity implements ExternalEntity<Integer> {

	@Column(name = "ext_key")
	private String extKey;

	@Column(name = "name")
	private String name;

	@Column(name = "address")
	private String address;

	@Column(name = "city")
	private String city;

	@Column(name = "billing_code")
	private String billingCode;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getLabel() {
		return getName() + (getBillingCode() != null ? " (" + getBillingCode() + ")" : "");
	}

	@Override
	public String getExtKey() {
		return extKey;
	}

	@Override
	public void setExtKey(String extKey) {
		this.extKey = extKey;
	}

	@Override
	public String toString() {
		return "Company [extKey=" + extKey + ", name=" + name + "]";
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getBillingCode() {
		return billingCode;
	}

	public void setBillingCode(String billingCode) {
		this.billingCode = billingCode;
	}

}
