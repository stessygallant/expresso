package com.sgitmanagement.expresso.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker interface to declare the list of field names for the unique constraints for the entity. We do not use the UniqueConstraints of the Table annotation because this one required the column names
 * (and we need the field names)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface UniqueFieldConstraints {
	String[] fieldNames();
}
