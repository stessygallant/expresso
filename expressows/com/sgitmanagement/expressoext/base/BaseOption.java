package com.sgitmanagement.expressoext.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import com.sgitmanagement.expresso.base.IBaseType;

@MappedSuperclass
public abstract class BaseOption extends BaseDeactivableEntity implements IBaseType {
	@Column(name = "sort_order")
	private Integer sortOrder = 1;

	@Column(name = "pgm_key")
	private String pgmKey;

	@Column(name = "description")
	private String description;

	public BaseOption() {
	}

	public BaseOption(String pgmKey, String description) {
		this();
		this.pgmKey = pgmKey;
		this.description = description;
		this.sortOrder = 1;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getPgmKey() {
		return this.pgmKey;
	}

	@Override
	public void setPgmKey(String pgmKey) {
		this.pgmKey = pgmKey;
	}

	@Override
	public Integer getSortOrder() {
		return sortOrder;
	}

	@Override
	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	@Override
	public String getLabel() {
		return getDescription();
	}

	@Override
	public String toString() {
		return "BaseOption [sortOrder=" + sortOrder + ", pgmKey=" + pgmKey + ", description=" + description + ", getId()=" + getId() + "]";
	}

}