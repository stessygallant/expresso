package com.sgitmanagement.expressoext.base;

import java.util.Date;

import org.hibernate.annotations.Formula;

import com.sgitmanagement.expresso.base.Creatable;
import com.sgitmanagement.expresso.base.Updatable;
import com.sgitmanagement.expresso.util.JAXBDateAdapter;
import com.sgitmanagement.expressoext.security.User;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@MappedSuperclass
public abstract class BaseUpdatableOptionEntity extends BaseOption implements Updatable, Creatable {
	@Column(name = "creation_user_id")
	private Integer creationUserId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "creation_user_id", insertable = false, updatable = false)
	private User creationUser;

	@Formula(value = "(SELECT CONCAT(p.first_name, \" \", p.last_name) FROM person p WHERE p.id = creation_user_id)")
	private String creationUserFullName;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "creation_date")
	private Date creationDate;

	@Column(name = "last_modified_user_id")
	private Integer lastModifiedUserId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "last_modified_user_id", insertable = false, updatable = false)
	private User lastModifiedUser;

	@Formula(value = "(SELECT CONCAT(p.first_name, \" \", p.last_name) FROM person p WHERE p.id = last_modified_user_id)")
	private String lastModifiedUserFullName;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_modified_date")
	private Date lastModifiedDate;

	@Override
	public Integer getCreationUserId() {
		return creationUserId;
	}

	@Override
	public void setCreationUserId(Integer creationUserId) {
		this.creationUserId = creationUserId;
	}

	@Override
	public Date getCreationDate() {
		return creationDate;
	}

	@Override
	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	@Override
	public User getCreationUser() {
		return creationUser;
	}

	@Override
	@XmlElement
	public String getCreationUserFullName() {
		return creationUserFullName;
	}

	@Override
	public Integer getLastModifiedUserId() {
		return lastModifiedUserId;
	}

	@Override
	public void setLastModifiedUserId(Integer lastModifiedUserId) {
		this.lastModifiedUserId = lastModifiedUserId;
	}

	@Override
	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	@Override
	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	@Override
	public User getLastModifiedUser() {
		return lastModifiedUser;
	}

	@Override
	@XmlElement
	public String getLastModifiedUserFullName() {
		return lastModifiedUserFullName;
	}
}