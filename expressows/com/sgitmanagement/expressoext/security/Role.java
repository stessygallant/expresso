package com.sgitmanagement.expressoext.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.sgitmanagement.expressoext.base.BaseOption;

/**
 * The persistent class for the role database table.
 */
@Entity
@Table(name = "role")
public class Role extends BaseOption {
	public enum R {
		USER(1, "user"); // Utilisateur

		private final int id;
		private final String pgmKey;

		private R(int id, String pgmKey) {
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

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "role_privilege",
			// join
			joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
			// inverse
			inverseJoinColumns = @JoinColumn(name = "privilege_id", referencedColumnName = "id"))
	private Set<Privilege> privileges;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "role_application",
			// join
			joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
			// inverse
			inverseJoinColumns = @JoinColumn(name = "application_id", referencedColumnName = "id"))
	private Set<Application> applications;

	@OneToMany(mappedBy = "role", cascade = { CascadeType.ALL }, orphanRemoval = true)
	private List<RoleInfo> roleInfos;

	@Column(name = "system_role")
	private boolean systemRole;

	public Set<Privilege> getPrivileges() {
		if (privileges == null) {
			privileges = new HashSet<>();
		}
		return privileges;
	}

	public Set<Application> getApplications() {
		if (applications == null) {
			applications = new HashSet<>();
		}
		return applications;
	}

	public List<RoleInfo> getRoleInfos() {
		if (roleInfos == null) {
			roleInfos = new ArrayList<>();
		}
		return roleInfos;
	}

	public boolean isSystemRole() {
		return systemRole;
	}

	public void setSystemRole(boolean systemRole) {
		this.systemRole = systemRole;
	}

}