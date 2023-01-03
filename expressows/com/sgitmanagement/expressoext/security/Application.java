package com.sgitmanagement.expressoext.security;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlElement;

import com.sgitmanagement.expressoext.base.BaseUpdatableDeactivableEntity;

@Entity
@Table(name = "application")
public class Application extends BaseUpdatableDeactivableEntity {
	@Column(name = "pgm_key")
	private String pgmKey;

	@Column(name = "description")
	private String description;

	@Column(name = "parameter")
	private String parameter;

	@Column(name = "system_application")
	private boolean systemApplication;

	@Column(name = "internal_only")
	private boolean internalOnly;

	@Column(name = "comments")
	private String comments;

	@Column(name = "owner_user_id")
	private Integer ownerUserId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "owner_user_id", insertable = false, updatable = false)
	private User ownerUser;

	@Column(name = "department_id")
	private Integer departmentId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "department_id", insertable = false, updatable = false)
	private Department department;

	@ManyToMany
	@JoinTable(name = "role_application",
			// join
			joinColumns = @JoinColumn(name = "application_id", referencedColumnName = "id"),
			// inverse
			inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
	private Set<Role> roles;

	public Set<Role> getRoles() {
		if (roles == null) {
			roles = new HashSet<>();
		}
		return roles;
	}

	public String getPgmKey() {
		return pgmKey;
	}

	public void setPgmKey(String pgmKey) {
		this.pgmKey = pgmKey;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public Integer getOwnerUserId() {
		return ownerUserId;
	}

	public void setOwnerUserId(Integer ownerUserId) {
		this.ownerUserId = ownerUserId;
	}

	@XmlElement
	public User getOwnerUser() {
		return ownerUser;
	}

	public Integer getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(Integer departmentId) {
		this.departmentId = departmentId;
	}

	@XmlElement
	public Department getDepartment() {
		return department;
	}

	public boolean isSystemApplication() {
		return systemApplication;
	}

	public void setSystemApplication(boolean systemApplication) {
		this.systemApplication = systemApplication;
	}

	@Override
	public String toString() {
		return "Application [pgmKey=" + pgmKey + ", description=" + description + ", systemApplication=" + systemApplication + ", comments=" + comments + ", ownerUserId=" + ownerUserId
				+ ", departmentId=" + departmentId + "]";
	}

	@Override
	public String getLabel() {
		return getPgmKey();
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	public boolean isInternalOnly() {
		return internalOnly;
	}

	public void setInternalOnly(boolean internalOnly) {
		this.internalOnly = internalOnly;
	}

}