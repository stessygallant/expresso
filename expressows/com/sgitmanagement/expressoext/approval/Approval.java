package com.sgitmanagement.expressoext.approval;

import java.util.Date;

import com.sgitmanagement.expresso.util.JAXBDateAdapter;
import com.sgitmanagement.expressoext.base.BaseUpdatableEntity;
import com.sgitmanagement.expressoext.security.Resource;
import com.sgitmanagement.expressoext.security.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@Entity
@Table(name = "approval")
public class Approval extends BaseUpdatableEntity {

	@Column(name = "resource_id")
	private Integer resourceId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "resource_id", insertable = false, updatable = false)
	private Resource resource;

	@Column(name = "entity_id")
	private Integer entityId;

	@Column(name = "approval_status_id")
	private Integer approvalStatusId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "approval_status_id", insertable = false, updatable = false)
	private ApprovalStatus approvalStatus;

	@Column(name = "last_approval_user_id")
	private Integer lastApprovalUserId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "last_approval_user_id", insertable = false, updatable = false)
	private User lastApprovalUser;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_approval_date")
	private Date lastApprovalDate;

	@Column(name = "last_approval_flow_id")
	private Integer lastApprovalFlowId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "last_approval_flow_id", insertable = false, updatable = false)
	private ApprovalFlow lastApprovalFlow;

	@Column(name = "requested_min_approval_limit")
	private Integer requestedMinApprovalLimit;

	@Column(name = "requested_approval_key")
	private String requestedApprovalKey;

	@Column(name = "comment")
	private String comment;

	@Column(name = "next_approval_flow_id")
	private Integer nextApprovalFlowId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "next_approval_flow_id", insertable = false, updatable = false)
	private ApprovalFlow nextApprovalFlow;

	public Integer getResourceId() {
		return resourceId;
	}

	public void setResourceId(Integer resourceId) {
		this.resourceId = resourceId;
	}

	@XmlElement
	public Resource getResource() {
		return resource;
	}

	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

	@XmlElement
	public User getLastApprovalUser() {
		return lastApprovalUser;
	}

	public Date getLastApprovalDate() {
		return lastApprovalDate;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setLastApprovalDate(Date lastApprovalDate) {
		this.lastApprovalDate = lastApprovalDate;
	}

	public Integer getRequestedMinApprovalLimit() {
		return requestedMinApprovalLimit;
	}

	public void setRequestedMinApprovalLimit(Integer requestedMinApprovalLimit) {
		this.requestedMinApprovalLimit = requestedMinApprovalLimit;
	}

	public String getRequestedApprovalKey() {
		return requestedApprovalKey;
	}

	public void setRequestedApprovalKey(String requestedApprovalKey) {
		this.requestedApprovalKey = requestedApprovalKey;
	}

	public Integer getLastApprovalUserId() {
		return lastApprovalUserId;
	}

	public void setLastApprovalUserId(Integer lastApprovalUserId) {
		this.lastApprovalUserId = lastApprovalUserId;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Integer getApprovalStatusId() {
		return approvalStatusId;
	}

	public void setApprovalStatusId(Integer approvalStatusId) {
		this.approvalStatusId = approvalStatusId;
	}

	public Integer getLastApprovalFlowId() {
		return lastApprovalFlowId;
	}

	public void setLastApprovalFlowId(Integer lastApprovalFlowId) {
		this.lastApprovalFlowId = lastApprovalFlowId;
	}

	@XmlElement
	public ApprovalStatus getApprovalStatus() {
		return approvalStatus;
	}

	@XmlElement
	public ApprovalFlow getLastApprovalFlow() {
		return lastApprovalFlow;
	}

	@XmlElement
	public boolean isCompleted() {
		return getApprovalStatus().getPgmKey().equals("APPROVED") || getApprovalStatus().getPgmKey().equals("REJECTED");
	}

	public Integer getNextApprovalFlowId() {
		return nextApprovalFlowId;
	}

	public void setNextApprovalFlowId(Integer nextApprovalFlowId) {
		this.nextApprovalFlowId = nextApprovalFlowId;
	}

	@XmlElement
	public ApprovalFlow getNextApprovalFlow() {
		return nextApprovalFlow;
	}
}
