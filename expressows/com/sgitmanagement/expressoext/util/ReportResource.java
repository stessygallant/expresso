package com.sgitmanagement.expressoext.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import com.sgitmanagement.expresso.exception.InvalidCredentialsException;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseResource;

@Path("/report")
public class ReportResource extends BaseResource<ReportService> {

	public ReportResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(request, response, ReportService.class);
	}

	/**
	 * Execute a report from a report name<br>
	 * This method is called by using POST /report?action=execute
	 *
	 * @param formParams
	 * @throws Exception
	 */
	public void execute(MultivaluedMap<String, String> formParams) throws Exception {
		// first we must validate the token
		if (!SystemEnv.INSTANCE.isInProduction()) {
			String sessionToken = formParams.getFirst("sessionToken");
			if (!Util.equals(sessionToken, getRequest().getSession().getAttribute("sessionToken"))) {
				throw new InvalidCredentialsException();
			}
		}

		getService().executeReport(formParams);
	}

	/**
	 * Execute a report from a report name<br>
	 * This method is called by using GET /report/execute
	 */
	@GET
	@Path("execute")
	public void executeReport(@Context UriInfo ui, @Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		MultivaluedMap<String, String> params = ui.getQueryParameters();
		getService().executeReport(params);
	}
}