package dev.petshopsoftware.utilities.HTTP.Server;

import dev.petshopsoftware.utilities.Util.ParsingMode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Route {
	String path();

	HTTPMethod method();

	ParsingMode parsingMode() default ParsingMode.JSON;
}
