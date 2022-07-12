package com.sgitmanagement.expressoext.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

@Path("/support")
public class SupportResource extends BaseResource<SupportService> {

	public SupportResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(request, response, SupportService.class);
	}

	@POST
	@Path("mail")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void sendEmail(MultivaluedMap<String, String> formParams) throws Exception {
		String title = URLDecoder.decode(formParams.getFirst("title"), StandardCharsets.UTF_8.name());
		String message = URLDecoder.decode(formParams.getFirst("message"), StandardCharsets.UTF_8.name());
		getService().sendSupportEmail(title, message);
	}

	@GET
	@Path("myIP")
	@Produces(MediaType.APPLICATION_JSON)
	public String getMyIPAddress() throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("ipAddress", Util.getIpAddress(request));
		map.put("internalIpAddress", Util.isInternalIpAddress(Util.getIpAddress(request)));
		return new Gson().toJson(map);
	}

	@GET
	@Path("printer")
	@Produces(MediaType.APPLICATION_JSON)
	public String getAvailablePrinterNames() throws Exception {
		return new Gson().toJson(getService().getPrinterNames());
	}

}
