package com.sgitmanagement.expresso.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.spi.ExtendedExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.util.Util;

@Provider
public class ExceptionHandlerProvider implements ExtendedExceptionMapper<Throwable> {
	final protected Logger logger = LoggerFactory.getLogger(ExceptionHandlerProvider.class);

	@Override
	public boolean isMappable(Throwable throwable) {
		return true;
	}

	@Override
	public Response toResponse(Throwable ex) {
		Response response = Util.buildReponse(ex);
		if (ex instanceof BaseException && !(ex instanceof InternalException)) {
			if (((BaseException) ex).getCode() == 422) { // validation
				logger.debug(ex.toString());
			} else {
				logger.warn(ex.toString());
			}
		} else {
			logger.error(ex.toString(), ex);
		}
		return response;
	}
}
