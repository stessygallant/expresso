package com.sgitmanagement.expressoext.filter;

import java.io.IOException;

import com.sgitmanagement.expresso.base.PersistenceManager;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class PersistenceResponseServerFilter implements ContainerResponseFilter {
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		try {
			// commit before writing response (marshaling)
			PersistenceManager.getInstance().commit();
		} catch (Exception ex) {
			throw new IOException("Cannot commit", ex);
		}
	}
}