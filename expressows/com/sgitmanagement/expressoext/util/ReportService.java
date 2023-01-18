package com.sgitmanagement.expressoext.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expressoext.base.BaseService;
import com.sgitmanagement.expressoext.security.User;

import jakarta.ws.rs.core.MultivaluedMap;

public class ReportService extends BaseService {

	public void executeReport(MultivaluedMap<String, String> params) throws Exception {
		User user = getUser();

		String reportName = params.getFirst("reportName");
		String resourceSecurityPath = params.getFirst("resourceName");
		String[] resources = resourceSecurityPath.split("/");

		boolean allowed = isUserAllowed("read", Arrays.asList(resources));
		if (!allowed) {
			throw new ForbiddenException("Cannot execute the report");
		} else {
			Map<String, String> paramMap = new HashMap<>();

			if (params != null) {
				for (Map.Entry<String, List<String>> entry : params.entrySet()) {
					if (entry.getValue() != null && entry.getValue().size() > 0 && entry.getValue().get(0) != null && entry.getValue().get(0).length() > 0) {
						// System.out.println("Adding " + entry.getKey() + "=[" + entry.getValue().get(0) + "]");
						paramMap.put(entry.getKey(), entry.getValue().get(0));
					}
				}
			}
			ReportUtil.INSTANCE.executeReport(user, reportName, reportName, paramMap, getResponse(), getResponse().getOutputStream());
		}
	}
}
