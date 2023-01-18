package com.sgitmanagement.expressoext.security;

import org.hibernate.annotations.Formula;

import com.sgitmanagement.expressoext.base.BaseDeactivableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlElement;

@Entity
@Table(name = "resource")
public class Resource extends BaseDeactivableEntity {

	@Column(name = "name")
	private String name;

	@Column(name = "path")
	private String path;

	@Column(name = "master")
	private boolean master;

	@Column(name = "master_resource_id")
	private Integer masterResourceId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "master_resource_id", insertable = false, updatable = false)
	private Resource masterResource;

	@Column(name = "application_id")
	private Integer applicationId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "application_id", insertable = false, updatable = false)
	private Application application;

	@Formula(value = "(SELECT a.description from application a where a.id = application_id)")
	private String applicationName;

	public Integer getMasterResourceId() {
		return masterResourceId;
	}

	public void setMasterResourceId(Integer masterResourceId) {
		this.masterResourceId = masterResourceId;
	}

	@XmlElement
	public Resource getMasterResource() {
		return masterResource;
	}

	public Integer getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(Integer applicationId) {
		this.applicationId = applicationId;
	}

	public Application getApplication() {
		return application;
	}

	public boolean isMaster() {
		return master;
	}

	public void setMaster(boolean master) {
		this.master = master;
	}

	@Override
	public String getLabel() {
		return getSecurityPath();
	}

	@XmlElement
	public String getSecurityPath() {
		String securityPath = getPath();
		if (getMasterResource() != null) {
			securityPath = getMasterResource().getSecurityPath() + "/" + securityPath;
		}
		return securityPath;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@XmlElement
	public String getApplicationName() {
		return applicationName;
	}

}