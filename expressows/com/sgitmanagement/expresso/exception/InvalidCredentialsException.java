package com.sgitmanagement.expresso.exception;

import javax.ws.rs.core.Response;

public class InvalidCredentialsException extends BaseException {
	private static final long serialVersionUID = 1L;

	public InvalidCredentialsException() {
		// use the message from the web site to return to the user
		super(Response.Status.UNAUTHORIZED.getStatusCode(), "unauthorized");
	}

	public InvalidCredentialsException(String message) {
		// use the message from the web site to return to the user
		super(Response.Status.UNAUTHORIZED.getStatusCode(), message);
	}
}
