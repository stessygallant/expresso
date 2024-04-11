package com.sgitmanagement.expresso.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ResourceEventListener {
	String resourceName() default "";

	String[] resourceNames() default "";

	boolean priority() default false;
}
