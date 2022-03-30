package com.sgitmanagement.expressoext.document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

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
