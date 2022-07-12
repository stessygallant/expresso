package com.sgitmanagement.expressoext.filter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import com.sgitmanagement.expresso.util.FieldRestrictionUtil;
import com.sgitmanagement.expressoext.security.User;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class FieldRestrictionFilter implements Filter {
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// get the user from the request
		User user = (User) request.getAttribute("user");

		// get the user roles
		Set<String> userRoles = user.getRoles().stream().map(role -> role.getPgmKey()).collect(Collectors.toSet());

		// use the FieldRestrictionUtil instance to apply the restrictions if needed
		FieldRestrictionUtil.INSTANCE.doFilter(request, response, chain, userRoles);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}
}
