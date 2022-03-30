package com.sgitmanagement.expressoext.base;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.Formula;

import com.sgitmanagement.expresso.base.Updatable;
import com.sgitmanagement.expresso.util.JAXBDateAdapter;
import com.sgitmanagement.expressoext.security.User;

@MappedSuperclass
public abstract class BaseUpdatableEntity extends BaseCreatableEntity implements Updatable {
	@Column(name = "last_modified_user_id")
	private Integer lastModifiedUserId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "last_modified_user_id", insertable = false, updatable = false)
	private User lastModifiedUser;

	@Formula(value = "(SELECT CONCAT(p.first_name, \" \", p.last_name) FROM person p WHERE p.id = last_modified_user_id)")
	private String lastModifiedUserFullName;

	// @Version
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_modified_date")
	private Date lastModifiedDate;

	// @Column
	// @UpdateTimestamp
	// private LocalDateTime updateDateTime;

	protected BaseUpdatableEntity() {
		super();
	}

	protected BaseUpdatableEntity(BaseUpdatableEntity baseUpdatableEntity) {
		super(baseUpdatableEntity);
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

	@XmlElement
	public String getLastModifiedUserFullName() {
		return lastModifiedUserFullName;
	}
}