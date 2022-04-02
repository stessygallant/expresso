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
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.sgitmanagement.expresso.base.ParentEntity;
import com.sgitmanagement.expresso.util.JAXBDateAdapter;
import com.sgitmanagement.expressoext.base.BaseEntity;

@Entity
@Table(name = "role_info")
public class RoleInfo extends BaseEntity {

	@Column(name = "role_id")
	private Integer roleId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id", insertable = false, updatable = false)
	@ParentEntity
	private Role role;

	@Column(name = "pgm_key")
	private String pgmKey;

	@Column(name = "description")
	private String description;

	@Column(name = "type")
	private String infoType;

	@Column(name = "default_number")
	private Integer defaultNumber;

	@Column(name = "default_string")
	private String defaultString;

	@Column(name = "default_text", length = 16777216, columnDefinition = "mediumtext")
	private String defaultText;

	@Temporal(TemporalType.DATE)
	@Column(name = "default_date")
	private Date defaultDate;

	public Integer getRoleId() {
		return roleId;
	}

	public void setRoleId(Integer roleId) {
		this.roleId = roleId;
	}

	public String getPgmKey() {
		return pgmKey;
	}

	public void setPgmKey(String pgmKey) {
		this.pgmKey = pgmKey;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getInfoType() {
		return infoType;
	}

	public void setInfoType(String infoType) {
		this.infoType = infoType;
	}

	public Integer getDefaultNumber() {
		return defaultNumber;
	}

	public void setDefaultNumber(Integer defaultNumber) {
		this.defaultNumber = defaultNumber;
	}

	public String getDefaultString() {
		return defaultString;
	}

	public void setDefaultString(String defaultString) {
		this.defaultString = defaultString;
	}

	public String getDefaultText() {
		return defaultText;
	}

	public void setDefaultText(String defaultText) {
		this.defaultText = defaultText;
	}

	public Date getDefaultDate() {
		return defaultDate;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setDefaultDate(Date defaultDate) {
		this.defaultDate = defaultDate;
	}

	public Role getRole() {
		return role;
	}

	@Override
	public String getLabel() {
		return pgmKey;
	}
}