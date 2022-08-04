package com.sgitmanagement.expresso.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)

@Target({ ElementType.FIELD })
public @interface KeyFieldReference {
	public String resourceName() default ""; // only mandatory if the field name (minus 'No') is not the same as the resource

	public String keyFieldName() default ""; // only mandatory if the field name is not the same as the keyField
}
