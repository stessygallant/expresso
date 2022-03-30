package com.sgitmanagement.expresso.exception;

import jakarta.servlet.http.HttpServletResponse;

public class ForbiddenException extends BaseException {
	private static final long serialVersionUID = 1L;

	public ForbiddenException(String message) {
		super(HttpServletResponse.SC_FORBIDDEN, message);
	}

	public ForbiddenException() {
		this("userNotAllowed");
	}

	public ForbiddenException(String action, String resource) {
		this("User not allowed to [" + action + "] on [" + resource + "]");
	}
}
