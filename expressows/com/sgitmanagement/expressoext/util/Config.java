package com.sgitmanagement.expressoext.util;

import java.util.Date;

import com.sgitmanagement.expresso.util.JAXBDateAdapter;
import com.sgitmanagement.expressoext.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@Entity
@Table(name = "config")
public class Config extends BaseEntity {

	@Column(name = "key_name")
	private String key;

	@Column(name = "int_value")
	private Integer intValue;

	@Column(name = "float_value")
	private Float floatValue;

	@Column(name = "char_value")
	private String charValue;

	@Temporal(TemporalType.DATE)
	@Column(name = "date_value")
	private Date dateValue;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "datetime_value")
	private Date datetimeValue;

	public Config() {

	}

	public Config(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Integer getIntValue() {
		return intValue;
	}

	public void setIntValue(Integer intValue) {
		this.intValue = intValue;
	}

	public Float getFloatValue() {
		return floatValue;
	}

	public void setFloatValue(Float floatValue) {
		this.floatValue = floatValue;
	}

	public String getCharValue() {
		return charValue;
	}

	public void setCharValue(String charValue) {
		this.charValue = charValue;
	}

	public Date getDateValue() {
		return dateValue;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setDateValue(Date dateValue) {
		this.dateValue = dateValue;
	}

	public Date getDatetimeValue() {
		return datetimeValue;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setDatetimeValue(Date datetimeValue) {
		this.datetimeValue = datetimeValue;
	}

}
