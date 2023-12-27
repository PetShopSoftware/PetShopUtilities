package dev.petshopsoftware.utilities.JSON;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.HttpURLConnection;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JSONResponse implements JSON {
	// 2XX code range
	public static final JSONResponse OK = new JSONResponse(HttpURLConnection.HTTP_OK, null, null);
	public static final JSONResponse CREATED = new JSONResponse(HttpURLConnection.HTTP_CREATED, null, null);
	public static final JSONResponse ACCEPTED = new JSONResponse(HttpURLConnection.HTTP_ACCEPTED, null, null);
	public static final JSONResponse NO_CONTENT = new JSONResponse(HttpURLConnection.HTTP_NO_CONTENT, null, null);

	// 4XX code range
	public static final JSONResponse BAD_REQUEST = new JSONResponse(HttpURLConnection.HTTP_BAD_REQUEST, null, null);
	public static final JSONResponse INVALID_JSON = new JSONResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid JSON payload.", null);
	public static final JSONResponse INVALID_BODY = new JSONResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid payload.", null);
	public static final JSONResponse UNAUTHORIZED = new JSONResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "Account not authorized.", null);
	public static final JSONResponse FORBIDDEN = new JSONResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "Access forbidden.", null);
	public static final JSONResponse NOT_FOUND = new JSONResponse(HttpURLConnection.HTTP_NOT_FOUND, "Resource not found.", null);
	public static final JSONResponse CONFLICT = new JSONResponse(HttpURLConnection.HTTP_CONFLICT, null, null);
	public static final JSONResponse TIMEOUT = new JSONResponse(420, "Wait a few seconds.", null);

	// 5XX code range
	public static final JSONResponse INTERNAL_ERROR = new JSONResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "An internal error occurred.", null);
	public static final JSONResponse NOT_IMPLEMENTED = new JSONResponse(HttpURLConnection.HTTP_NOT_IMPLEMENTED, "Service not implemented.", null);
	public static final JSONResponse UNAVAILABLE = new JSONResponse(HttpURLConnection.HTTP_UNAVAILABLE, "Service temporarily unavailable.", null);

	protected final Integer code;
	protected final String message;
	protected final Object data;

	public JSONResponse(Integer code, String message, Object data) {
		this.code = code;
		this.message = message;
		this.data = data;
	}

	public JSONResponse() {
		this(null, null, null);
	}

	public static JSONResponse findResponse(int code) {
		return null;
	}

	public JSONResponse code(Integer code) {
		return new JSONResponse(code, this.message, this.data);
	}

	public JSONResponse message(String message) {
		return new JSONResponse(this.code, message, this.data);
	}

	public JSONResponse data(Object data) {
		return new JSONResponse(this.code, this.message, data);
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public Object getData() {
		return data;
	}

	public boolean sameCode(JSONResponse jsonResponse) {
		return Objects.equals(this.code, jsonResponse.code);
	}

	@Override
	public String toString() {
		return toJSON().toPrettyString();
	}
}
