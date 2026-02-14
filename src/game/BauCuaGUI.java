package game;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

// Giao diện chính: Trang chủ, Bàn chơi, Hiệu ứng & Âm thanh
public class BauCuaGUI extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContainer;
    private BauCuaClient client;
    private String serverHost;
    private int serverPort;

    private static final int[] TABLE_SRV_IDS = {5, 0, 4, 3, 1, 2};
    private static final String[] SYMBOLS = {"bau", "cua", "tom", "ca", "ga", "nai"};

    private Map<String, ImageIcon> iconCache = new HashMap<>();
    private Map<String, Image> rawImages = new HashMap<>();

    private JLabel balanceLabel;
    private JLabel[] diceLabels = new JLabel[3];
    private BetCard[] betCards = new BetCard[6];

    private long selectedChip = 10;
    private long currentBalance = 10000;
    private Map<Integer, Long> myBets = new HashMap<>();
    private boolean isMuted = false;
    private Clip bgmClip;
    private boolean isRolling = false;
    private ImageIcon[] diceIcons;

    private static final String IMG_DIR = "assets/images/";
    private static final String AUDIO_DIR = "assets/audio/";

    public BauCuaGUI() {
        setTitle("Bầu Cua Tôm Cá - OCSF");
        setSize(480, 820);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        preloadAndScaleAssets();

        mainContainer = new JPanel(cardLayout);
        mainContainer.add(createHomePanel(), "HOME");
        mainContainer.add(createGamePanel(), "GAME");
        add(mainContainer);
        cardLayout.show(mainContainer, "HOME");

        setVisible(true);
    }

    // Kết nối đến Server OCSF
    public void connectToServer(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
        client = new BauCuaClient(host, port, this);
        try {
            client.openConnection();
        } catch (Exception e) {
            showError("Lỗi kết nối server!");
        }
    }

    // Tải trước và scale hình ảnh để chạy mượt hơn
    private void preloadAndScaleAssets() {
        String[] assets = {"background.png", "title.png", "tieude.png", "btn_play.png", "btn_roll.png", 
                           "btn_home.png", "btn_sound.png", "table.png", "hoa_mai.png",
                           "bau.png", "cua.png", "tom.png", "ca.png", "ga.png", "nai.png"};
        for (String a : assets) {
            try {
                File f = new File(IMG_DIR + a);
                if (f.exists()) rawImages.put(a, ImageIO.read(f));
            } catch (Exception e) {}
        }
        diceIcons = new ImageIcon[6];
        for (int i = 0; i < 6; i++) diceIcons[i] = scaledIcon(SYMBOLS[i] + ".png", 90, 90);
    }

    private ImageIcon scaledIcon(String name, int w, int h) {
        String key = name + "@" + w + "x" + h;
        if (iconCache.containsKey(key)) return iconCache.get(key);
        Image src = rawImages.get(name);
        if (src == null) return new ImageIcon();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        ImageIcon icon = new ImageIcon(dst);
        iconCache.put(key, icon);
        return icon;
    }

    // Tạo màn hình chờ với thanh loading 100%
    private JPanel createHomePanel() {
        JPanel p = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                g.drawImage(rawImages.get("background.png"), 0, 0, getWidth(), getHeight(), null);
                // Vẽ hoa mai to và rõ hơn ở góc phải
                g.drawImage(rawImages.get("hoa_mai.png"), getWidth() - 280, -20, 300, 350, null);
            }
        };

        // Logo tiêu đề - Sử dụng tieude.png cho màu vàng sáng
        JLabel title = new JLabel(scaledIcon("tieude.png", 320, 110));
        title.setBounds(80, 140, 320, 110);
        p.add(title);

        // Thanh Loading bo tròn mượt mà
        final int barW = 260, barH = 16;
        final int barX = (480 - barW) / 2;
        final int barY = 350;

        JPanel loadingTrack = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
            }
        };
        loadingTrack.setOpaque(false);
        loadingTrack.setBounds(barX, barY, barW, barH);
        
        JPanel loadingBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 200, 0)); // Màu vàng tươi
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
            }
        };
        loadingBar.setOpaque(false);
        loadingBar.setBounds(0, 0, 0, barH);
        loadingTrack.add(loadingBar);
        p.add(loadingTrack);

        JLabel percentLabel = new JLabel("0%", SwingConstants.CENTER);
        percentLabel.setForeground(Color.BLACK);
        percentLabel.setFont(new Font("Arial", Font.BOLD, 16));
        percentLabel.setBounds(0, barY + 20, 480, 30);
        p.add(percentLabel);

        // Nút Play
        JButton playBtn = new JButton(scaledIcon("btn_play.png", 200, 85));
        playBtn.setBounds(140, 440, 200, 85);
        playBtn.setVisible(false);
        cleanButton(playBtn);
        playBtn.addActionListener(e -> {
            resetUI(); // Xóa trạng thái cũ
            cardLayout.show(mainContainer, "GAME");
            playBGM();
        });
        p.add(playBtn);

        javax.swing.Timer loader = new javax.swing.Timer(25, null);
        final int[] progress = {0};
        loader.addActionListener(e -> {
            progress[0] += 2;
            loadingBar.setSize((int)(progress[0] * (barW / 100.0)), barH);
            percentLabel.setText(progress[0] + "%");
            if (progress[0] >= 100) {
                loader.stop();
                playBtn.setVisible(true);
                playSound("bet.wav");
            }
        });
        loader.start();
        return p;
    }

    // Xóa sạch trạng thái cược khi quay về Home hoặc chơi mới
    private void resetUI() {
        // 1. Xóa trạng thái cục bộ
        isRolling = false;
        myBets.clear();
        selectedChip = 10;
        currentBalance = 10000;
        if (betCards != null) {
            for (BetCard c : betCards) {
                if (c != null) c.setAmount(0);
            }
        }
        // Reset dice về mặc định
        if (diceLabels != null && diceIcons != null) {
            for (JLabel lbl : diceLabels) {
                if (lbl != null) lbl.setIcon(diceIcons[0]);
            }
        }
        // Reset balance label
        if (balanceLabel != null) {
            balanceLabel.setText("Số dư: 10,000 VNĐ");
        }

        // 2. Ngắt kết nối cũ → tạo kết nối mới → server cấp BALANCE:10000 mới
        try {
            if (client != null && client.isConnected()) {
                client.closeConnection();
            }
        } catch (Exception e) {}
        // Reconnect để server tạo phiên mới với balance 10000
        client = new BauCuaClient(serverHost, serverPort, this);
        try {
            client.openConnection();
        } catch (Exception e) {
            showError("Không thể kết nối lại server!");
        }
    }

    // Tạo bàn chơi: Xúc xắc, Nút bấm, Ô đặt cược
    private JPanel createGamePanel() {
        JLayeredPane layered = new JLayeredPane();
        layered.setPreferredSize(new Dimension(480, 820));

        JPanel bgPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                g.drawImage(rawImages.get("background.png"), 0, 0, 480, 820, null);
                g.drawImage(rawImages.get("hoa_mai.png"), 230, 0, 250, 300, null);
                g.drawImage(rawImages.get("table.png"), 15, 300, 450, 300, null);
            }
        };
        bgPanel.setBounds(0, 0, 480, 820);
        layered.add(bgPanel, JLayeredPane.DEFAULT_LAYER);

        JPanel uiPanel = new JPanel(null);
        uiPanel.setOpaque(false);
        uiPanel.setBounds(0, 0, 480, 820);

        JLabel headerLogo = new JLabel(scaledIcon("tieude.png", 250, 80));
        headerLogo.setBounds(115, 10, 250, 80);
        uiPanel.add(headerLogo);

        for (int i = 0; i < 3; i++) {
            diceLabels[i] = new JLabel(diceIcons[0]);
            diceLabels[i].setBounds(90 + (i * 105), 100, 90, 90);
            uiPanel.add(diceLabels[i]);

            JLabel lbl = new JLabel("Xúc xắc " + (i + 1), SwingConstants.CENTER);
            lbl.setForeground(Color.WHITE);
            lbl.setFont(new Font("Arial", Font.BOLD, 12));
            lbl.setBounds(90 + (i * 105), 195, 90, 20);
            uiPanel.add(lbl);
        }

        JButton homeBtn = new JButton(scaledIcon("btn_home.png", 45, 45));
        homeBtn.setBounds(80, 235, 45, 45);
        cleanButton(homeBtn);
        homeBtn.addActionListener(e -> cardLayout.show(mainContainer, "HOME"));
        uiPanel.add(homeBtn);

        JButton rollBtn = new JButton(scaledIcon("btn_roll.png", 160, 60));
        rollBtn.setBounds(160, 230, 160, 60);
        cleanButton(rollBtn);
        rollBtn.addActionListener(e -> roll());
        uiPanel.add(rollBtn);

        JButton soundBtn = new JButton(scaledIcon("btn_sound.png", 45, 45));
        soundBtn.setBounds(355, 235, 45, 45);
        cleanButton(soundBtn);
        soundBtn.addActionListener(e -> toggleSound());
        uiPanel.add(soundBtn);

        int gridX = 25, gridY = 310;
        for (int i = 0; i < 6; i++) {
            final int srvIdx = TABLE_SRV_IDS[i], pos = i;
            betCards[i] = new BetCard(() -> betOn(srvIdx, pos));
            betCards[i].setBounds(gridX + (i % 3) * 145, gridY + (i / 3) * 145, 135, 135);
            uiPanel.add(betCards[i]);
        }

        long[] chipVals = {10, 50, 100, 500, 1000};
        Color[] chipColors = {new Color(0x555555), new Color(0x007bff), new Color(0xff4500), new Color(0x800080), new Color(0xffd700)};
        for (int i = 0; i < 5; i++) {
            final long val = chipVals[i];
            final Color color = chipColors[i];
            JButton chip = new JButton(val >= 1000 ? "1K" : String.valueOf(val)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (selectedChip == val) { g2.setColor(Color.WHITE); g2.fillOval(0, 0, 55, 55); }
                    g2.setColor(color); g2.fillOval(2, 2, 51, 51);
                    g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2)); g2.drawOval(6, 6, 43, 43);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            chip.setBounds(50 + (i * 78), 620, 55, 55);
            chip.setForeground(val == 1000 ? Color.BLACK : Color.WHITE);
            chip.setFont(new Font("Arial", Font.BOLD, 14));
            cleanButton(chip);
            chip.addActionListener(e -> { selectedChip = val; playSound("bet.wav"); repaint(); });
            uiPanel.add(chip);
        }

        balanceLabel = new JLabel("Số dư: 10,000 VNĐ", SwingConstants.CENTER);
        balanceLabel.setForeground(new Color(255, 215, 0));
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 20));
        balanceLabel.setBounds(90, 710, 300, 40);
        uiPanel.add(balanceLabel);

        layered.add(uiPanel, JLayeredPane.PALETTE_LAYER);
        JPanel p = new JPanel(new BorderLayout()); p.add(layered);
        return p;
    }

    private class BetCard extends JPanel {
        private long amount = 0;
        public BetCard(Runnable onClick) {
            setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { onClick.run(); } });
        }
        public void setAmount(long a) { this.amount = a; repaint(); }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (amount > 0) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 215, 0, 60)); g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 215, 0)); g2.fillRoundRect(10, 15, 90, 25, 15, 15);
                g2.setColor(Color.BLACK); g2.setFont(new Font("Arial", Font.BOLD, 16));
                String txt = String.format("%,d", amount);
                int tx = 10 + (90 - g2.getFontMetrics().stringWidth(txt)) / 2;
                g2.drawString(txt, tx, 34);
            }
        }
    }

    // Xử lý khi người dùng chọn ô đặt cược
    private void betOn(int srvIdx, int pos) {
        if (isRolling || client == null) return;
        // Kiểm tra đủ tiền trước khi đặt
        if (currentBalance < selectedChip) {
            showError("Không đủ tiền! Số dư: " + String.format("%,d", currentBalance) + " VNĐ");
            return;
        }
        client.handleMessageFromUI("BET:" + srvIdx + ":" + selectedChip);
        currentBalance -= selectedChip; // Trừ tiền cục bộ ngay
        myBets.put(srvIdx, myBets.getOrDefault(srvIdx, 0L) + selectedChip);
        betCards[pos].setAmount(myBets.get(srvIdx));
        balanceLabel.setText("Số dư: " + String.format("%,d", currentBalance) + " VNĐ");
        playSound("bet.wav");
    }

    // Hiệu ứng lắc xúc xắc (chạy ảnh ngẫu nhiên nhấp nháy)
    private void roll() {
        if (isRolling || myBets.isEmpty() || client == null) return;
        isRolling = true; client.handleMessageFromUI("ROLL");
        playSound("roll.wav");
        Random rand = new Random();
        javax.swing.Timer timer = new javax.swing.Timer(50, null);
        final int[] frame = {0};
        timer.addActionListener(e -> {
            for (JLabel lbl : diceLabels) lbl.setIcon(diceIcons[rand.nextInt(6)]);
            if (frame[0]++ > 15) timer.stop();
        });
        timer.start();
    }

    public void displayDice(int[] res) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 3; i++) diceLabels[i].setIcon(diceIcons[res[i]]);
            isRolling = false; myBets.clear();
            for (BetCard c : betCards) c.setAmount(0);
        });
    }

    public void updateBalance(long b) {
        SwingUtilities.invokeLater(() -> {
            currentBalance = b; // Đồng bộ với server
            balanceLabel.setText("Số dư: " + String.format("%,d", b) + " VNĐ");
        });
    }

    public void showResult(String m) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, m)); }
    public void showError(String m) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, m, "Thông báo", 2)); }

    private void toggleSound() {
        if (bgmClip != null) {
            if (isMuted) {
                bgmClip.start();
                isMuted = false;
            } else {
                bgmClip.stop();
                isMuted = true;
            }
        } else {
            isMuted = !isMuted;
        }
    }

    private void playBGM() {
        try {
            // Đóng clip cũ trước để không chồng tiếng
            if (bgmClip != null) {
                bgmClip.stop();
                bgmClip.close();
                bgmClip = null;
            }
            File f = new File(AUDIO_DIR + "bgm.wav"); if (!f.exists()) return;
            bgmClip = AudioSystem.getClip();
            bgmClip.open(AudioSystem.getAudioInputStream(f));
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            if (!isMuted) bgmClip.start();
        } catch (Exception e) {}
    }

    private void playSound(String fn) {
        if (isMuted) return;
        try {
            File f = new File(AUDIO_DIR + fn); if (!f.exists()) return;
            Clip c = AudioSystem.getClip();
            c.open(AudioSystem.getAudioInputStream(f));
            c.start();
        } catch (Exception e) {}
    }

    private void cleanButton(JButton b) {
        b.setBorderPainted(false); b.setContentAreaFilled(false);
        b.setFocusPainted(false); b.setCursor(new Cursor(12));
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        SwingUtilities.invokeLater(() -> {
            BauCuaGUI gui = new BauCuaGUI();
            gui.connectToServer(host, port);
        });
    }
}
