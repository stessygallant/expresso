package com.sgitmanagement.expressoext.approval;

import com.sgitmanagement.expressoext.base.BaseOptionService;
import com.sgitmanagement.expressoext.util.MainUtil;

public class ApprovalStatusService extends BaseOptionService<ApprovalStatus> {
	public static void main(String[] args) throws Exception {
		ApprovalStatusService service = newServiceStatic(ApprovalStatusService.class, ApprovalStatus.class);

		System.out.println(service.list());
		MainUtil.close();
	}
}
