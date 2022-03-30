package com.sgitmanagement.expressoext.base;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import com.sgitmanagement.expresso.base.ExternalEntity;

@MappedSuperclass
public abstract class BaseExternalOption extends BaseOption implements ExternalEntity<Integer> {

	@Column(name = "ext_key")
	private String extKey;

	public BaseExternalOption() {
	}

	public BaseExternalOption(String pgmKey, String description, String extKey) {
		super(pgmKey, description);
		this.extKey = extKey;
	}

	@Override
	public String getExtKey() {
		return extKey;
	}

	@Override
	public void setExtKey(String extKey) {
		this.extKey = extKey;
	}
}