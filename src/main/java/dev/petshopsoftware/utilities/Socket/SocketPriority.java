package dev.petshopsoftware.utilities.Socket;

public class SocketPriority {
    public final static int NONE = 0;
    public final static int USER = 100;
    public final static int ADMIN = 1000;

    public static boolean isAuthorized(int clientPriority, int priority){
        return clientPriority >= priority;
    }

    public static boolean isAuthorized(SocketClient client, int priority){
        return isAuthorized(client.getPriority(), priority);
    }
}
