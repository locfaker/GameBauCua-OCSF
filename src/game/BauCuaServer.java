package game;

import ocsf.server.*;
import models.*;
import java.io.IOException;
import java.util.*;

/**
 * BauCuaServer - extends AbstractServer 
 */
// Xử lý logic Game phía Server: Quản lý tiền, Nhận cược, Lắc xúc xắc
public class BauCuaServer extends AbstractServer {
    private static final long INITIAL_BALANCE = 10000;
    private DiceEngine diceEngine = new DiceEngine();

    public BauCuaServer(int port) {
        super(port);
    }

    // Khi Client kết nối: Cấp 10,000 VNĐ ban đầu
    @Override
    protected void clientConnected(ConnectionToClient client) {
        client.setInfo("balance", INITIAL_BALANCE);
        client.setInfo("bets", new HashMap<Integer, Long>());
        System.out.println(">>> Client moi ket noi: " + client.getInetAddress());
        try {
            client.sendToClient("BALANCE:" + INITIAL_BALANCE);
        } catch (IOException e) {
            System.err.println("Loi gui balance ban dau: " + e.getMessage());
        }
    }

    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        System.out.println("<<< Client ngat ket noi: " + client.getInetAddress());
    }

    @Override
    protected synchronized void clientException(ConnectionToClient client, Throwable exception) {
        System.out.println("!!! Client loi: " + exception.getMessage());
    }

    @Override
    protected void serverStarted() {
        System.out.println("=== Server OCSF dang lang nghe port " + getPort() + " ===");
    }

    @Override
    protected void serverStopped() {
        System.out.println("=== Server da dung ===");
    }

    // Nhận và phân loại yêu cầu từ Client (ĐẶT CƯỢC hoặc LẮC)
    @Override
    @SuppressWarnings("unchecked")
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        String message = (String) msg;
        System.out.println("Nhan tu client: " + message);

        if (message.startsWith("BET:")) {
            handleBet(message, client);
        } else if (message.equals("ROLL")) {
            handleRoll(client);
        }
    }

    // Trừ tiền và ghi nhận cược của người chơi
    @SuppressWarnings("unchecked")
    private void handleBet(String message, ConnectionToClient client) {
        try {
            String[] parts = message.split(":");
            int symbolIdx = Integer.parseInt(parts[1]);
            long amount = Long.parseLong(parts[2]);

            long balance = (long) client.getInfo("balance");
            if (balance >= amount) {
                client.setInfo("balance", balance - amount);
                Map<Integer, Long> bets = (Map<Integer, Long>) client.getInfo("bets");
                bets.put(symbolIdx, bets.getOrDefault(symbolIdx, 0L) + amount);
                client.sendToClient("BALANCE:" + client.getInfo("balance"));
            }
        } catch (Exception e) {}
    }

    // Lắc xúc xắc, tính kết quả Thắng/Thua và gửi lại cho Client
    @SuppressWarnings("unchecked")
    private void handleRoll(ConnectionToClient client) {
        Map<Integer, Long> bets = (Map<Integer, Long>) client.getInfo("bets");
        if (bets.isEmpty()) return;

        List<DiceSymbol> result = diceEngine.roll();
        int[] dice = { result.get(0).getValue(), result.get(1).getValue(), result.get(2).getValue() };

        Map<Integer, Integer> matchCounts = new HashMap<>();
        for (int d : dice) matchCounts.put(d, matchCounts.getOrDefault(d, 0) + 1);

        long totalWin = 0;
        for (Map.Entry<Integer, Long> entry : bets.entrySet()) {
            int betSymbol = entry.getKey();
            long betAmount = entry.getValue();
            int count = matchCounts.getOrDefault(betSymbol, 0);
            if (count > 0) totalWin += betAmount + (betAmount * count);
        }

        long newBalance = (long) client.getInfo("balance") + totalWin;
        client.setInfo("balance", newBalance);
        bets.clear();

        try {
            client.sendToClient("DICE:" + dice[0] + "," + dice[1] + "," + dice[2]);
            if (totalWin > 0) client.sendToClient("WIN:" + totalWin);
            else client.sendToClient("LOSE");
            client.sendToClient("BALANCE:" + newBalance);
        } catch (IOException e) {}
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        try {
            BauCuaServer server = new BauCuaServer(port);
            server.listen();
        } catch (Exception e) {}
    }
}
