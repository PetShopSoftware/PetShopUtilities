package dev.petshopsoftware.utilities.HTTP.Server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.net.httpserver.Headers;
import dev.petshopsoftware.utilities.JSON.JSONResponse;
import dev.petshopsoftware.utilities.Util.ParsingMode;

public class HTTPResponse extends JSONResponse {
	// 2XX code range
	public static final HTTPResponse OK = new HTTPResponse(JSONResponse.OK);
	public static final HTTPResponse CREATED = new HTTPResponse(JSONResponse.CREATED);
	public static final HTTPResponse ACCEPTED = new HTTPResponse(JSONResponse.ACCEPTED);
	public static final HTTPResponse NO_CONTENT = new HTTPResponse(JSONResponse.NO_CONTENT);

	// 4XX code range
	public static final HTTPResponse BAD_REQUEST = new HTTPResponse(JSONResponse.BAD_REQUEST);
	public static final HTTPResponse INVALID_JSON = new HTTPResponse(JSONResponse.INVALID_JSON);
	public static final HTTPResponse INVALID_BODY = new HTTPResponse(JSONResponse.INVALID_BODY);
	public static final HTTPResponse UNAUTHORIZED = new HTTPResponse(JSONResponse.UNAUTHORIZED);
	public static final HTTPResponse FORBIDDEN = new HTTPResponse(JSONResponse.FORBIDDEN);
	public static final HTTPResponse NOT_FOUND = new HTTPResponse(JSONResponse.NOT_FOUND);
	public static final HTTPResponse CONFLICT = new HTTPResponse(JSONResponse.CONFLICT);
	public static final HTTPResponse TIMEOUT = new HTTPResponse(JSONResponse.TIMEOUT);

	// 5XX code range
	public static final HTTPResponse INTERNAL_ERROR = new HTTPResponse(JSONResponse.INTERNAL_ERROR);
	public static final HTTPResponse NOT_IMPLEMENTED = new HTTPResponse(JSONResponse.NOT_IMPLEMENTED);
	public static final HTTPResponse UNAVAILABLE = new HTTPResponse(JSONResponse.UNAVAILABLE);

	@JsonIgnore
	private final ParsingMode parsingMode;
	@JsonIgnore
	private final Headers headers;

	public HTTPResponse(Integer code, String message, Object data, ParsingMode parsingMode) {
		super(code, message, data);
		this.parsingMode = parsingMode;
		this.headers = new Headers();
	}

	public HTTPResponse(int code, byte[] data) {
		this(code, null, data, ParsingMode.RAW);
	}

	public HTTPResponse(int code, String data) {
		this(code, null, data, ParsingMode.STRING);
	}

	public HTTPResponse(JSONResponse response) {
		this(response.getCode(), response.getMessage(), response.getData(), ParsingMode.JSON);
	}

	public HTTPResponse() {
		this(null, null, null, ParsingMode.JSON);
	}

	@Override
	public HTTPResponse code(Integer code) {
		return new HTTPResponse(code, this.message, this.data, this.parsingMode);
	}

	@Override
	public HTTPResponse message(String message) {
		return new HTTPResponse(this.code, message, this.data, this.parsingMode);
	}

	@Override
	public HTTPResponse data(Object data) {
		return new HTTPResponse(this.code, this.message, data, this.parsingMode);
	}

	public HTTPResponse parsingMode(ParsingMode parsingMode) {
		return new HTTPResponse(this.code, this.message, this.data, parsingMode);
	}

	public ParsingMode getParsingMode() {
		return parsingMode;
	}

	public HTTPResponse header(String key, String value) {
		this.headers.add(key, value);
		return this;
	}

	public Headers getHeaders() {
		return headers;
	}
}
