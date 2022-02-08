package com.sgitmanagement.expresso.exception;

public class WrongVersionException extends BaseException {
	private static final long serialVersionUID = 1L;

	public WrongVersionException() {
		// 412 means PRECONDITION_FAILED
		super(412, null);
	}
}
