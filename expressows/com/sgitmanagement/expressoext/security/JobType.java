package com.sgitmanagement.expressoext.security;

import java.util.HashSet;
import java.util.Set;

import com.sgitmanagement.expressoext.base.BaseOption;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_type")
public class JobType extends BaseOption {
	public enum Type {
		STAFF(1, "STAFF"), HOURLY(2, "HOURLY"), EXTERNAL(3, "EXTERNAL");

		private final int id;
		private final String pgmKey;

		private Type(int id, String pgmKey) {
			this.id = id;
			this.pgmKey = pgmKey;
		}

		public int getId() {
			return id;
		}

		public String getPgmKey() {
			return pgmKey;
		}
	}

	@ManyToMany
	@JoinTable(name = "job_type_role",
			// Vendor
			joinColumns = @JoinColumn(name = "job_type_id", referencedColumnName = "id"),
			// Craft
			inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
	private Set<Role> roles;

	public Set<Role> getRoles() {
		if (roles == null) {
			roles = new HashSet<>();
		}
		return roles;
	}
}
