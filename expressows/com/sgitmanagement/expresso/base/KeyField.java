package com.sgitmanagement.expresso.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)

@Target({ ElementType.FIELD })
public @interface KeyField {
	public int padding() default 0;

	public int length() default 0;

	public String prefix() default "";

	// 0: means digits
	// x: letter lowercase
	// X: letter uppercase
	// -: mandatory dash
	public String format() default "";
}
