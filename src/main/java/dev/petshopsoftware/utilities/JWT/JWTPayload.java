package dev.petshopsoftware.utilities.JWT;

import dev.petshopsoftware.utilities.JSON.JSON;

public class JWTPayload implements JSON {
	private String iss;
	private String sub;
	private long exp;
	private long iat;
	private String jti;

	public JWTPayload() {
	}

	public JWTPayload(String iss, String sub, long exp, long iat, String jti) {
		this.iss = iss;
		this.sub = sub;
		this.exp = exp;
		this.iat = iat;
		this.jti = jti;
	}


	public String getSub() {
		return sub;
	}

	public String getIss() {
		return iss;
	}

	public long getExp() {
		return exp;
	}

	public long getIat() {
		return iat;
	}

	public String getJti() {
		return jti;
	}
}
