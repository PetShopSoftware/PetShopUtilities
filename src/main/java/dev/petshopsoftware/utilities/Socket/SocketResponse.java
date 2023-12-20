package dev.petshopsoftware.utilities.Socket;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.petshopsoftware.utilities.JSON.JSONResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SocketResponse extends JSONResponse {
	// 2XX code range
	public static final SocketResponse OK = new SocketResponse(JSONResponse.OK);
	public static final SocketResponse CREATED = new SocketResponse(JSONResponse.CREATED);
	public static final SocketResponse ACCEPTED = new SocketResponse(JSONResponse.ACCEPTED);
	public static final SocketResponse NO_CONTENT = new SocketResponse(JSONResponse.NO_CONTENT);

	// 4XX code range
	public static final SocketResponse BAD_REQUEST = new SocketResponse(JSONResponse.BAD_REQUEST);
	public static final SocketResponse INVALID_JSON = new SocketResponse(JSONResponse.INVALID_JSON);
	public static final SocketResponse INVALID_BODY = new SocketResponse(JSONResponse.INVALID_BODY);
	public static final SocketResponse UNAUTHORIZED = new SocketResponse(JSONResponse.UNAUTHORIZED);
	public static final SocketResponse FORBIDDEN = new SocketResponse(JSONResponse.FORBIDDEN);
	public static final SocketResponse NOT_FOUND = new SocketResponse(JSONResponse.NOT_FOUND);
	public static final SocketResponse TIMEOUT = new SocketResponse(JSONResponse.TIMEOUT);

	// 5XX code range
	public static final SocketResponse INTERNAL_ERROR = new SocketResponse(JSONResponse.INTERNAL_ERROR);
	public static final SocketResponse NOT_IMPLEMENTED = new SocketResponse(JSONResponse.NOT_IMPLEMENTED);
	public static final SocketResponse UNAVAILABLE = new SocketResponse(JSONResponse.UNAVAILABLE);

	private final String channel;

	public SocketResponse(int code, String channel, String message, Object data) {
		super(code, message, data);
		this.channel = channel.toUpperCase();
	}

	public SocketResponse(JSONResponse jsonResponse) {
		super(jsonResponse.getCode(), jsonResponse.getMessage(), jsonResponse.getData());
		this.channel = null;
	}

	public SocketResponse() {
		this.channel = null;
	}

	@Override
	public SocketResponse code(Integer code) {
		return new SocketResponse(code, this.channel, this.message, this.data);
	}

	public SocketResponse channel(String channel) {
		return new SocketResponse(this.code, channel, this.message, this.data);
	}

	@Override
	public JSONResponse message(String message) {
		return new SocketResponse(this.code, this.channel, message, this.data);
	}

	@Override
	public JSONResponse data(Object data) {
		return new SocketResponse(this.code, this.channel, this.message, data);
	}

	public String getChannel() {
		return channel;
	}

	@Override
	public String toString() {
		return code + " " + toJSON().toString();
	}
}
