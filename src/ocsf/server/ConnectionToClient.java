package ocsf.server;

import java.io.*;
import java.net.*;
import java.util.*;

// Đại diện cho một kết nối cụ thể từ một Client đến Server
public class ConnectionToClient extends Thread {
    private Socket clientSocket;
    private AbstractServer server;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Map<String, Object> savedInfo = new HashMap<>();
    private boolean readyToStop = false;

    public ConnectionToClient(Socket socket, AbstractServer server) throws IOException {
        this.clientSocket = socket;
        this.server = server;
        output = new ObjectOutputStream(clientSocket.getOutputStream());
        input = new ObjectInputStream(clientSocket.getInputStream());
    }

    // Gửi dữ liệu riêng cho Client này
    public void sendToClient(Object msg) throws IOException {
        synchronized(this) {
            output.writeObject(msg);
            output.reset();
        }
    }

    public final void close() throws IOException {
        readyToStop = true;
        clientSocket.close();
    }

    public InetAddress getInetAddress() { return clientSocket.getInetAddress(); }
    public void setInfo(String infoType, Object info) { savedInfo.put(infoType, info); }
    public Object getInfo(String infoType) { return savedInfo.get(infoType); }

    // Luồng đọc dữ liệu từ Client này gửi lên Server
    @Override
    public final void run() {
        try {
            while (!readyToStop) {
                Object msg = input.readObject();
                server.handleMessageFromClient(msg, this);
            }
        } catch (Exception exception) {
            if (!readyToStop) server.clientException(this, exception);
        } finally {
            try { close(); } catch (Exception e) {}
            server.clientDisconnected(this);
        }
    }
}
