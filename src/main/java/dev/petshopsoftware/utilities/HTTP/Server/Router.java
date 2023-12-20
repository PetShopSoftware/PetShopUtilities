package dev.petshopsoftware.utilities.HTTP.Server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Router {
	String value();
}
