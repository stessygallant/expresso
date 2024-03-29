package com.sgitmanagement.expressoext.modif;

import java.util.ArrayList;
import java.util.List;

import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expressoext.base.BaseEntityService;

public class AuditTrailService extends BaseEntityService<AuditTrail> {

	@Override
	public List<AuditTrail> list(Query query) throws Exception {
		// can only be used to see one resource at a time
		// verify if the user is allowed to read the resource

		if (query.getFilter("id") != null) {
			if (query.getFilter("id").getIntValue() != -1) {
				Integer id = query.getFilter("id").getIntValue();
				AuditTrail auditTrail = get(id);
				verifyUserPrivileges("read", auditTrail.getResourceName(), auditTrail.getResourceId());
				List<AuditTrail> auditTrails = new ArrayList<>();
				auditTrails.add(auditTrail);
				return auditTrails;
			} else {
				return new ArrayList<>();
			}
		} else if (query.getFilter("resourceName") != null && query.getFilter("resourceId") != null && query.getFilter("resourceId").getValue() != null) {
			String resourceName = (String) query.getFilter("resourceName").getValue();
			Integer resourceId = query.getFilter("resourceId").getIntValue();
			verifyUserPrivileges("read", resourceName, resourceId);
			return super.list(query);
		} else {
			return new ArrayList<>();
		}
	}
}
