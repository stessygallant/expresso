package com.sgitmanagement.expresso.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ValidationException extends BaseException {
	private static final long serialVersionUID = 1L;
	// 422 means Unprocessable Entity
	final public static int HTTPCODE_UNPROCESSABLE_ENTITY = 422;

	public ValidationException(String message, Exception cause) {

		super(HTTPCODE_UNPROCESSABLE_ENTITY, message, cause);

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

	@Override
	public String toString() {
		StringWriter errors = new StringWriter();
		if (getCause() != null) {
			getCause().printStackTrace(new PrintWriter(errors));
		}
		String params = "";
		if (getParams() != null) {
			for (String name : getParams().keySet()) {
				params += "[" + name + "=" + getParams().get(name).toString() + "]";
			}
		}
		return "ValidationException [code=" + this.getCode() + ", description=" + this.getDescription() + ", params=" + params + "] at " + new Date() + ": " + errors.toString();
	}
}
