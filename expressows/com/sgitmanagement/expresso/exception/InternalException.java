package com.sgitmanagement.expresso.exception;

import javax.ws.rs.core.Response;

public class InternalException extends BaseException {
	private static final long serialVersionUID = 1L;

	public InternalException(String message, Throwable cause) {
		super(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), message, cause, "Problème interne de l'application. Réessayer plus tard.");
	}
}
