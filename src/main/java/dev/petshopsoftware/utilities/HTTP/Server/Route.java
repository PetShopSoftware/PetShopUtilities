package dev.petshopsoftware.utilities.HTTP.Server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Route {
	String path();

	HTTPMethod method();

//	ParsingMode requestParsingMode() default ParsingMode.JSON;
}
