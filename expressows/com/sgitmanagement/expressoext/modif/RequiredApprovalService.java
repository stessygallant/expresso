package com.sgitmanagement.expressoext.modif;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sgitmanagement.expresso.base.PersistenceManager;
import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseEntity;
import com.sgitmanagement.expressoext.base.BaseEntityService;

public class RequiredApprovalService extends BaseEntityService<RequiredApproval> {
	@Override
	public RequiredApproval create(RequiredApproval e) throws Exception {
		e.setRequiredApprovalStatusId(newService(RequiredApprovalStatusService.class, RequiredApprovalStatus.class).get("NEW").getId());
		return super.create(e);
	}

	@Override
	protected Filter getActiveOnlyFilter() throws Exception {
		return new Filter("requiredApprovalStatus.pgmKey", Operator.eq, "NEW");
	}

	@Override
	public List<RequiredApproval> list(Query query) throws Exception {
		// can only be used to see one resource at a time
		// verify if the user is allowed to read the resource

		if (query.getFilter("id") != null) {
			Integer id = Integer.parseInt("" + query.getFilter("id").getValue());
			RequiredApproval requiredApproval = get(id);
			verifyUserPrivileges("read", requiredApproval.getResourceName(), requiredApproval.getResourceId());
			List<RequiredApproval> requiredApprovals = new ArrayList<>();
			requiredApprovals.add(requiredApproval);
			return requiredApprovals;
		} else if (query.getFilter("resourceName") != null) {
			// Ok. only for a specific resource
			return super.list(query);
		} else {
			if (isUserAdmin()) {
				return super.list(query);
			} else {
				return new ArrayList<>();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public RequiredApproval approve(RequiredApproval requiredApproval) throws Exception {
		// we need to have an exclusive
		lock(requiredApproval);

		requiredApproval.setApprobationDate(new Date());
		requiredApproval.setApprobationUserId(getUser().getId());
		requiredApproval.setRequiredApprovalStatusId(newService(RequiredApprovalStatusService.class, RequiredApprovalStatus.class).get("APPROVED").getId());

		// set the new value on the resource
		BaseEntityService service = newService(requiredApproval.getResourceName());
		BaseEntity entity = (BaseEntity) service.get(requiredApproval.getResourceId());
		Field field = Util.getField(entity, requiredApproval.getResourceFieldName());
		if (requiredApproval.getNewValueReferenceId() != null) {
			field.set(entity, requiredApproval.getNewValueReferenceId());
		} else {
			field.set(entity, Util.convertValue(requiredApproval.getNewValue(), field.getType().getName()));
		}
		service.update(entity);

		return super.update(requiredApproval);
	}

	public RequiredApproval reject(RequiredApproval requiredApproval, String comment) throws Exception {
		requiredApproval.setApprobationDate(new Date());
		requiredApproval.setApprobationUserId(getUser().getId());
		requiredApproval.setApprobationComment(comment);
		requiredApproval.setRequiredApprovalStatusId(newService(RequiredApprovalStatusService.class, RequiredApprovalStatus.class).get("REJECTED").getId());
		return super.update(requiredApproval);
	}

	@Override
	public void verifyActionRestrictions(String action, RequiredApproval requiredApproval) {
		boolean allowed = false;
		switch (action) {
		case "update":
		case "delete":
			allowed = requiredApproval != null && (requiredApproval.getRequiredApprovalStatus().getPgmKey().equals("NEW") || isUserAdmin());
			break;
		case "approve":
		case "reject":
			allowed = requiredApproval != null && isUserAllowed("alter", requiredApproval.getResourceName())
					&& (requiredApproval.getRequiredApprovalStatus().getPgmKey().equals("NEW") || isUserAdmin());
			break;
		}

		if (!allowed) {
			throw new ForbiddenException();
		}
	}

	public static void main(String[] args) throws Exception {
		RequiredApprovalService service = newServiceStatic(RequiredApprovalService.class, RequiredApproval.class);
		service.list();

		PersistenceManager.getInstance().commitAndClose();

		System.out.println("Done");
	}

}
