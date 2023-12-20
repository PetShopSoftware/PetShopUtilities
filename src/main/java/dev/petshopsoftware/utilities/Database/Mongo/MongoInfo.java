package dev.petshopsoftware.utilities.Database.Mongo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MongoInfo {
    String identifier() default "id";

    String collection();
}
