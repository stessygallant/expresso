package com.sgitmanagement.expressoext.security;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.sgitmanagement.expresso.base.ParentEntity;
import com.sgitmanagement.expresso.base.ProtectedByCreator;
import com.sgitmanagement.expressoext.base.BaseEntity;

@Entity
@Table(name = "user_config")
@ProtectedByCreator
public class UserConfig extends BaseEntity {

	@Column(name = "user_id")
	private Integer userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	@ParentEntity
	private User user;

	@Column(name = "config_key")
	private String key;

	@Column(name = "config_value", length = 16777216, columnDefinition = "mediumtext")
	private String value;

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public User getUser() {
		return user;
	}

}