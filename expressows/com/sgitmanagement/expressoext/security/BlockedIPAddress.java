package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expressoext.base.BaseUpdatableDeactivableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "blocked_ip_address")
public class BlockedIPAddress extends BaseUpdatableDeactivableEntity {

	@Column(name = "ip_address")
	private String ipAddress;

	@Column(name = "notes")
	private String notes;

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	@Override
	public String getLabel() {
		return getIpAddress();
	}
}
