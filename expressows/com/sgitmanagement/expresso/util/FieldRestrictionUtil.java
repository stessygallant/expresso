package com.sgitmanagement.expresso.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.base.FieldRestriction;

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
