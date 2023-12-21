package dev.petshopsoftware.utilities.Socket;

import dev.petshopsoftware.utilities.Logging.Log;
import dev.petshopsoftware.utilities.Logging.Logger;
import dev.petshopsoftware.utilities.Util.ParsingMode;
import dev.petshopsoftware.utilities.Util.ReflectionUtil;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SocketServer extends WebSocketServer {
	public static final List<SocketServer> ACTIVE_SERVERS = new ArrayList<>();

	public final String name;
	public final Logger logger;

	public final Map<String, SocketClient> clientsByUserID = new ConcurrentHashMap<>();
	public final Map<WebSocket, SocketClient> clientsBySocket = new ConcurrentHashMap<>();

	protected SocketServer(String name, int port) {
		super(new InetSocketAddress(port));
		this.name = name;
		this.setReuseAddr(true);
		this.logger = new Logger("socket-" + name);
	}

	public static void stopAll() {
		new ArrayList<>(ACTIVE_SERVERS).forEach(socketServer -> {
			try {
				socketServer.stop();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void onStart() {
		ACTIVE_SERVERS.add(this);
	}

	@Override
	public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
		this.clientsBySocket.put(webSocket, new SocketClient(webSocket, SocketPriority.NONE, null));
		handleConnection(webSocket, clientHandshake);
	}

	@Override
	public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
		SocketClient info = this.clientsBySocket.remove(webSocket);
		if (info.getID() != null)
			this.clientsByUserID.remove(info.getID());
		handleDisconnection(webSocket, code, reason, remote);
	}

	@Override
	public void onMessage(WebSocket webSocket, String rawMessage) {
		handleMessage(webSocket, rawMessage);
	}

	@Override
	public void onError(WebSocket webSocket, Exception e) {
		if (webSocket == null)
			logger.error(Log.fromException(new RuntimeException("Socket " + name + " exception.", e)));
		else
			logger.error(Log.fromException(new RuntimeException("Client " + clientsBySocket.get(webSocket).getID() + " exception.", e)));
	}

	protected void handleConnection(WebSocket webSocket, ClientHandshake clientHandshake) {

	}

	protected void handleDisconnection(WebSocket webSocket, int code, String reason, boolean remote) {

	}

	protected void handleAuthentication(WebSocket webSocket, int priority) {

	}

	protected void handleMessage(WebSocket webSocket, String rawMessage) {
		parseResponse(webSocket, rawMessage);
	}

	protected void parseResponse(WebSocket webSocket, String rawMessage) {
		String channel;
		String content;
		try {
			String[] message = rawMessage.split(" ", 2);
			channel = message[0];
			content = message.length > 1 ? message[1] : null;
		} catch (Exception e) {
			logger.warn(Log.fromException(new RuntimeException("Failed to parse message from " + clientsBySocket.get(webSocket).getID() + ".", e)));
			return;
		}

		Method selectedMethod = ReflectionUtil.getMethodsAnnotatedWith(getClass(), SocketChannel.class)
				.stream()
				.filter(method -> method.getAnnotation(SocketChannel.class).value().equalsIgnoreCase(channel))
				.findFirst()
				.orElse(null);

		if (selectedMethod == null) {
			send(webSocket, SocketResponse.NOT_FOUND.channel("ERROR"));
			return;
		}

		SocketChannel selectedChannel = Objects.requireNonNull(selectedMethod).getAnnotation(SocketChannel.class);

		if (!isAuthenticated(webSocket, selectedChannel)) {
			send(webSocket, SocketResponse.UNAUTHORIZED.channel("ERROR"));
			return;
		}

		SocketResponse socketResponse;
		try {
			socketResponse = (SocketResponse) selectedMethod.invoke(this, webSocket, selectedChannel.parsingMode().parse.apply(content));
		} catch (Exception e) {
			send(webSocket, SocketResponse.INTERNAL_ERROR.channel("ERROR"));
			logger.error(Log.fromException(new RuntimeException("An internal error occurred.", e)));
			return;
		}

		send(webSocket, socketResponse != null ? socketResponse.toString() : "OK");
	}

	@SocketChannel(value = "auth", parsingMode = ParsingMode.STRING)
	abstract public SocketResponse authenticate(WebSocket webSocket, String token);

	@SocketChannel(value = "ping", priority = SocketPriority.USER, parsingMode = ParsingMode.STRING)
	public SocketResponse ping(WebSocket webSocket, String message) {
		return SocketResponse.OK.channel("PING");
	}

	public void send(WebSocket webSocket, String message, int priority) {
		if (!isAuthenticated(webSocket, priority)) return;
		webSocket.send(message);
	}

	public void send(WebSocket webSocket, SocketResponse response, int priority) {
		if (response.getChannel() == null)
			throw new IllegalArgumentException("Socket response is missing target channel.");
		send(webSocket, response.toString(), priority);
	}

	public void send(WebSocket webSocket, String message) {
		send(webSocket, message, SocketPriority.NONE);
	}

	public void send(WebSocket webSocket, SocketResponse response) {
		send(webSocket, response, SocketPriority.NONE);
	}

	public void broadcast(String message, int priority) {
		clientsBySocket.keySet().forEach(webSocket -> send(webSocket, message, priority));
	}

	public void broadcast(SocketResponse response, int priority) {
		if (response.getChannel() == null)
			throw new IllegalArgumentException("Socket response is missing target channel.");
		broadcast(response.toString(), priority);
	}

	public void broadcast(String message) {
		broadcast(message, SocketPriority.NONE);
	}

	public void broadcast(SocketResponse response) {
		broadcast(response.toString(), SocketPriority.NONE);
	}

	protected boolean isAuthenticated(WebSocket webSocket, int priority) {
		return this.clientsBySocket.get(webSocket).isAuthorized(priority);
	}

	protected boolean isAuthenticated(WebSocket webSocket, SocketChannel channel) {
		return isAuthenticated(webSocket, channel.priority());
	}

	@Override
	public void stop() throws InterruptedException {
		super.stop();
		ACTIVE_SERVERS.remove(this);
	}
}
