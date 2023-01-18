package com.sgitmanagement.expressoext.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Formula;

import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseExternalOption;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlElement;

@Entity
@Table(name = "job_title")
public class JobTitle extends BaseExternalOption {

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "job_type_id", insertable = false, updatable = false)
	private JobType jobType;

	@Column(name = "job_type_id")
	private Integer jobTypeId;

	@OneToMany(mappedBy = "jobTitle")
	private List<JobTitleApprobationAmount> jobTitleApprobationAmounts;

	@OneToMany(mappedBy = "jobTitle")
	private List<User> users;

	@ManyToMany
	@JoinTable(name = "job_title_role",
			// Title
			joinColumns = @JoinColumn(name = "job_title_id", referencedColumnName = "id"),
			// Role
			inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
	private Set<Role> roles;

	/*
	 * Manager Job Title
	 */
	@Formula(value = "(SELECT GROUP_CONCAT(eq.managed_job_title_id SEPARATOR  ',') FROM job_title_manage eq WHERE eq.job_title_id = id)")
	private String managedJobTitleStringIds;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "job_title_manage", joinColumns = @JoinColumn(name = "job_title_id"))
	@Column(name = "managed_job_title_id")
	private Set<Integer> managedJobTitleIds = new HashSet<>();

	@ManyToMany
	@JoinTable(name = "job_title_manage", //
			joinColumns = @JoinColumn(name = "job_title_id", referencedColumnName = "id"), // Title
			inverseJoinColumns = @JoinColumn(name = "managed_job_title_id", referencedColumnName = "id")) // managed Title
	private Set<JobTitle> managedJobTitles;

	public Set<Role> getRoles() {
		if (roles == null) {
			roles = new HashSet<>();
		}
		return roles;
	}

	public Integer getJobTypeId() {
		return jobTypeId;
	}

	public void setJobTypeId(Integer jobTypeId) {
		this.jobTypeId = jobTypeId;
	}

	@XmlElement
	public JobType getJobType() {
		return jobType;
	}

	public List<JobTitleApprobationAmount> getJobTitleApprobationAmounts() {
		if (jobTitleApprobationAmounts == null) {
			jobTitleApprobationAmounts = new ArrayList<>();
		}
		return jobTitleApprobationAmounts;
	}

	public Set<Integer> getManagedJobTitleIds() {
		return Util.stringIdsToIntegers(this.managedJobTitleStringIds);
	}

	public void setManagedJobTitleIds(Set<Integer> managedJobTitleIds) {
		this.managedJobTitleIds.clear();
		this.managedJobTitleIds.addAll(managedJobTitleIds);
	}

	public Set<JobTitle> getManagedJobTitles() {
		return managedJobTitles;
	}

	public List<User> getUsers() {
		return users;
	}
}
