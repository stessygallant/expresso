package com.sgitmanagement.expressoext.document;

import java.util.Date;

import com.sgitmanagement.expresso.util.JAXBDateAdapter;
import com.sgitmanagement.expressoext.base.BaseFile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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

	@Temporal(TemporalType.DATE)
	@Column(name = "from_date")
	private Date fromDate;

	@Temporal(TemporalType.DATE)
	@Column(name = "to_date")
	private Date toDate;

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

	public Date getFromDate() {
		return fromDate;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}

	public Date getToDate() {
		return toDate;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}
}
