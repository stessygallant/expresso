package com.sgitmanagement.expressoext.util;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.sgitmanagement.expressoext.base.BaseCreatableEntity;

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