package com.sgitmanagement.expressoext.file;

import com.sgitmanagement.expressoext.base.BaseResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;

@Path("/fileLink")
public class FileLinkResource extends BaseResource<FileLinkService> {

	public FileLinkResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(request, response, FileLinkService.class);
	}

	@GET
	@Produces({ "application/pdf", "text/plain", "image/jpeg", "application/xml", "application/vnd.ms-excel" })
	@Path("/{applicationName}")
	public void downloadFile(@PathParam("applicationName") String applicationName) throws Exception {
		getService().downloadFile(applicationName);
	}
}