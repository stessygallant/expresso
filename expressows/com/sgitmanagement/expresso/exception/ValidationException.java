package com.sgitmanagement.expresso.exception;

import java.util.HashMap;
import java.util.Map;

public class ValidationException extends BaseException {
	private static final long serialVersionUID = 1L;

	public ValidationException(String message, Exception cause) {
		// 422 means Unprocessable Entity
		super(422, message, cause);

		if (cause != null) {
			Map<String, Object> params = new HashMap<>();
			params.put("exception", cause.toString());
			setParams(params);
		}
	}

	public ValidationException(String message) {
		this(message, (Exception) null);
	}

	public ValidationException(String message, Map<String, Object> params) {
		this(message);
		setParams(params);
	}

	public ValidationException(String message, String paramName, Object paramValue) {
		this(message);

		Map<String, Object> params = new HashMap<>();
		params.put(paramName, paramValue);
		setParams(params);
	}
}
