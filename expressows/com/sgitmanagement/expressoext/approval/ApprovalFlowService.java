package com.sgitmanagement.expressoext.approval;

import com.sgitmanagement.expresso.dto.Query;
import com.sgitmanagement.expresso.dto.Query.Sort;
import com.sgitmanagement.expressoext.base.BaseEntityService;
import com.sgitmanagement.expressoext.util.MainUtil;

public class ApprovalFlowService extends BaseEntityService<ApprovalFlow> {
	public static void main(String[] args) throws Exception {
		ApprovalFlowService service = newServiceStatic(ApprovalFlowService.class, ApprovalFlow.class);

		System.out.println(service.list(new Query().setActiveOnly(true).setPageSize(50)));
		MainUtil.close();
	}

	@Override
	protected Sort[] getDefaultQuerySort() {
		return new Query.Sort[] { new Query.Sort("approvalOrder", Query.Sort.Direction.asc) };
	}
}
