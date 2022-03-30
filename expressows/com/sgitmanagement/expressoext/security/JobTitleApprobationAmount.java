package com.sgitmanagement.expressoext.security;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import jakarta.xml.bind.annotation.XmlElement;

import com.sgitmanagement.expresso.base.ParentEntity;
import com.sgitmanagement.expressoext.base.BaseUpdatableEntity;

@Entity
@Table(name = "job_title_approbation_amount")
public class JobTitleApprobationAmount extends BaseUpdatableEntity {

	@Column(name = "job_title_id")
	private Integer jobTitleId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_title_id", insertable = false, updatable = false)
	@ParentEntity
	private JobTitle jobTitle;

	@Column(name = "approbation_amount")
	private float approbationAmount;

	@Column(name = "resource_id")
	private Integer resourceId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "resource_id", insertable = false, updatable = false)
	private Resource resource;

	public Integer getJobTitleId() {
		return jobTitleId;
	}

	public void setJobTitleId(Integer jobTitleId) {
		this.jobTitleId = jobTitleId;
	}

	public float getApprobationAmount() {
		return approbationAmount;
	}

	public void setApprobationAmount(float approbationAmount) {
		this.approbationAmount = approbationAmount;
	}

	public Integer getResourceId() {
		return resourceId;
	}

	public void setResourceId(Integer resourceId) {
		this.resourceId = resourceId;
	}

	public JobTitle getJobTitle() {
		return jobTitle;
	}

	@XmlElement
	public Resource getResource() {
		return resource;
	}

}
