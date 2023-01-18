package com.sgitmanagement.expressoext.ldap;

import java.util.Arrays;
import java.util.Date;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class ActiveDirectoryUser {
	private String dn;
	private String sAMAccountName;
	private String name;
	private String displayName;
	private String firstName;
	private String lastName;
	private String mail;
	private String telephoneNumber;
	private String title;
	private String department;
	private String manager;
	private byte[] encodedPassword;
	private String country;
	private String countryCode;
	private String city;
	private String state;
	private String postalCode;
	private String address;
	private String company;
	private String userPrincipalName;
	private String description;
	private String countryNumber;
	private String homeDrive;
	private String homeDirectory;
	private String employeeId;
	private String employeeNumber;
	private String employeeType;
	private Date lastLogonDate;
	private String extensionAttribute1;
	private String extensionAttribute2;
	private String extensionAttribute3;
	private String extensionAttribute4;
	private String extensionAttribute5;
	private String extensionAttribute6;
	private String extensionAttribute7;
	private String extensionAttribute8;
	private String extensionAttribute9;
	private String extensionAttribute10;
	private String extensionAttribute11;
	private String extensionAttribute12;
	private String extensionAttribute13;
	private String extensionAttribute14;
	private String extensionAttribute15;
	private boolean active;

	public String getDisplayName() {
		return displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getCountryNumber() {
		return countryNumber;
	}

	public void setCountryNumber(String countryNumber) {
		this.countryNumber = countryNumber;
	}

	public String getUserPrincipalName() {
		return userPrincipalName;
	}

	public void setUserPrincipalName(String userPrincipalName) {
		this.userPrincipalName = userPrincipalName;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getTelephoneNumber() {
		return telephoneNumber;
	}

	public void setTelephoneNumber(String telephoneNumber) {
		this.telephoneNumber = telephoneNumber;
	}

	public String getsAMAccountName() {
		return sAMAccountName;
	}

	public void setsAMAccountName(String sAMAccountName) {
		this.sAMAccountName = sAMAccountName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getManager() {
		return manager;
	}

	public void setManager(String manager) {
		this.manager = manager;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public byte[] getEncodedPassword() {
		return encodedPassword;
	}

	public void setEncodedPassword(byte[] encodedPassword) {
		this.encodedPassword = encodedPassword;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getHomeDrive() {
		return homeDrive;
	}

	public void setHomeDrive(String homeDrive) {
		this.homeDrive = homeDrive;
	}

	public String getHomeDirectory() {
		return homeDirectory;
	}

	public void setHomeDirectory(String homeDirectory) {
		this.homeDirectory = homeDirectory;
	}

	public String getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(String employeeId) {
		this.employeeId = employeeId;
	}

	public String getEmployeeNumber() {
		return employeeNumber;
	}

	public void setEmployeeNumber(String employeeNumber) {
		this.employeeNumber = employeeNumber;
	}

	public String getEmployeeType() {
		return employeeType;
	}

	public void setEmployeeType(String employeeType) {
		this.employeeType = employeeType;
	}

	public String getExtensionAttribute1() {
		return extensionAttribute1;
	}

	public void setExtensionAttribute1(String extensionAttribute1) {
		this.extensionAttribute1 = extensionAttribute1;
	}

	public String getExtensionAttribute2() {
		return extensionAttribute2;
	}

	public void setExtensionAttribute2(String extensionAttribute2) {
		this.extensionAttribute2 = extensionAttribute2;
	}

	public String getExtensionAttribute3() {
		return extensionAttribute3;
	}

	public void setExtensionAttribute3(String extensionAttribute3) {
		this.extensionAttribute3 = extensionAttribute3;
	}

	public String getExtensionAttribute4() {
		return extensionAttribute4;
	}

	public void setExtensionAttribute4(String extensionAttribute4) {
		this.extensionAttribute4 = extensionAttribute4;
	}

	public String getExtensionAttribute5() {
		return extensionAttribute5;
	}

	public void setExtensionAttribute5(String extensionAttribute5) {
		this.extensionAttribute5 = extensionAttribute5;
	}

	public String getExtensionAttribute6() {
		return extensionAttribute6;
	}

	public void setExtensionAttribute6(String extensionAttribute6) {
		this.extensionAttribute6 = extensionAttribute6;
	}

	public String getExtensionAttribute7() {
		return extensionAttribute7;
	}

	public void setExtensionAttribute7(String extensionAttribute7) {
		this.extensionAttribute7 = extensionAttribute7;
	}

	public String getExtensionAttribute8() {
		return extensionAttribute8;
	}

	public void setExtensionAttribute8(String extensionAttribute8) {
		this.extensionAttribute8 = extensionAttribute8;
	}

	public String getExtensionAttribute9() {
		return extensionAttribute9;
	}

	public void setExtensionAttribute9(String extensionAttribute9) {
		this.extensionAttribute9 = extensionAttribute9;
	}

	public String getExtensionAttribute10() {
		return extensionAttribute10;
	}

	public void setExtensionAttribute10(String extensionAttribute10) {
		this.extensionAttribute10 = extensionAttribute10;
	}

	public String getExtensionAttribute11() {
		return extensionAttribute11;
	}

	public void setExtensionAttribute11(String extensionAttribute11) {
		this.extensionAttribute11 = extensionAttribute11;
	}

	public String getExtensionAttribute12() {
		return extensionAttribute12;
	}

	public void setExtensionAttribute12(String extensionAttribute12) {
		this.extensionAttribute12 = extensionAttribute12;
	}

	public String getExtensionAttribute13() {
		return extensionAttribute13;
	}

	public void setExtensionAttribute13(String extensionAttribute13) {
		this.extensionAttribute13 = extensionAttribute13;
	}

	public String getExtensionAttribute14() {
		return extensionAttribute14;
	}

	public void setExtensionAttribute14(String extensionAttribute14) {
		this.extensionAttribute14 = extensionAttribute14;
	}

	public String getExtensionAttribute15() {
		return extensionAttribute15;
	}

	public void setExtensionAttribute15(String extensionAttribute15) {
		this.extensionAttribute15 = extensionAttribute15;
	}

	public Date getLastLogonDate() {
		return lastLogonDate;
	}

	public void setLastLogonDate(Date lastLogonDate) {
		this.lastLogonDate = lastLogonDate;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Override
	public String toString() {
		return "ActiveDirectoryUser [dn=" + dn + ", sAMAccountName=" + sAMAccountName + ", name=" + name + ", displayName=" + displayName + ", firstName=" + firstName + ", lastName=" + lastName
				+ ", mail=" + mail + ", telephoneNumber=" + telephoneNumber + ", title=" + title + ", department=" + department + ", manager=" + manager + ", encodedPassword="
				+ Arrays.toString(encodedPassword) + ", country=" + country + ", countryCode=" + countryCode + ", city=" + city + ", state=" + state + ", postalCode=" + postalCode + ", address="
				+ address + ", company=" + company + ", userPrincipalName=" + userPrincipalName + ", description=" + description + ", countryNumber=" + countryNumber + ", homeDrive=" + homeDrive
				+ ", homeDirectory=" + homeDirectory + ", employeeId=" + employeeId + ", employeeNumber=" + employeeNumber + ", employeeType=" + employeeType + ", lastLogonDate=" + lastLogonDate
				+ ", extensionAttribute1=" + extensionAttribute1 + ", extensionAttribute2=" + extensionAttribute2 + ", extensionAttribute3=" + extensionAttribute3 + ", extensionAttribute4="
				+ extensionAttribute4 + ", extensionAttribute5=" + extensionAttribute5 + ", extensionAttribute6=" + extensionAttribute6 + ", extensionAttribute7=" + extensionAttribute7
				+ ", extensionAttribute8=" + extensionAttribute8 + ", extensionAttribute9=" + extensionAttribute9 + ", extensionAttribute10=" + extensionAttribute10 + ", extensionAttribute11="
				+ extensionAttribute11 + ", extensionAttribute12=" + extensionAttribute12 + ", extensionAttribute13=" + extensionAttribute13 + ", extensionAttribute14=" + extensionAttribute14
				+ ", extensionAttribute15=" + extensionAttribute15 + ", active=" + active + "]";
	}

}
