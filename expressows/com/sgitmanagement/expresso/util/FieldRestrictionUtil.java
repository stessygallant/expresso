package com.sgitmanagement.expresso.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sgitmanagement.expresso.base.FieldRestriction;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public enum FieldRestrictionUtil {
	INSTANCE;

	final private Logger logger = LoggerFactory.getLogger(FieldRestrictionUtil.class);

	// <Entity, <Field,Role>>
	private Map<String, Map<String, String>> fieldRestrictionEntityMap;

	private FieldRestrictionUtil() {
		if (fieldRestrictionEntityMap == null) {
			try {
				init();
			} catch (Exception ex) {
				logger.error("Cannot instantiate FieldRestrictionUtil", ex);
			}
		}
	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @param chain
	 * @throws IOException
	 * @throws ServletException
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain, Set<String> userRoles) throws IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		if (fieldRestrictionEntityMap.isEmpty() || (httpServletRequest.getHeader("Accept") != null
				&& !(httpServletRequest.getHeader("Accept").indexOf("application/json") != -1 || httpServletRequest.getHeader("Accept").indexOf("*/*") != -1))) {
			chain.doFilter(request, response);
		} else {
			// pass the request along the filter chain
			HttpServletResponse httpServletResponse = new FieldRestrictionFieldResponseWrapper((HttpServletResponse) response);
			chain.doFilter(request, httpServletResponse);

			try {
				byte[] bytes = ((ByteArrayServletOutputStream) httpServletResponse.getOutputStream()).toByteArray();
				if (bytes.length > 0 && httpServletResponse.getStatus() == HttpServletResponse.SC_OK) {
					if (bytes[0] == '[' || bytes[0] == '{') {
						String jsonResponse = new String(bytes);
						Date date = new Date();

						JsonElement rootJsonElement = null;
						for (String resourceName : fieldRestrictionEntityMap.keySet()) {
							if (jsonResponse.indexOf("\"type\":\"" + resourceName + "\"") != -1) {
								logger.debug("Removing restricted fields in [" + resourceName + "]");

								Map<String, String> fieldMap = fieldRestrictionEntityMap.get(resourceName);
								if (rootJsonElement == null) {
									// build the Json object
									rootJsonElement = new Gson().fromJson(jsonResponse, JsonElement.class);
								}

								// parse the JSON and remove the restricted field if required
								removeRestrictedFields(userRoles, resourceName, fieldMap, rootJsonElement);

								jsonResponse = new Gson().toJson(rootJsonElement);
							}
						}

						// write back the JSON reponse
						bytes = jsonResponse.getBytes();

						logger.debug("FieldSecurityFilter time [" + (new Date().getTime() - date.getTime()) + "]");
					}
				}
				response.setContentLength(bytes.length);
				response.getOutputStream().write(bytes);
			} catch (Exception ex) {
				logger.error("Error applying FieldSecurityFilter", ex);
			}
		}
	}

	/**
	 * 
	 * @param profiles
	 * @param resourceName
	 * @param fieldMap
	 * @param rootJsonElement
	 */
	private void removeRestrictedFields(Set<String> userRoles, String resourceName, Map<String, String> fieldMap, JsonElement rootJsonElement) {
		if (rootJsonElement instanceof JsonObject) {
			// parse the current object
			JsonObject entityJsonObject = (JsonObject) rootJsonElement;

			// then verify if the current object is the resource
			if (entityJsonObject.has("type") && !entityJsonObject.get("type").isJsonNull() && entityJsonObject.get("type").getAsString().equals(resourceName)) {
				// remove all member if fieldMap if required
				for (Entry<String, String> fieldEntry : fieldMap.entrySet()) {
					String fieldName = fieldEntry.getKey();
					String role = fieldEntry.getValue();
					if (entityJsonObject.has(fieldName)) {
						// remove the field if user not in role
						if (!userRoles.contains(role)) {
							entityJsonObject.remove(fieldName);
						}
					}
				}
			}

			// parse sub objects
			for (Entry<String, JsonElement> jsonObjectEntry : entityJsonObject.entrySet()) {
				removeRestrictedFields(userRoles, resourceName, fieldMap, jsonObjectEntry.getValue());
			}
		} else if (rootJsonElement instanceof JsonArray) {
			for (JsonElement jsonElement : (JsonArray) rootJsonElement) {
				removeRestrictedFields(userRoles, resourceName, fieldMap, jsonElement);
			}
		} else {
			// primitive, ignore
		}
	}

	/**
	 * 
	 */
	static class FieldRestrictionFieldResponseWrapper extends HttpServletResponseWrapper {
		private ServletOutputStream outputStream;

		public FieldRestrictionFieldResponseWrapper(HttpServletResponse response) throws IOException {
			super(response);
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (outputStream == null) {
				outputStream = new ByteArrayServletOutputStream();
			}
			return outputStream;
		}

		@Override
		public String toString() {
			try {
				return new String(((ByteArrayServletOutputStream) getOutputStream()).toByteArray());
			} catch (IOException e) {
				return null;
			}
		}
	}

	/**
	 * 
	 */
	static public class ByteArrayServletOutputStream extends ServletOutputStream {
		protected final ByteArrayOutputStream buf;

		public ByteArrayServletOutputStream() {
			buf = new ByteArrayOutputStream();
		}

		public byte[] toByteArray() {
			return buf.toByteArray();
		}

		@Override
		public void write(int b) {
			buf.write(b);
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(WriteListener listener) {
			// noop
		}
	}

	/**
	 * 
	 * @param resourceName
	 * @return
	 */
	public Map<String, String> getFieldRestrictionMap(String resourceName) {
		return fieldRestrictionEntityMap.get(resourceName);
	}

	/**
	 * 
	 * @param resourceName
	 * @param fieldName
	 * @return
	 */
	public String getFieldRestrictionRole(String resourceName, String fieldName) {
		return fieldRestrictionEntityMap.get(resourceName) != null ? fieldRestrictionEntityMap.get(resourceName).get(fieldName) : null;
	}

	/**
	 * 
	 * @throws Exception
	 */
	private void init() throws Exception {
		fieldRestrictionEntityMap = new HashMap<>();

		// get all entity with FieldRestriction
		Set<Class<?>> fieldRestrictionEntityClasses = new Reflections((Object[]) new String[] { "ca.cezinc.expressoservice", "com.sgitmanagement" }).getTypesAnnotatedWith(FieldRestriction.class);

		// build a map <resourceName, aggregatableServiceClass>
		for (Class<?> fieldRestrictionEntityClass : fieldRestrictionEntityClasses) {
			String resourceName = StringUtils.uncapitalize(fieldRestrictionEntityClass.getSimpleName());
			String entityRestrictedRole = fieldRestrictionEntityClass.getAnnotation(FieldRestriction.class).role();

			logger.info("FieldRestricted entity [" + resourceName + "] role[" + entityRestrictedRole + "]");

			// get all restricted fields in the entity
			Map<String, String> fieldMap = new HashMap<>();
			List<Field> fields = FieldUtils.getFieldsListWithAnnotation(fieldRestrictionEntityClass, FieldRestriction.class);
			for (Field field : fields) {
				String role = field.getAnnotation(FieldRestriction.class).role();
				if (role == null || role.trim().length() == 0) {
					role = entityRestrictedRole;
				}
				logger.debug("  FieldRestricted field [" + field.getName() + "] role[" + role + "]");
				fieldMap.put(field.getName(), role);
			}

			fieldRestrictionEntityMap.put(resourceName, fieldMap);
		}
	}

	public static void main(String[] args) throws Exception {
		System.out.println(FieldRestrictionUtil.INSTANCE.getFieldRestrictionMap("vendor"));
	}
}
