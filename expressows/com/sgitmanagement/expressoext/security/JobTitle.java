package com.sgitmanagement.expressoext.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import jakarta.xml.bind.annotation.XmlElement;

import org.hibernate.annotations.Formula;

import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseExternalOption;

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

	@ManyToMany
	@JoinTable(name = "job_title_role",
			// Vendor
			joinColumns = @JoinColumn(name = "job_title_id", referencedColumnName = "id"),
			// Craft
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
			joinColumns = @JoinColumn(name = "job_title_id", referencedColumnName = "id"), //
			inverseJoinColumns = @JoinColumn(name = "managed_job_title_id", referencedColumnName = "id")) //
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
}
