package com.sgitmanagement.expressoext.document;

import com.sgitmanagement.expressoext.base.BaseOption;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_type")
public class DocumentType extends BaseOption {

	@Column(name = "resource_name")
	private String resourceName;

	@Column(name = "confidential")
	private boolean confidential;

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public boolean isConfidential() {
		return confidential;
	}

	public void setConfidential(boolean confidential) {
		this.confidential = confidential;
	}
}
