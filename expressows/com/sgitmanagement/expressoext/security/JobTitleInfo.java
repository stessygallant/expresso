package com.sgitmanagement.expressoext.security;

import java.util.Date;

import com.sgitmanagement.expresso.base.ParentEntity;
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
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@Entity
@Table(name = "job_title_info")
public class JobTitleInfo extends BaseEntity {

	@Column(name = "job_title_id")
	private Integer jobTitleId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_title_id", insertable = false, updatable = false)
	@ParentEntity
	private JobTitle jobTitle;

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

	@Override
	public String getLabel() {
		return pgmKey;
	}

	public JobTitle getJobTitle() {
		return jobTitle;
	}

	public Integer getJobTitleId() {
		return jobTitleId;
	}

	public void setJobTitleId(Integer jobTitleId) {
		this.jobTitleId = jobTitleId;
	}
}