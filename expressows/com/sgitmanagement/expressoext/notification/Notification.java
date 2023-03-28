package com.sgitmanagement.expressoext.notification;

import java.util.Date;

import com.sgitmanagement.expresso.util.JAXBDateAdapter;
import com.sgitmanagement.expressoext.base.BaseUpdatableDeactivableEntity;
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
@Table(name = "notification")
public class Notification extends BaseUpdatableDeactivableEntity {

	@Column(name = "resource_name")
	private String resourceName;

	@Column(name = "resource_id")
	private Integer resourceId;

	@Column(name = "resource_no")
	private String resourceTitle;

	@Column(name = "resource_ext_key")
	private String resourceExtKey;

	@Column(name = "resource_status_pgm_key")
	private String resourceStatusPgmKey;

	@Column(name = "resource_url")
	private String resourceUrl;

	@Column(name = "service_description")
	private String serviceDescription;

	@Column(name = "description")
	private String description;

	@Column(name = "notes")
	private String notes;

	@Column(name = "avail_actions")
	private String availableActions;

	/*
	 * The <user> is the person responsible
	 */
	@Column(name = "user_id")
	private Integer userId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private User user;

	// The <notifiedUser> is the person who received the notification (may be the same as user or by delegation)
	@Column(name = "notified_user_id")
	private Integer notifiedUserId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "notified_user_id", insertable = false, updatable = false)
	private User notifiedUser;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "requested_date")
	private Date requestedDate;

	@Column(name = "requester_user_id")
	private Integer requesterUserId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "requester_user_id", insertable = false, updatable = false)
	private User requesterUser;

	@Column(name = "performed_action")
	private String performedAction;

	@Column(name = "performed_action_user_id")
	private Integer performedActionUserId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "performed_action_user_id", insertable = false, updatable = false)
	private User performedActionUser;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "performed_action_date")
	private Date performedActionDate;

	@Column(name = "notifiable_service_class_name")
	private String notifiableServiceClassName;

	@Override
	public String getLabel() {
		return getDescription();
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

	public String getResourceExtKey() {
		return resourceExtKey;
	}

	public void setResourceExtKey(String resourceExtKey) {
		this.resourceExtKey = resourceExtKey;
	}

	public String getResourceUrl() {
		return resourceUrl;
	}

	public void setResourceUrl(String resourceUrl) {
		this.resourceUrl = resourceUrl;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAvailableActions() {
		return availableActions;
	}

	public void setAvailableActions(String availableActions) {
		this.availableActions = availableActions;
	}

	public Date getRequestedDate() {
		return requestedDate;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setRequestedDate(Date requestedDate) {
		this.requestedDate = requestedDate;
	}

	public Integer getRequesterUserId() {
		return requesterUserId;
	}

	public void setRequesterUserId(Integer requesterUserId) {
		this.requesterUserId = requesterUserId;
	}

	public String getPerformedAction() {
		return performedAction;
	}

	public void setPerformedAction(String performedAction) {
		this.performedAction = performedAction;
	}

	public Integer getPerformedActionUserId() {
		return performedActionUserId;
	}

	public void setPerformedActionUserId(Integer performedActionUserId) {
		this.performedActionUserId = performedActionUserId;
	}

	public Date getPerformedActionDate() {
		return performedActionDate;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setPerformedActionDate(Date performedActionDate) {
		this.performedActionDate = performedActionDate;
	}

	@XmlElement
	public User getRequesterUser() {
		return requesterUser;
	}

	@XmlElement
	public User getPerformedActionUser() {
		return performedActionUser;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Integer getNotifiedUserId() {
		return notifiedUserId;
	}

	public void setNotifiedUserId(Integer notifiedUserId) {
		this.notifiedUserId = notifiedUserId;
	}

	@XmlElement
	public User getUser() {
		return user;
	}

	@XmlElement
	public User getNotifiedUser() {
		return notifiedUser;
	}

	public String getNotifiableServiceClassName() {
		return notifiableServiceClassName;
	}

	public void setNotifiableServiceClassName(String notifiableServiceClassName) {
		this.notifiableServiceClassName = notifiableServiceClassName;
	}

	public String getServiceDescription() {
		return serviceDescription;
	}

	public void setServiceDescription(String serviceDescription) {
		this.serviceDescription = serviceDescription;
	}

	@Override
	public String toString() {
		return "Notification [resourceName=" + resourceName + ", resourceId=" + resourceId + ", resourceTitle=" + resourceTitle + ", resourceExtKey=" + resourceExtKey + ", resourceUrl=" + resourceUrl
				+ ", serviceDescription=" + serviceDescription + ", description=" + description + ", availableActions=" + availableActions + ", userId=" + userId + ", notifiedUserId=" + notifiedUserId
				+ ", requestedDate=" + requestedDate + ", requesterUserId=" + requesterUserId + "]";
	}

	public String getResourceStatusPgmKey() {
		return resourceStatusPgmKey;
	}

	public void setResourceStatusPgmKey(String resourceStatusPgmKey) {
		this.resourceStatusPgmKey = resourceStatusPgmKey;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getResourceTitle() {
		return resourceTitle;
	}

	public void setResourceTitle(String resourceTitle) {
		this.resourceTitle = resourceTitle;
	}
}
