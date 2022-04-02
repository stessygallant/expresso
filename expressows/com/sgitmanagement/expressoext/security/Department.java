package com.sgitmanagement.expressoext.security;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import jakarta.xml.bind.annotation.XmlElement;

import com.sgitmanagement.expressoext.base.BaseExternalOption;

@Entity
@Table(name = "department")
public class Department extends BaseExternalOption {

	@Column(name = "representative_user_id")
	private Integer representativeUserId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "representative_user_id", insertable = false, updatable = false)
	private BasicUser representativeUser;

	@Column(name = "company_id")
	private Integer companyId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "company_id", insertable = false, updatable = false)
	private Company company;

	@ManyToMany
	@JoinTable(name = "department_role",
			//
			joinColumns = @JoinColumn(name = "department_id", referencedColumnName = "id"),
			//
			inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
	private Set<Role> roles;

	public Integer getRepresentativeUserId() {
		return representativeUserId;
	}

	public void setRepresentativeUserId(Integer representativeUserId) {
		this.representativeUserId = representativeUserId;
	}

	@XmlElement
	public BasicUser getRepresentativeUser() {
		return representativeUser;
	}

	public Set<Role> getRoles() {
		if (roles == null) {
			roles = new HashSet<>();
		}
		return roles;
	}

	public Integer getCompanyId() {
		return companyId;
	}

	public void setCompanyId(Integer companyId) {
		this.companyId = companyId;
	}

	@XmlElement
	public Company getCompany() {
		return company;
	}
}