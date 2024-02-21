package com.sgitmanagement.expressoext.approval;

import java.util.Arrays;

import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseUpdatableDeactivableEntity;
import com.sgitmanagement.expressoext.security.JobTitle;
import com.sgitmanagement.expressoext.security.Resource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlElement;

@Entity
@Table(name = "approval_flow")
public class ApprovalFlow extends BaseUpdatableDeactivableEntity {

	@Column(name = "resource_id")
	private Integer resourceId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "resource_id", insertable = false, updatable = false)
	private Resource resource;

	@Column(name = "job_title_id")
	private Integer jobTitleId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "job_title_id", insertable = false, updatable = false)
	private JobTitle jobTitle;

	@Column(name = "approval_order")
	private int approvalOrder;

	@Column(name = "max_approval_limit")
	private Integer maxApprovalLimit;

	@Column(name = "allowed_approval_keys")
	private String allowedApprovalKeys;

	@Column(name = "mandatory")
	private boolean mandatory;

	public Integer getJobTitleId() {
		return jobTitleId;
	}

	public void setJobTitleId(Integer jobTitleId) {
		this.jobTitleId = jobTitleId;
	}

	public Integer getResourceId() {
		return resourceId;
	}

	public void setResourceId(Integer resourceId) {
		this.resourceId = resourceId;
	}

	@XmlElement
	public JobTitle getJobTitle() {
		return jobTitle;
	}

	@XmlElement
	public Resource getResource() {
		return resource;
	}

	public int getApprovalOrder() {
		return approvalOrder;
	}

	public void setApprovalOrder(int approvalOrder) {
		this.approvalOrder = approvalOrder;
	}

	public Integer getMaxApprovalLimit() {
		return maxApprovalLimit;
	}

	public void setMaxApprovalLimit(Integer maxApprovalLimit) {
		this.maxApprovalLimit = maxApprovalLimit;
	}

	public boolean isRequestedApprovalLimitAllowed(Integer requestedApprovalLimit) {
		return requestedApprovalLimit == null || maxApprovalLimit == null || maxApprovalLimit >= requestedApprovalLimit;
	}

	public String getAllowedApprovalKeys() {
		return allowedApprovalKeys;
	}

	public void setAllowedApprovalKeys(String allowedApprovalKeys) {
		this.allowedApprovalKeys = allowedApprovalKeys;
	}

	public boolean isRequestedApprovalKeyAllowed(String requestedApprovalKey) {

		// if it can approve anything
		return Util.isNull(this.allowedApprovalKeys) ||

		// or it can approved this key
				(!Util.isNull(requestedApprovalKey) && Arrays.stream(this.allowedApprovalKeys.split(",")).anyMatch(requestedApprovalKey::equals));
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}
}
