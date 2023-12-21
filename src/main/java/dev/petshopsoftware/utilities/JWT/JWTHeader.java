package dev.petshopsoftware.utilities.JWT;

import dev.petshopsoftware.utilities.JSON.JSON;

public class JWTHeader implements JSON {
	private final String alg;
	private final String typ;

	public JWTHeader(String alg, String typ) {
		this.alg = alg;
		this.typ = typ;
	}

	public String getAlg() {
		return alg;
	}

	public String getTyp() {
		return typ;
	}
}
