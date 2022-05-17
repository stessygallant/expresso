package com.sgitmanagement.expressoext.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Formula;

import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseOption;

import jakarta.xml.bind.annotation.XmlElement;

/**
 * The persistent class for the role database table.
 */
@Entity
@Table(name = "role")
public class Role extends BaseOption {
	public enum R {
		USER(1, "user"); // Utilisateur

		private final int id;
		private final String pgmKey;

		private R(int id, String pgmKey) {
			this.id = id;
			this.pgmKey = pgmKey;
		}

		public int getId() {
			return id;
		}

		public String getPgmKey() {
			return pgmKey;
		}
	}

	@Column(name = "system_role")
	private boolean systemRole;

	/*
	 * Privilege
	 */
	// @Formula(value = "(SELECT GROUP_CONCAT(t.privilegename SEPARATOR ',') FROM role_privilege r INNER JOIN privilege t ON t.id = r.privilege_id WHERE r.role_id = id)")
	// private String privilegeLabels;

	@Formula(value = "(SELECT GROUP_CONCAT(r.privilege_id SEPARATOR  ',') FROM role_privilege r WHERE r.role_id = id)")
	private String privilegeStringIds;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "role_privilege", joinColumns = @JoinColumn(name = "role_id"))
	@Column(name = "privilege_id")
	private Set<Integer> privilegeIds = new HashSet<>();

	@ManyToMany
	@JoinTable(name = "role_privilege", joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "privilege_id", referencedColumnName = "id"))
	private Set<Privilege> privileges;

	/*
	 * Application
	 */
	@Formula(value = "(SELECT GROUP_CONCAT(t.pgm_key SEPARATOR  ',') FROM role_application r INNER JOIN application t ON t.id = r.application_id WHERE r.role_id = id)")
	private String applicationLabels;

	@Formula(value = "(SELECT GROUP_CONCAT(r.application_id SEPARATOR  ',') FROM role_application r WHERE r.role_id = id)")
	private String applicationStringIds;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "role_application", joinColumns = @JoinColumn(name = "role_id"))
	@Column(name = "application_id")
	private Set<Integer> applicationIds = new HashSet<>();

	@ManyToMany
	@JoinTable(name = "role_application", joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "application_id", referencedColumnName = "id"))
	private Set<Application> applications;

	@OneToMany(mappedBy = "role", cascade = { CascadeType.ALL }, orphanRemoval = true)
	private List<RoleInfo> roleInfos;

	/*
	 * Department
	 */
	@Formula(value = "(SELECT GROUP_CONCAT(t.description SEPARATOR  ',') FROM department_role r INNER JOIN department t ON t.id = r.department_id WHERE r.role_id = id)")
	private String departmentLabels;

	@Formula(value = "(SELECT GROUP_CONCAT(r.department_id SEPARATOR  ',') FROM department_role r WHERE r.role_id = id)")
	private String departmentStringIds;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "department_role", joinColumns = @JoinColumn(name = "role_id"))
	@Column(name = "department_id")
	private Set<Integer> departmentIds = new HashSet<>();

	@ManyToMany
	@JoinTable(name = "department_role", joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "department_id", referencedColumnName = "id"))
	private Set<Department> departments;

	/*
	 * JobTitle
	 */
	@Formula(value = "(SELECT GROUP_CONCAT(t.description SEPARATOR  ',') FROM job_title_role r INNER JOIN job_title t ON t.id = r.job_title_id WHERE r.role_id = id)")
	private String jobTitleLabels;

	@Formula(value = "(SELECT GROUP_CONCAT(r.job_title_id SEPARATOR  ',') FROM job_title_role r WHERE r.role_id = id)")
	private String jobTitleStringIds;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "job_title_role", joinColumns = @JoinColumn(name = "role_id"))
	@Column(name = "job_title_id")
	private Set<Integer> jobTitleIds = new HashSet<>();

	@ManyToMany
	@JoinTable(name = "job_title_role", joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "job_title_id", referencedColumnName = "id"))
	private Set<JobTitle> jobTitles;

	/*
	 * JobType
	 */
	@Formula(value = "(SELECT GROUP_CONCAT(t.description SEPARATOR  ',') FROM job_type_role r INNER JOIN job_type t ON t.id = r.job_type_id WHERE r.role_id = id)")
	private String jobTypeLabels;

	@Formula(value = "(SELECT GROUP_CONCAT(r.job_type_id SEPARATOR  ',') FROM job_type_role r WHERE r.role_id = id)")
	private String jobTypeStringIds;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "job_type_role", joinColumns = @JoinColumn(name = "role_id"))
	@Column(name = "job_type_id")
	private Set<Integer> jobTypeIds = new HashSet<>();

	@ManyToMany
	@JoinTable(name = "job_type_role", joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "job_type_id", referencedColumnName = "id"))
	private Set<JobType> jobTypes;

	/*
	 * User
	 */
	@Formula(value = "(SELECT GROUP_CONCAT(t.username SEPARATOR  ',') FROM user_role r INNER JOIN user t ON t.id = r.user_id WHERE r.role_id = id)")
	private String userLabels;

	@Formula(value = "(SELECT GROUP_CONCAT(r.user_id SEPARATOR  ',') FROM user_role r WHERE r.role_id = id)")
	private String userStringIds;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "role_id"))
	@Column(name = "user_id")
	private Set<Integer> userIds = new HashSet<>();

	@ManyToMany
	@JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"))
	private Set<User> users;

	public Set<Privilege> getPrivileges() {
		if (privileges == null) {
			privileges = new HashSet<>();
		}
		return privileges;
	}

	public Set<Application> getApplications() {
		if (applications == null) {
			applications = new HashSet<>();
		}
		return applications;
	}

	public List<RoleInfo> getRoleInfos() {
		if (roleInfos == null) {
			roleInfos = new ArrayList<>();
		}
		return roleInfos;
	}

	public boolean isSystemRole() {
		return systemRole;
	}

	public void setSystemRole(boolean systemRole) {
		this.systemRole = systemRole;
	}

	/*
	 * Department
	 */
	public Set<Integer> getDepartmentIds() {
		return Util.stringIdsToIntegers(this.departmentStringIds);
	}

	public void setDepartmentIds(Set<Integer> departmentIds) {
		this.departmentIds.clear();
		this.departmentIds.addAll(departmentIds);
	}

	public Set<Department> getDepartment() {
		return departments;
	}

	@XmlElement
	public String getDepartmentLabels() {
		return departmentLabels;
	}

	/*
	 * JobTitle
	 */
	public Set<Integer> getJobTitleIds() {
		return Util.stringIdsToIntegers(this.jobTitleStringIds);
	}

	public void setJobTitleIds(Set<Integer> jobTitleIds) {
		this.jobTitleIds.clear();
		this.jobTitleIds.addAll(jobTitleIds);
	}

	public Set<JobTitle> getJobTitle() {
		return jobTitles;
	}

	@XmlElement
	public String getJobTitleLabels() {
		return jobTitleLabels;
	}

	/*
	 * JobType
	 */
	public Set<Integer> getJobTypeIds() {
		return Util.stringIdsToIntegers(this.jobTypeStringIds);
	}

	public void setJobTypeIds(Set<Integer> jobTypeIds) {
		this.jobTypeIds.clear();
		this.jobTypeIds.addAll(jobTypeIds);
	}

	public Set<JobType> getJobType() {
		return jobTypes;
	}

	@XmlElement
	public String getJobTypeLabels() {
		return jobTypeLabels;
	}

	/*
	 * User
	 */
	public Set<Integer> getUserIds() {
		return Util.stringIdsToIntegers(this.userStringIds);
	}

	public void setUserIds(Set<Integer> userIds) {
		this.userIds.clear();
		this.userIds.addAll(userIds);
	}

	public Set<User> getUser() {
		return users;
	}

	@XmlElement
	public String getUserLabels() {
		return userLabels;
	}

	/*
	 * Application
	 */
	public Set<Integer> getApplicationIds() {
		return Util.stringIdsToIntegers(this.applicationStringIds);
	}

	public void setApplicationIds(Set<Integer> applicationIds) {
		this.applicationIds.clear();
		this.applicationIds.addAll(applicationIds);
	}

	public Set<Application> getApplication() {
		return applications;
	}

	@XmlElement
	public String getApplicationLabels() {
		return applicationLabels;
	}

	/*
	 * Privilege
	 */
	public Set<Integer> getPrivilegeIds() {
		return Util.stringIdsToIntegers(this.privilegeStringIds);
	}

	public void setPrivilegeIds(Set<Integer> privilegeIds) {
		this.privilegeIds.clear();
		this.privilegeIds.addAll(privilegeIds);
	}

	public Set<Privilege> getPrivilege() {
		return privileges;
	}

	// @XmlElement
	// public String getPrivilegeLabels() {
	// return privilegeLabels;
	// }
}