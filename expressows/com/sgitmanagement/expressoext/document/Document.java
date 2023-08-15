package com.sgitmanagement.expressoext.document;

import com.sgitmanagement.expressoext.base.BaseFile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlElement;

@Entity
@Table(name = "document")
public class Document extends BaseFile {

	@Column(name = "resource_name")
	private String resourceName;

	@Column(name = "resource_id")
	private Integer resourceId;

	@Column(name = "description")
	private String description;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "document_type_id", insertable = false, updatable = false)
	private DocumentType documentType;

	@Column(name = "document_type_id")
	private Integer documentTypeId;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getDocumentTypeId() {
		return documentTypeId;
	}

	public void setDocumentTypeId(Integer documentTypeId) {
		this.documentTypeId = documentTypeId;
	}

	@XmlElement
	public DocumentType getDocumentType() {
		return documentType;
	}
}
