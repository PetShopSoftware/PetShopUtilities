package dev.petshopsoftware.utilities.Database.Mongo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MongoInfo {
	String identifier() default "id";

	String collection();

	MongoIndexData[] indexes() default {};

	@interface MongoIndexData {
		String field();

		boolean unique() default false;

		boolean sparse() default false;

		boolean ascending() default true;

		boolean text() default false;

		boolean partial() default false;

		long ttl() default -1;
	}
}
