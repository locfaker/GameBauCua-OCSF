package ocsf.client;

import java.io.*;
import java.net.*;

// Lớp nền tảng cho phía Client: Kết nối, gửi/nhận dữ liệu
public abstract class AbstractClient implements Runnable {
    private Socket clientSocket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Thread clientReader;
    private String host;
    private int port;
    private boolean readyToStop = true;

    public AbstractClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // Mở kết nối đến Server
    public final synchronized void openConnection() throws IOException {
        if (!readyToStop) return;
        clientSocket = new Socket(host, port);
        output = new ObjectOutputStream(clientSocket.getOutputStream());
        input = new ObjectInputStream(clientSocket.getInputStream());
        clientReader = new Thread(this);
        readyToStop = false;
        clientReader.start();
    }

    public final synchronized void closeConnection() throws IOException {
        readyToStop = true;
        try {
            if (clientSocket != null) clientSocket.close();
        } finally {
            connectionClosed();
        }
    }

    // Gửi đối tượng (Object) lên Server
    public final void sendToServer(Object msg) throws IOException {
        if (clientSocket == null || output == null) throw new SocketException("No connection");
        synchronized(this) {
            output.writeObject(msg);
            output.reset();
        }
    }

    public final boolean isConnected() { return !readyToStop; }
    protected abstract void handleMessageFromServer(Object msg);
    protected void connectionClosed() {}
    protected void connectionEstablished() {}
    protected void connectionException(Exception exception) {}

    // Luồng đọc dữ liệu liên tục từ Server
    @Override
    public final void run() {
        connectionEstablished();
        try {
            while (!readyToStop) {
                Object msg = input.readObject();
                handleMessageFromServer(msg);
            }
        } catch (Exception exception) {
            if (!readyToStop) connectionException(exception);
        } finally {
            try { closeConnection(); } catch (Exception e) {}
        }
    }
}
