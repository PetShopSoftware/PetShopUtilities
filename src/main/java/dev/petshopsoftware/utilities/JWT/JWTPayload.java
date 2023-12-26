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

	public void setSub(String sub) {
		this.sub = sub;
	}

	public String getIss() {
		return iss;
	}

	public void setIss(String iss) {
		this.iss = iss;
	}

	public long getExp() {
		return exp;
	}

	public void setExp(long exp) {
		this.exp = exp;
	}

	public long getIat() {
		return iat;
	}

	public void setIat(long iat) {
		this.iat = iat;
	}

	public String getJti() {
		return jti;
	}

	public void setJti(String jti) {
		this.jti = jti;
	}
}
