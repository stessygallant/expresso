package com.sgitmanagement.expressoext.modif;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sgitmanagement.expresso.base.RequireApproval;
import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expresso.util.DateUtil;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expresso.util.mail.Mailer;
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

			if (query.getFilter("mine") != null) {
				query.removeFilter("mine");
				List<RequiredApproval> requiredApprovals = super.list(query);

				if (query.activeOnly()) {
					// do not allow on inactive because it is too long
					for (RequiredApproval requiredApproval : new ArrayList<>(requiredApprovals)) {
						if (!isUserAllowedToApprove(requiredApproval)) {
							// remove from the list
							requiredApprovals.remove(requiredApproval);
						}
					}
				}

				return requiredApprovals;
			} else {
				return super.list(query);
			}
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
		requiredApproval.setApprobationDate(new Date());
		requiredApproval.setApprobationUserId(getUser().getId());
		requiredApproval.setRequiredApprovalStatusId(newService(RequiredApprovalStatusService.class, RequiredApprovalStatus.class).get("APPROVED").getId());

		// set the new value on the resource
		BaseEntityService service = getService(requiredApproval);
		BaseEntity entity = getEntity(requiredApproval);

		// global synchronized on the service class
		synchronized (service.getClass()) {
			// because we can approve multiple request for the same entity at the same time
			service.lock(entity);

			Field field = getField(entity, requiredApproval);
			if (requiredApproval.getNewValueReferenceId() != null) {
				field.set(entity, requiredApproval.getNewValueReferenceId());
			} else {
				field.set(entity, Util.convertValue(requiredApproval.getNewValue(), field.getType().getName()));
			}
			service.update(entity);

			requiredApproval = super.update(requiredApproval);
			commit();
		}
		sendEmail(requiredApproval, "ra-approved");
		return requiredApproval;
	}

	public RequiredApproval reject(RequiredApproval requiredApproval, String comment) throws Exception {
		requiredApproval.setApprobationDate(new Date());
		requiredApproval.setApprobationUserId(getUser().getId());
		requiredApproval.setApprobationComment(comment);
		requiredApproval.setRequiredApprovalStatusId(newService(RequiredApprovalStatusService.class, RequiredApprovalStatus.class).get("REJECTED").getId());

		requiredApproval = super.update(requiredApproval);
		sendEmail(requiredApproval, "ra-rejected");
		return requiredApproval;
	}

	/**
	 * 
	 * @param modem
	 * @throws Exception
	 */
	private void sendEmail(RequiredApproval requiredApproval, String template) throws Exception {
		Map<String, String> params = new HashMap<>();
		params.put("resourceName", requiredApproval.getResourceName());
		params.put("resourceNo", requiredApproval.getResourceNo());
		params.put("approver", (requiredApproval.getApprobationUser() != null ? requiredApproval.getApprobationUser().getFullName() : ""));
		params.put("reason", requiredApproval.getApprobationComment());
		params.put("date", DateUtil.formatDate(requiredApproval.getCreationDate()));

		params.put("url", getLink(requiredApproval));

		String to = requiredApproval.getCreationUser().getEmail();

		Mailer.INSTANCE.sendMail(to, "requiredapproval/" + template, params);
	}

	@Override
	public void verifyActionRestrictions(String action, RequiredApproval requiredApproval) {
		boolean allowed = false;
		if (requiredApproval != null) {
			switch (action) {
			case "update":
			case "delete":
				// only the creator can modify it and only if NEW
				allowed = requiredApproval.getRequiredApprovalStatus().getPgmKey().equals("NEW")
						&& (requiredApproval.getCreationUserId().equals(getUser().getId()) || isUserAllowedToApprove(requiredApproval) || isUserAdmin());
				break;

			case "approve":
			case "reject":
				// only if NEW
				if (requiredApproval.getRequiredApprovalStatus().getPgmKey().equals("NEW")) {
					allowed = isUserAllowedToApprove(requiredApproval);
				}
				break;
			}
		}

		if (!allowed) {
			throw new ForbiddenException();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private BaseEntityService getService(RequiredApproval requiredApproval) throws Exception {
		BaseEntityService service = newService(requiredApproval.getResourceName());
		return service;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private BaseEntity getEntity(RequiredApproval requiredApproval) throws Exception {
		BaseEntityService service = getService(requiredApproval);
		BaseEntity entity = (BaseEntity) service.get(requiredApproval.getResourceId());
		return entity;
	}

	private Field getField(BaseEntity entity, RequiredApproval requiredApproval) throws Exception {
		Field field = Util.getField(entity, requiredApproval.getResourceFieldName());
		return field;
	}

	public boolean isUserAllowedToApprove(RequiredApproval requiredApproval) {
		boolean allowed = false;
		try {
			BaseEntity entity = getEntity(requiredApproval);
			Field field = getField(entity, requiredApproval);

			String requireApprovalRole = field.getAnnotation(RequireApproval.class).role();
			if (requireApprovalRole == null || requireApprovalRole.length() == 0) {
				// get it from the resource
				requireApprovalRole = entity.getClass().getAnnotation(RequireApproval.class).role();
			}
			allowed = isUserInRole(requireApprovalRole);

		} catch (Exception ex) {
			logger.error("Cannot validate if user is allowed to approve required approval", ex);
		}
		return allowed;
	}

	public static void main(String[] args) throws Exception {
		RequiredApprovalService service = newServiceStatic(RequiredApprovalService.class, RequiredApproval.class);
		service.list();

		service.closeServices();

		System.out.println("Done");
	}

}
