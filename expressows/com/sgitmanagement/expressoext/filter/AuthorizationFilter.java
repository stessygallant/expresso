package com.sgitmanagement.expressoext.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.UserManager;
import com.sgitmanagement.expresso.exception.BaseException;
import com.sgitmanagement.expresso.exception.InvalidCredentialsException;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.security.AuthorizationHelper;
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
		HttpServletRequest request = (HttpServletRequest) servletRequest;

		try {
			// get the user
			User user = (User) UserManager.getInstance().getUser();

			boolean allowed = false;
			String action = null;
			List<String> resources = new ArrayList<>();

			try {
				// retrieve the action and put it in the request
				if (request.getMethod().equals("POST")) {
					action = Util.getParameterValue(request, "action");
				}

				if (action == null) {
					String method = request.getMethod();
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
				request.setAttribute("action", action);

				// get the security path and the resource from the URL
				String[] uris = request.getRequestURI().split("/");

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
							String creationUserName = Util.getParameterValue(request, "creationUserName");
							if (creationUserName != null) {
								user = AuthorizationHelper.getUser(creationUserName);
								UserManager.getInstance().setUser(user);
							}

							allowed = AuthorizationHelper.isUserAllowed(user, action, resources);
							if (!allowed) {
								logger.error("WARNING: user [" + user.getUserName() + "] do not have access to [" + action + "] on [" + request.getRequestURI() + "]");
							}
						}
					} else if (securityPath.equals("websocket")) {
						allowed = AuthorizationHelper.isUserAllowed(user, action, resources);
					} else {
						// rest or sso

						// verify that the user has the permission to execute the action on the resource
						if (user == null) {
							throw new InvalidCredentialsException("No user found");
						}

						allowed = AuthorizationHelper.isUserAllowed(user, action, resources);
					}
				}

				if (allowed) {
					String version = request.getHeader(HEADER_VERSION);
					String appNAme = request.getHeader(HEADER_APPLICATION_NAME);
					String ip = Util.getIpAddress(request);
					String userAgent = request.getHeader("User-Agent");

					// logger.debug(String.format("START %10s %10s %s %s %s %s%s", (ip != null ? ip : "n/a"),
					// (user != null ? user.getUserName() : "n/a"), version, appNAme, request.getMethod(),
					// request.getRequestURI(),
					// (action != null && !action.equals("read") && !action.equals("create") ? " action=" + action
					// : "")));

					// pass the request along the filter chains
					long startTime = new Date().getTime();
					chain.doFilter(servletRequest, servletResponse);

					long endTime = new Date().getTime();

					if (user != null && user.isGenericAccount() && action.equals("read")) {
						// do not log calls from TV
					} else if (request.getRequestURI() != null && request.getRequestURI().endsWith("systemMessage")) {
						// do not logs those calls
					} else {
						try {
							Client userAgentClient = new Parser().parse(userAgent);

							// System.out.println(userAgentClient.userAgent.family); // => "Mobile Safari"
							// System.out.println(userAgentClient.userAgent.major); // => "5"
							// System.out.println(userAgentClient.userAgent.minor); // => "1"
							// System.out.println(userAgentClient.os.family); // => "iOS"
							// System.out.println(userAgentClient.os.major); // => "5"
							// System.out.println(userAgentClient.os.minor); // => "1"
							// System.out.println(userAgentClient.device.family); // => "iPhone"

							logger.info(String.format("%10s %10s %s %s %s %s%s - [%s %s/%s] (ms:%d)", (ip != null ? ip : "n/a"), (user != null ? user.getUserName() : "n/a"), version, appNAme,
									request.getMethod(), request.getRequestURI(), (action != null && !action.equals("read") && !action.equals("create") ? " action=" + action : ""),
									(userAgentClient != null && userAgentClient.userAgent != null ? userAgentClient.userAgent.family : ""), //
									(userAgentClient != null && userAgentClient.userAgent != null ? userAgentClient.userAgent.major + "." + userAgentClient.userAgent.minor : ""), //
									(userAgentClient != null && userAgentClient.os != null ? userAgentClient.os.family : ""), //
									(endTime - startTime)));
						} catch (Exception ex) {
							logger.warn("Cannot log info: " + ex);
						}
					}
				} else {
					logger.warn(user + ": " + action + " -> " + resources + ": FORBIDDEN");

					HttpServletResponse resp = (HttpServletResponse) servletResponse;
					resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
				}

			} finally {
				// clear the request
				request.removeAttribute("action");

				// must clear the ThreadLocal
				AuditTrailInterceptor.clear();
			}
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
