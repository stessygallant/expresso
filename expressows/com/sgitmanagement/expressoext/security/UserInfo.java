package com.sgitmanagement.expressoext.security;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.sgitmanagement.expresso.base.ParentEntity;
import com.sgitmanagement.expresso.base.ProtectedByCreator;
import com.sgitmanagement.expresso.util.JAXBDateAdapter;
import com.sgitmanagement.expressoext.base.BaseEntity;

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

	@Override
	public String toString() {
		return "UserInfo [userId=" + userId + ", user=" + user + ", roleInfoId=" + roleInfoId + ", roleInfo=" + roleInfo + ", password=" + password + ", numberValue=" + numberValue + ", textValue="
				+ textValue + ", stringValue=" + stringValue + ", dateValue=" + dateValue + "]";
	}

}