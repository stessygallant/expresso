package com.sgitmanagement.expressoext.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlElement;

import com.sgitmanagement.expressoext.base.BaseEntity;

@Entity
@Table(name = "privilege")
public class Privilege extends BaseEntity {
	@Column(name = "action_id")
	private Integer actionId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "action_id", insertable = false, updatable = false)
	private Action action;

	@Column(name = "resource_id")
	private Integer resourceId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "resource_id", insertable = false, updatable = false)
	private Resource resource;

	public Privilege() {
	}

	public Privilege(Integer actionId, Integer resourceId) {
		super();
		this.actionId = actionId;
		this.resourceId = resourceId;
	}

	@XmlElement
	public Action getAction() {
		return action;
	}

	public Integer getActionId() {
		return actionId;
	}

	public void setActionId(Integer actionId) {
		this.actionId = actionId;
	}

	@XmlElement
	public Resource getResource() {
		return resource;
	}

	public Integer getResourceId() {
		return resourceId;
	}

	public void setResourceId(Integer resourceId) {
		this.resourceId = resourceId;
	}

}