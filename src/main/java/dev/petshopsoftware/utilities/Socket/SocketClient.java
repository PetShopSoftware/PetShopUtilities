package dev.petshopsoftware.utilities.Socket;

import org.java_websocket.WebSocket;

public class SocketClient {
    private final WebSocket socket;
    private int priority;
    private String id;

    public SocketClient(WebSocket socket, int priority, String userID) {
        this.socket = socket;
        this.priority = priority;
        this.id = userID;
    }

    public SocketClient(WebSocket socket) {
        this(socket, SocketPriority.NONE, null);
    }

    public WebSocket getSocket() {
        return socket;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public boolean isAuthorized(int priority) {
        return SocketPriority.isAuthorized(this, priority);
    }
}
