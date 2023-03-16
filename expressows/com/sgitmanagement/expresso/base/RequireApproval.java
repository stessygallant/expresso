package com.sgitmanagement.expresso.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)

@Target({ ElementType.TYPE, ElementType.FIELD })
public @interface RequireApproval {
	public String role() default ""; // only mandatory for Type

	public String descriptionFieldName() default "description"; // only mandatory for Type

	public String additionalInfoFieldName() default ""; // only mandatory for Type
}
