package com.sgitmanagement.expressoext.ldap;

import java.util.Set;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class ActiveDirectoryGroup {
	private String dn;
	private String name;
	private String description;
	private String info;
	private String managedBy;
	private String extensionAttribute1;

	// member DN are empty by default: must request them if needed
	private Set<String> memberDNs;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	@XmlElement
	public String getManagedByCn() {
		if (managedBy != null && managedBy.startsWith("CN=")) {
			return managedBy.substring(3, managedBy.indexOf(",OU="));
		} else {
			return managedBy;
		}
	}

	public String getManagedBy() {
		return managedBy;
	}

	public void setManagedBy(String managedBy) {
		this.managedBy = managedBy;
	}

	public String getExtensionAttribute1() {
		return extensionAttribute1;
	}

	public void setExtensionAttribute1(String extensionAttribute1) {
		this.extensionAttribute1 = extensionAttribute1;
	}

	@Override
	public String toString() {
		return "ActiveDirectoryGroup [dn=" + dn + ", name=" + name + ", description=" + description + ", info=" + info + ", managedBy=" + managedBy + ", extensionAttribute1=" + extensionAttribute1
				+ "]";
	}

	public Set<String> getMemberDNs() {
		return memberDNs;
	}

	public void setMemberDNs(Set<String> memberDNs) {
		this.memberDNs = memberDNs;
	}
}
