package com.sgitmanagement.expressoext.security;

import java.util.Date;

import com.sgitmanagement.expresso.base.ParentEntity;
import com.sgitmanagement.expresso.base.ProtectedByCreator;
import com.sgitmanagement.expresso.util.JAXBDateAdapter;
import com.sgitmanagement.expressoext.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@Entity
@Table(name = "user_info")
@ProtectedByCreator
public class UserInfo extends BaseEntity {

	@Column(name = "user_id")
	private Integer userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	@ParentEntity
	private User user;

	@Column(name = "role_info_id")
	private Integer roleInfoId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "role_info_id", insertable = false, updatable = false)
	private RoleInfo roleInfo;

	@Column(name = "job_title_info_id")
	private Integer jobTitleInfoId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "job_title_info_id", insertable = false, updatable = false)
	private JobTitleInfo jobTitleInfo;

	@Transient
	private String password;

	@Column(name = "number_value")
	private Integer numberValue;

	@Column(name = "text_value", length = 16777216, columnDefinition = "mediumtext")
	private String textValue;

	@Column(name = "string_value")
	private String stringValue;

	@Temporal(TemporalType.DATE)
	@Column(name = "date_value")
	private Date dateValue;

	public UserInfo() {

	}

	public UserInfo(Integer userId, Integer roleInfoId) {
		super();
		this.userId = userId;
		this.roleInfoId = roleInfoId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public User getUser() {
		return user;
	}

	public Integer getRoleInfoId() {
		return roleInfoId;
	}

	public void setRoleInfoId(Integer roleInfoId) {
		this.roleInfoId = roleInfoId;
	}

	public Integer getNumberValue() {
		return numberValue;
	}

	public void setNumberValue(Integer numberValue) {
		this.numberValue = numberValue;
	}

	public String getTextValue() {
		return textValue;
	}

	public void setTextValue(String textValue) {
		this.textValue = textValue;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	public Date getDateValue() {
		return dateValue;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setDateValue(Date dateValue) {
		this.dateValue = dateValue;
	}

	@XmlElement
	public RoleInfo getRoleInfo() {
		return roleInfo;
	}

	public Integer getJobTitleInfoId() {
		return jobTitleInfoId;
	}

	public void setJobTitleInfoId(Integer jobTitleInfoId) {
		this.jobTitleInfoId = jobTitleInfoId;
	}

	@XmlElement
	public JobTitleInfo getJobTitleInfo() {
		return jobTitleInfo;
	}

}