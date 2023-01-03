package com.sgitmanagement.expressoext.base;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.Formula;

import com.sgitmanagement.expresso.base.Creatable;
import com.sgitmanagement.expresso.util.JAXBDateAdapter;
import com.sgitmanagement.expressoext.security.User;

@MappedSuperclass
public abstract class BaseCreatableEntity extends BaseEntity implements Creatable {
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

	// @Column
	// @CreationTimestamp
	// private LocalDateTime createDateTime;

	protected BaseCreatableEntity() {
		super();
	}

	protected BaseCreatableEntity(BaseCreatableEntity baseCreatableEntity) {
		super(baseCreatableEntity);
	}

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

	@XmlElement
	public String getCreationUserFullName() {
		return creationUserFullName;
	}

}