package com.sgitmanagement.expressoext.modif;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.sgitmanagement.expressoext.base.BaseCreatableEntity;

@Entity
@Table(name = "audit_trail")
public class AuditTrail extends BaseCreatableEntity {

	@Column(name = "resource_name")
	private String resourceName;

	@Column(name = "resource_id")
	private Integer resourceId;

	@Column(name = "resource_field_name")
	private String resourceFieldName;

	@Column(name = "old_value")
	private String oldValue;

	@Column(name = "new_value")
	private String newValue;

	public AuditTrail() {

	}

	public AuditTrail(String resourceName, Integer resourceId, String resourceFieldName, String oldValue, String newValue, Integer userId) {
		super();
		this.resourceName = resourceName;
		this.resourceId = resourceId;
		this.resourceFieldName = resourceFieldName;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.setCreationUserId(userId);
		this.setCreationDate(new Date());
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public Integer getResourceId() {
		return resourceId;
	}

	public void setResourceId(Integer resourceId) {
		this.resourceId = resourceId;
	}

	public String getResourceFieldName() {
		return resourceFieldName;
	}

	public void setResourceFieldName(String resourceFieldName) {
		this.resourceFieldName = resourceFieldName;
	}

	public String getOldValue() {
		return oldValue;
	}

	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}
}
