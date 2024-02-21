package com.sgitmanagement.expressoext.approval;

import java.util.Date;
import java.util.List;

import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseEntityService;
import com.sgitmanagement.expressoext.security.JobTitle;
import com.sgitmanagement.expressoext.util.MainUtil;

public class ApprovalService extends BaseEntityService<Approval> {
	public static void main(String[] args) throws Exception {
		ApprovalService service = newServiceStatic(ApprovalService.class, Approval.class);

		System.out.println(service.list(new Query().setActiveOnly(true).setPageSize(50)));
		MainUtil.close();
	}

	@Override
	public Approval create(Approval e) throws Exception {
		if (e.getApprovalStatusId() == null) {
			e.setApprovalStatusId(newService(ApprovalStatusService.class, ApprovalStatus.class).get("NEW").getId());
		}
		if (e.getNextApprovalFlowId() == null) {
			e.setNextApprovalFlowId(getNextApprovalFlow(e, null).getId());
		}

		return super.create(e);
	}

	/**
	 * 
	 * @param approvable
	 * @return
	 * @throws Exception
	 */
	public boolean canApprove(Approvable approvable) throws Exception {
		return getFirstPossibleApprovalFlow(approvable) != null;
	}

	/**
	 * 
	 * @param resourceId
	 * @return
	 * @throws Exception
	 */
	private List<ApprovalFlow> getApprovalFlows(Integer resourceId) throws Exception {
		Query query = new Query().setActiveOnly(true);
		query.addSort(new Query.Sort("approvalOrder", Query.Sort.Direction.asc));
		query.addFilter(new Filter("resourceId", resourceId));
		return newService(ApprovalFlowService.class, ApprovalFlow.class).list(query);
	}

	/**
	 * 
	 * @param approvalFlows
	 * @param approval
	 * @return next approvalFlow if found, null otherwise
	 */
	private ApprovalFlow getNextApprovalFlow(Approval approval, ApprovalFlow currentApprovalFlow) throws Exception {
		List<ApprovalFlow> approvalFlows = getApprovalFlows(approval.getResourceId());
		for (int i = 0; i < approvalFlows.size(); i++) {
			ApprovalFlow approvalFlow = approvalFlows.get(i);
			// get the next approval_order (skip same order level)
			if (currentApprovalFlow == null || approvalFlow.getApprovalOrder() > currentApprovalFlow.getApprovalOrder()) {
				// the approvalFlow must contain the requested key
				if (approvalFlow.isRequestedApprovalKeyAllowed(approval.getRequestedApprovalKey())) {
					return approvalFlow;
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @param approvable
	 * @return
	 * @throws Exception
	 */
	private ApprovalFlow getFirstPossibleApprovalFlow(Approvable approvable) throws Exception {
		JobTitle userJobtitle = getUser().getJobTitle();

		Approval approval = approvable.getApproval();
		ApprovalFlow lastApprovalFlow = approval.getLastApprovalFlow();

		List<ApprovalFlow> approvalFlows = getApprovalFlows(approvable.getApproval().getResourceId());
		for (int i = 0; i < approvalFlows.size(); i++) {
			ApprovalFlow approvalFlow = approvalFlows.get(i);

			if (lastApprovalFlow == null || approvalFlow.getApprovalOrder() > lastApprovalFlow.getApprovalOrder()) {

				// verify request key
				if (approvalFlow.isRequestedApprovalKeyAllowed(approval.getRequestedApprovalKey())) {

					// verify job title
					if (approvalFlow.getJobTitleId().equals(userJobtitle.getId())) {

						// verify limit
						// if the limit is not respected, it can still approve it but it will be passed to the next approver
						// if (approvalFlow.isRequestedApprovalLimitAllowed(approval.getRequestedMinApprovalLimit())) {
						return approvalFlow;
						// }
					}

					if (approvalFlow.isMandatory()) {
						// we cannot skip this level
						break;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @param approvable
	 * @param approvalFlow
	 * @param approvalStatusPgmKey
	 * @param comment
	 * @throws Exception
	 */
	private Approval setNewStatus(Approvable approvable, ApprovalFlow approvalFlow, boolean approved, String comment, boolean autoApprove) throws Exception {
		if (approvalFlow != null || autoApprove) {
			Approval approval = approvable.getApproval();
			approval.setLastApprovalFlowId(autoApprove ? null : approvalFlow.getId());
			approval.setLastApprovalDate(new Date());
			approval.setLastApprovalUserId(getUser().getId());
			approval.setComment((Util.isNull(approval.getComment()) ? "" : approval.getComment() + ".\n") + comment);
			approval.setNextApprovalFlowId(null);

			String approvalStatusPgmKey;
			if (approved) {
				// verify limit
				if (autoApprove) {
					approvalStatusPgmKey = "APPROVED";
				} else if (approvalFlow.isRequestedApprovalLimitAllowed(approval.getRequestedMinApprovalLimit())) {
					approvalStatusPgmKey = "APPROVED";
				} else {
					approvalStatusPgmKey = "INAPPR";
					ApprovalFlow nextApprovalFlow = getNextApprovalFlow(approvable.getApproval(), approvalFlow);
					approval.setNextApprovalFlowId(nextApprovalFlow != null ? nextApprovalFlow.getId() : null);
				}
			} else {
				approvalStatusPgmKey = "REJECTED";
			}

			approval.setApprovalStatusId(newService(ApprovalStatusService.class, ApprovalStatus.class).get(approvalStatusPgmKey).getId());

			return update(approval);
		} else {
			throw new Exception(String.format("User [%s] is not an approver for approvable [%s]", getUser().getFullName(), approvable.getApproval().getResource().getName()));
		}
	}

	public Approval approve(Approvable approvable, String comment) throws Exception {
		ApprovalFlow approvalFlow = getFirstPossibleApprovalFlow(approvable);
		return setNewStatus(approvable, approvalFlow, true, comment, false);
	}

	public Approval autoApprove(Approvable approvable, String comment) throws Exception {
		return setNewStatus(approvable, null, true, comment, true);
	}

	public Approval reject(Approvable approvable, String comment) throws Exception {
		ApprovalFlow approvalFlow = getFirstPossibleApprovalFlow(approvable);
		return setNewStatus(approvable, approvalFlow, false, comment, false);
	}
}
