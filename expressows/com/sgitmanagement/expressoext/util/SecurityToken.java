package com.sgitmanagement.expressoext.util;

import com.sgitmanagement.expressoext.base.BaseCreatableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "security_token")
public class SecurityToken extends BaseCreatableEntity {

	@Column(name = "security_token_no")
	private String securityTokenNo;

	public String getSecurityTokenNo() {
		return securityTokenNo;
	}

	public void setSecurityTokenNo(String securityTokenNo) {
		this.securityTokenNo = securityTokenNo;
	}

}