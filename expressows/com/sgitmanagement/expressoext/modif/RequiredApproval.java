package com.sgitmanagement.expressoext.modif;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.sgitmanagement.expresso.util.JAXBDateAdapter;
import com.sgitmanagement.expressoext.base.BaseCreatableEntity;
import com.sgitmanagement.expressoext.security.User;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@Entity
@Table(name = "required_approval")
public class RequiredApproval extends BaseCreatableEntity {

	@Column(name = "resource_name")
	private String resourceName;

	@Column(name = "resource_id")
	private Integer resourceId;

	@Column(name = "resource_no")
	private String resourceNo;

	@Column(name = "resource_description")
	private String resourceDescription;

	@Column(name = "resource_field_name")
	private String resourceFieldName;

	@Column(name = "old_value")
	private String oldValue;

	@Column(name = "new_value")
	private String newValue;

	@Column(name = "new_value_ref_id")
	private Integer newValueReferenceId;

	@Column(name = "required_approval_status_id")
	private Integer requiredApprovalStatusId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "required_approval_status_id", insertable = false, updatable = false)
	private RequiredApprovalStatus requiredApprovalStatus;

	@Column(name = "notes")
	private String notes;

	@Column(name = "approbation_comment")
	private String approbationComment;

	@Column(name = "approbation_user_id")
	private Integer approbationUserId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "approbation_user_id", insertable = false, updatable = false)
	private User approbationUser;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "approbation_date")
	private Date approbationDate;

	public RequiredApproval() {

	}

	public RequiredApproval(String resourceName, Integer resourceId, String resourceNo, String resourceDescription, String resourceFieldName, String oldValue, String newValue,
			Integer newValueReferenceId) {
		super();
		this.resourceName = resourceName;
		this.resourceId = resourceId;
		this.resourceNo = resourceNo;
		this.resourceDescription = resourceDescription;
		this.resourceFieldName = resourceFieldName;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.newValueReferenceId = newValueReferenceId;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public Integer getResourceId() {
		return resourceId;
	}

	public void setResourceId(Integer resourceId) {
		this.resourceId = resourceId;
	}

	public String getResourceFieldName() {
		return resourceFieldName;
	}

	public void setResourceFieldName(String resourceFieldName) {
		this.resourceFieldName = resourceFieldName;
	}

	public String getOldValue() {
		return oldValue;
	}

	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}

	public Integer getNewValueReferenceId() {
		return newValueReferenceId;
	}

	public void setNewValueReferenceId(Integer newValueReferenceId) {
		this.newValueReferenceId = newValueReferenceId;
	}

	public Integer getRequiredApprovalStatusId() {
		return requiredApprovalStatusId;
	}

	public void setRequiredApprovalStatusId(Integer requiredApprovalStatusId) {
		this.requiredApprovalStatusId = requiredApprovalStatusId;
	}

	public String getApprobationComment() {
		return approbationComment;
	}

	public void setApprobationComment(String approbationComment) {
		this.approbationComment = approbationComment;
	}

	public Integer getApprobationUserId() {
		return approbationUserId;
	}

	public void setApprobationUserId(Integer approbationUserId) {
		this.approbationUserId = approbationUserId;
	}

	public Date getApprobationDate() {
		return approbationDate;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setApprobationDate(Date approbationDate) {
		this.approbationDate = approbationDate;
	}

	@XmlElement
	public RequiredApprovalStatus getRequiredApprovalStatus() {
		return requiredApprovalStatus;
	}

	@XmlElement
	public User getApprobationUser() {
		return approbationUser;
	}

	public String getResourceNo() {
		return resourceNo;
	}

	public void setResourceNo(String resourceNo) {
		this.resourceNo = resourceNo;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getResourceDescription() {
		return resourceDescription;
	}

	public void setResourceDescription(String resourceDescription) {
		this.resourceDescription = resourceDescription;
	}
}
