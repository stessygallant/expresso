package com.sgitmanagement.expressoext.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.sgitmanagement.expresso.base.ParentEntity;
import com.sgitmanagement.expresso.base.ProtectedByCreator;
import com.sgitmanagement.expressoext.base.BaseEntity;

@Entity
@Table(name = "user_preference")
@ProtectedByCreator
public class UserPreference extends BaseEntity {

	@Column(name = "user_id")
	private Integer userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	@ParentEntity
	private User user;

	@Column(name = "application")
	private String application;

	@Column(name = "preferences", length = 65536, columnDefinition = "text")
	private String preferences;

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public User getUser() {
		return user;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getPreferences() {
		return preferences;
	}

	public void setPreferences(String preferences) {
		this.preferences = preferences;
	}

}