package com.sgitmanagement.expresso.audit;

import com.sgitmanagement.expresso.base.Updatable;

public interface Auditable extends Updatable {
	default public String getAuditResourceName() {
		return null;
	}
}
