package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expressoext.base.BaseEntityService;
import com.sgitmanagement.expressoext.util.MainUtil;

public class BasicUserService extends BaseEntityService<BasicUser> {

	public static void main(String[] args) throws Exception {
		BasicUserService service = newServiceStatic(BasicUserService.class, BasicUser.class);
		service.list();
		MainUtil.close();
	}
}
