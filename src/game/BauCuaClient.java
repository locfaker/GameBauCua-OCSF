package game;

import ocsf.client.*;
import java.io.*;

/**
 * BauCuaClient - Kế thừa AbstractClient 
 */
// Cầu nối liên lạc giữa Giao diện (GUI) và Server
public class BauCuaClient extends AbstractClient {
    private BauCuaGUI gui;

    public BauCuaClient(String host, int port, BauCuaGUI gui) {
        super(host, port);
        this.gui = gui;
    }

    // Xử lý thông tin gửi từ Server (Tiền, Xúc xắc, Thắng/Thua)
    @Override
    protected void handleMessageFromServer(Object msg) {
        if (!(msg instanceof String)) return;
        String message = (String) msg;

        if (message.startsWith("BALANCE:")) {
            gui.updateBalance(Long.parseLong(message.substring(8)));
        } else if (message.startsWith("DICE:")) {
            String[] dice = message.substring(5).split(",");
            int[] results = { Integer.parseInt(dice[0]), Integer.parseInt(dice[1]), Integer.parseInt(dice[2]) };
            gui.displayDice(results);
        } else if (message.startsWith("WIN:")) {
            gui.showResult("BẠN THẮNG: " + message.substring(4) + " VNĐ");
        } else if (message.equals("LOSE")) {
            gui.showResult("BẠN THUA!");
        } else if (message.startsWith("ERROR:")) {
            gui.showError(message.substring(6));
        }
    }

    // Gửi hành động của người chơi lên Server
    public void handleMessageFromUI(String message) {
        try {
            sendToServer(message);
        } catch (IOException e) {
            gui.showError("Lỗi kết nối server!");
        }
    }

    @Override
    protected void connectionException(Exception exception) {
        gui.showError("Mất kết nối server!");
    }
}
