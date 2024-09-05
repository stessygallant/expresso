package com.sgitmanagement.expressoext.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.UserManager;
import com.sgitmanagement.expresso.exception.BaseException;
import com.sgitmanagement.expresso.exception.InvalidCredentialsException;
import com.sgitmanagement.expresso.util.ServerTimingUtil;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.security.AuthorizationHelper;
import com.sgitmanagement.expressoext.security.BasicUser;
import com.sgitmanagement.expressoext.security.User;
import com.sgitmanagement.expressoext.security.UserService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua_parser.Client;
import ua_parser.Parser;

public class AuthorizationFilter implements Filter {
	final private static Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);
	final private static Parser userAgentParser = new Parser();

	final private static String HEADER_VERSION = "X-Version";
	final private static String HEADER_APPLICATION_NAME = "X-AppName";

	@Override
	public void init(FilterConfig arg0) throws ServletException {

	}

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
		HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

		try {
			ServerTimingUtil.startTiming("AuthorizationFilter");
			ServerTimingUtil.startTiming("Authorization");

			// get the user
			BasicUser user = (BasicUser) UserManager.getInstance().getUser();

			boolean allowed = false;
			String action = null;
			List<String> resources = new ArrayList<>();

			try {
				// retrieve the action and put it in the request
				if (httpServletRequest.getMethod().equals("POST")) {
					action = Util.getParameterValue(httpServletRequest, "action");
				}

				if (action == null) {
					String method = httpServletRequest.getMethod();
					switch (method) {
					case "DELETE":
						action = "delete";
						break;
					case "PUT":
						action = "update";
						break;
					case "POST":
						action = "create";
						break;
					case "GET":
					default:
						action = "read";
						break;
					}
				}
				httpServletRequest.setAttribute("action", action);

				// get the security path and the resource from the URL
				String[] uris = httpServletRequest.getRequestURI().split("/");

				// URI must be at least 3 parts: /ws/auth/resource
				if (uris.length >= 3) {

					// 3 is the start index of the resource
					for (int i = 3; i < uris.length; i += 2) {
						String resource = uris[i];
						if (resource.equals("file")) {
							// this is for backward compatibility
							// ex: workorder/22924/document/50/file/Sec68L12NEMAOCR.png
							// we apply security only on workorder/22924/document/50
							break;
						} else {
							resources.add(resource);
						}
					}

					// verify the security path
					String securityPath = uris[2];
					if (securityPath.equals("public")) {
						// user is null here. Assume system in service
						user = UserService.newServiceStatic(UserService.class, User.class).getPublicUser();

						// the action must be publicly allowed (user is null)
						// verify that the user has the permission to execute the action on the resource
						allowed = AuthorizationHelper.isUserAllowed(user, action, resources);

					} else if (securityPath.equals("upload")) {
						// this is a patch because Kerberos does not support upload large file on Google Chrome

						// make sure that this is only used for upload
						if (action.equals("create")) {

							// there is the creationUserId in the url. use it
							String creationUserName = Util.getParameterValue(httpServletRequest, "creationUserName");
							if (creationUserName != null) {
								user = AuthorizationHelper.getUser(creationUserName);
								UserManager.getInstance().setUser(user);
							}

							allowed = AuthorizationHelper.isUserAllowed(user, action, resources);
							if (!allowed) {
								logger.error("WARNING: user [" + user.getUserName() + "] do not have access to [" + action + "] on [" + httpServletRequest.getRequestURI() + "]");
							}
						}
					} else if (securityPath.equals("websocket")) {
						// path contains the resource security path (not the resource path)
						resources = new ArrayList<>();
						for (int i = 3; i < uris.length; i++) {
							resources.add(uris[i]);
						}
						logger.debug("websocket  action:" + action + " resources:" + resources);
						allowed = AuthorizationHelper.isUserAllowed(user, action, resources);
					} else {
						// rest or sso

						// verify that the user has the permission to execute the action on the resource
						if (user == null) {
							throw new InvalidCredentialsException("No user found");
						}

						if (user.getTerminationDate() != null && user.getTerminationDate().before(new Date())) {
							// logger.warn("User [" + user.getUserName() + "] is terminated");
							// throw new InvalidCredentialsException("User is terminated");
						}

						allowed = AuthorizationHelper.isUserAllowed(user, action, resources);
					}
				}
				ServerTimingUtil.endTiming();

				ServerTimingUtil.startTiming("UserAgentParser");
				String version = httpServletRequest.getHeader(HEADER_VERSION);
				String appNAme = httpServletRequest.getHeader(HEADER_APPLICATION_NAME);
				String ip = Util.getIpAddress(httpServletRequest);
				String userAgent = httpServletRequest.getHeader("User-Agent");

				// caution: userAgentClient takes up to 20ms
				Client userAgentClient = userAgentParser.parse(userAgent);
				// System.out.println(userAgentClient.userAgent.family); // => "Mobile Safari"
				// System.out.println(userAgentClient.userAgent.major); // => "5"
				// System.out.println(userAgentClient.userAgent.minor); // => "1"
				// System.out.println(userAgentClient.os.family); // => "iOS"
				// System.out.println(userAgentClient.os.major); // => "5"
				// System.out.println(userAgentClient.os.minor); // => "1"
				// System.out.println(userAgentClient.device.family); // => "iPhone"
				String browser = (userAgentClient != null && userAgentClient.userAgent != null ? userAgentClient.userAgent.family : "");
				String browserVersion = (userAgentClient != null && userAgentClient.userAgent != null ? userAgentClient.userAgent.major + "." + userAgentClient.userAgent.minor : "");
				String os = (userAgentClient != null && userAgentClient.os != null ? userAgentClient.os.family : "");

				String msg = String.format("%10s %10s %s %s %s %s%s - [%s %s/%s]", //
						(ip != null ? ip : "n/a"), //
						(user != null ? user.getUserName() : "n/a"), //
						version, appNAme, httpServletRequest.getMethod(), //
						httpServletRequest.getRequestURI(), //
						(action != null && !action.equals("read") && !action.equals("create") ? " action=" + action : ""), //
						browser, //
						browserVersion, //
						os);
				ServerTimingUtil.endTiming();
				if (allowed) {

					// logger.info(String.format("START %10s %10s %s %s %s %s%s", (ip != null ? ip : "n/a"), (user != null ? user.getUserName() : "n/a"), version, appNAme, request.getMethod(),
					// request.getRequestURI(), (action != null && !action.equals("read") && !action.equals("create") ? " action=" + action : "")));

					// pass the request along the filter chains
					ServerTimingUtil.startTiming("Service");
					long startTime = new Date().getTime();
					chain.doFilter(servletRequest, servletResponse);
					long endTime = new Date().getTime();
					ServerTimingUtil.endTiming();

					if (httpServletRequest.getRequestURI() != null && (httpServletRequest.getRequestURI().endsWith("systemMessage") || httpServletRequest.getRequestURI().endsWith("data"))) {
						// do not logs those calls
						// } else if (user != null && user.isGenericAccount() && action.equals("read")) {
						// // do not log calls from TV
					} else {
						logger.info(String.format("%s (ms:%d)", msg, (endTime - startTime)));
					}
				} else {
					if (user == null || user.getTerminationDate() != null) {
						httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
					} else {
						msg = String.format("INVALID %s (%s)", msg, (action + " -> " + resources + ": FORBIDDEN"));

						String[] pentestIPs = new String[] { "147.253.144.195", "144.217.204.215" };
						if (ip != null && Arrays.stream(pentestIPs).anyMatch(ip::equals)) {
							logger.warn(msg);
						} else {
							logger.error(msg);
						}

						httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
					}
				}
			} finally {

				// clear the request
				httpServletRequest.removeAttribute("action");

				// must clear the ThreadLocal
				AuditTrailInterceptor.close();
			}
			ServerTimingUtil.endTiming();
		} catch (BaseException e) {
			// in this case, we cannot return an exception
			HttpServletResponse resp = (HttpServletResponse) servletResponse;
			resp.setStatus(e.getCode());
			PrintWriter pw = resp.getWriter();
			pw.write(e.getDescription());
			pw.flush();
			pw.close();
		} catch (Exception e) {
			logger.error("Cannot process request", e);
			HttpServletResponse resp = (HttpServletResponse) servletResponse;
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			PrintWriter pw = resp.getWriter();
			pw.write("" + e);
			pw.flush();
			pw.close();
		}
	}
}
