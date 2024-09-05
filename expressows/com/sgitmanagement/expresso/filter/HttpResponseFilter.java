package com.sgitmanagement.expresso.filter;

import java.io.IOException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.PersistenceManager;
import com.sgitmanagement.expresso.util.ServerTimingUtil;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class HttpResponseFilter implements ContainerResponseFilter {
	static final private Logger logger = LoggerFactory.getLogger(HttpResponseFilter.class);

	@Override
	public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
		try {
			// commit before writing response (marshaling), but do not close
			PersistenceManager.getInstance().commit();

			if (ServerTimingUtil.serverTiming.get() != null) {
				// this shall remove Total entry
				ServerTimingUtil.complete();

				// Add an header for each
				String serverTimingHeader = ServerTimingUtil.serverTiming.get().entrySet().stream().map(entry -> entry.getKey() + ";dur=" + entry.getValue()).collect(Collectors.joining(", "));

				// make sure not to throw HeadersTooLargeException
				if (serverTimingHeader.length() > 1000) {
					// remove dbQuery
					serverTimingHeader = ServerTimingUtil.serverTiming.get().entrySet().stream().filter(entry -> !entry.getKey().endsWith("dbQuery"))
							.map(entry -> entry.getKey() + ";dur=" + entry.getValue()).collect(Collectors.joining(", "));

					if (serverTimingHeader.length() > 1000) {
						// only total
						serverTimingHeader = ServerTimingUtil.serverTiming.get().entrySet().stream().filter(entry -> entry.getKey().equals("Total"))
								.map(entry -> entry.getKey() + ";dur=" + entry.getValue()).collect(Collectors.joining(", "));
					}
				}
				// logger.debug("HttpResponseFilter: " + serverTimingHeader);

				// https://w3c.github.io/server-timing/
				containerResponseContext.getHeaders().add("Server-Timing", serverTimingHeader);
				containerResponseContext.getHeaders().add("Timing-Allow-Origin", "*");

				// clear memory
				ServerTimingUtil.clear();
			}
		} catch (Exception ex) {
			logger.error("Error committing", ex);
			PersistenceManager.getInstance().rollback();
		}
	}
}
