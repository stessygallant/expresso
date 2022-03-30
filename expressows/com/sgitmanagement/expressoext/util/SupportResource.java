package com.sgitmanagement.expressoext.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import jakarta.servlet.ServletContext;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.sgitmanagement.expresso.util.Util;

@Path("/support")
public class SupportResource {
	final private static Logger logger = LoggerFactory.getLogger(SupportResource.class);

	@Context
	private ServletContext context;
	@Context
	private HttpServletRequest request;
	@Context
	private HttpServletResponse response;

	static private Stack<String> lastMails = new Stack<>();

	@POST
	@Path("mail")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void sendEmail(MultivaluedMap<String, String> formParams) throws Exception {
		String title = formParams.getFirst("title");
		String message = formParams.getFirst("message");
		message = URLDecoder.decode(message, StandardCharsets.UTF_8.name());

		// only send the same email once (avoid sending email in a loop)
		if (!lastMails.contains(message)) {
			lastMails.push(message);
			if (lastMails.size() > 100) {
				lastMails.pop();
			}
			logger.warn(title + " - " + message);
			// Mailer.INSTANCE.sendMail(title, message);
		}
	}

	@GET
	@Path("myIP")
	@Produces(MediaType.APPLICATION_JSON)
	public String getMyIPAddress() {
		Map<String, Object> map = new HashMap<>();
		map.put("ipAddress", Util.getIpAddress(request));
		map.put("internalIpAddress", Util.isInternalIpAddress(Util.getIpAddress(request)));
		return new Gson().toJson(map);
	}
}
