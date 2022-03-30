package com.sgitmanagement.expressoext.security;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.sgitmanagement.expressoext.base.BaseOption;

@Entity
@Table(name = "action")
public class Action extends BaseOption {
	public enum R {
		READ(2, "read"); // Lecture

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

	@Column(name = "system_action")
	private boolean systemAction;

	public boolean isSystemAction() {
		return systemAction;
	}

	public void setSystemAction(boolean systemAction) {
		this.systemAction = systemAction;
	}

}