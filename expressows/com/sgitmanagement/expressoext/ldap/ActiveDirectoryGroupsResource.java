package com.sgitmanagement.expressoext.ldap;

import com.sgitmanagement.expressoext.base.BaseResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/activeDirectoryGroup")
public class ActiveDirectoryGroupsResource extends BaseResource<ActiveDirectoryGroupService> {
	public ActiveDirectoryGroupsResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(request, response, ActiveDirectoryGroupService.class);
	}

	// @GET
	// @Produces(MediaType.APPLICATION_JSON)
	// public BudgetSummary getBudgetSummary(@QueryParam("query") String queryJSONString) throws Exception {
	//
	// Query query = Query.getQuery(queryJSONString);
	// BudgetSummary budgetSummary = getService().getBudgetSummary(query);
	//
	// return budgetSummary;
	// }
}
