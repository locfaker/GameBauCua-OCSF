package ocsf.server;

import java.io.*;
import java.net.*;
import java.util.*;

// Lớp nền tảng cho phía Server: Lắng nghe kết nối và quản lý Client
public abstract class AbstractServer implements Runnable {
    private ServerSocket serverSocket = null;
    private Thread connectionListener = null;
    private int port;
    private int timeout = 500;
    private int backlog = 10;
    private List<Thread> clientThreads = Collections.synchronizedList(new ArrayList<Thread>());
    private boolean readyToStop = true;

    public AbstractServer(int port) {
        this.port = port;
    }

    // Bắt đầu lắng nghe các kết nối mới từ Client
    public final void listen() throws IOException {
        if (!readyToStop) return;
        serverSocket = new ServerSocket(port, backlog);
        readyToStop = false;
        connectionListener = new Thread(this);
        connectionListener.start();
        serverStarted();
    }

    public final void stopListening() { readyToStop = true; }

    public final synchronized void close() throws IOException {
        stopListening();
        if (serverSocket != null) serverSocket.close();
        serverStopped();
    }

    // Gửi tin nhắn cho tất cả Client đang kết nối
    public void sendToAllClients(Object msg) {
        synchronized(clientThreads) {
            for (Thread t : clientThreads) {
                if (t instanceof ConnectionToClient) {
                    try { ((ConnectionToClient)t).sendToClient(msg); } catch (Exception e) {}
                }
            }
        }
    }

    public final int getPort() { return port; }

    protected abstract void handleMessageFromClient(Object msg, ConnectionToClient client);
    protected void clientConnected(ConnectionToClient client) {}
    protected synchronized void clientDisconnected(ConnectionToClient client) {}
    protected synchronized void clientException(ConnectionToClient client, Throwable exception) {}
    protected void serverStarted() {}
    protected void serverStopped() {}

    // Luồng chính để chấp nhận các kết nối mới
    @Override
    public final void run() {
        try {
            while (!readyToStop) {
                Socket clientSocket = serverSocket.accept();
                ConnectionToClient connection = new ConnectionToClient(clientSocket, this);
                clientThreads.add(connection);
                connection.start();
                clientConnected(connection);
            }
        } catch (IOException exception) {
            if (!readyToStop) connectionException(exception);
        }
    }

    protected synchronized void connectionException(Exception exception) {}
}
