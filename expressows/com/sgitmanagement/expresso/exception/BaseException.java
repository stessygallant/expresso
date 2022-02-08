package com.sgitmanagement.expresso.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class BaseException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private int code;
	private String description;
	private Map<String, Object> params;

	public BaseException() {
	}

	public BaseException(int code) {
		super();
		this.code = code;
	}

	public BaseException(int code, String message) {
		super(message);
		this.code = code;
		this.description = message;
	}

	public BaseException(int code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
		this.description = message;
	}

	public BaseException(int code, String message, Throwable cause, String description) {
		super(message, cause);
		this.code = code;
		this.description = description;
	}

	public int getCode() {
		return code;
	}

	public BaseException setCode(int code) {
		this.code = code;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public BaseException setDescription(String description) {
		this.description = description;
		return this;
	}

	public BaseException addParam(String key, Object value) {
		if (this.params == null) {
			this.params = new HashMap<>();
		}
		this.params.put(key, value);
		return this;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public BaseException setParams(Map<String, Object> params) {
		this.params = params;
		return this;
	}

	@Override
	public String toString() {
		StringWriter errors = new StringWriter();
		if (getCause() != null) {
			getCause().printStackTrace(new PrintWriter(errors));
		}
		return "BaseException [code=" + code + ", description=" + description + ", message=" + getMessage() + "] at " + new Date() + ": " + errors.toString();
	}
}
