package com.sgitmanagement.expressoext.base;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public abstract class BaseFile extends BaseUpdatableEntity {
	@Column(name = "file_name")
	private String fileName;

	@Transient
	private String absolutePath;

	public BaseFile() {
	}

	public BaseFile(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public String getLabel() {
		return getFileName();
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public void setAbsolutePath(String absolutePath) {
		this.absolutePath = absolutePath;
	}

	public String getSuffix() {
		if (fileName != null && fileName.indexOf('.') != -1) {
			return fileName.substring(fileName.lastIndexOf('.') + 1);
		} else {
			return null;
		}
	}

	public String getFileNameNoSuffix() {
		if (fileName != null && fileName.indexOf('.') != -1) {
			return fileName.substring(0, fileName.lastIndexOf('.'));
		} else {
			return fileName;
		}
	}
}