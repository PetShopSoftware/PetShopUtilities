package dev.petshopsoftware.utilities.Socket;

import dev.petshopsoftware.utilities.Util.ParsingMode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SocketChannel {
	String value();

	int priority() default 0;

	ParsingMode parsingMode() default ParsingMode.RAW;
}
