package dev.petshopsoftware.utilities.HTTP.Server;

public enum HTTPMethod {
	GET(false),
	POST(true),
	PUT(true),
	DELETE(false),
	PATCH(true),
	HEAD(false),
	OPTIONS(false),
	TRACE(false);

	private final boolean hasBody;

	HTTPMethod(boolean hasBody) {
		this.hasBody = hasBody;
	}

	public final boolean hasBody() {
		return hasBody;
	}
}
