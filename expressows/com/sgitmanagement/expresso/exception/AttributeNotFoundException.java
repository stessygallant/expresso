package com.sgitmanagement.expresso.exception;

public class AttributeNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public AttributeNotFoundException() {
		super();
	}

	public AttributeNotFoundException(String message) {
		super(message);
	}
}