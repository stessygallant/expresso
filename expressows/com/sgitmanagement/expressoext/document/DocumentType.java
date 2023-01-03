package com.sgitmanagement.expressoext.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.sgitmanagement.expressoext.base.BaseOption;

@Entity
@Table(name = "document_type")
public class DocumentType extends BaseOption {

	@Column(name = "resource_name")
	private String resourceName;

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}
}
