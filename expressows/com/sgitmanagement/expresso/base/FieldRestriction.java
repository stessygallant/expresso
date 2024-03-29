package com.sgitmanagement.expresso.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)

// FieldRestriction will forbid the field to be audited
@Target({ ElementType.TYPE, ElementType.FIELD })
public @interface FieldRestriction {
	public String role() default "";// only mandatory for Type
}
