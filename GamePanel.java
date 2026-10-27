import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Map;
import java.util.LinkedHashMap;

public class GamePanel extends JPanel {
    Image bg = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir") + File.separator + "background.png");

    private int timeLeft = 60;
    private Timer timer;

    private boolean gameStarted = false;
    private boolean gameOver = false;

    private FishManager fishManager = new FishManager();
    private Fish caughtFish;
    private Image caughtFishImage;
    private int barSpeed = 5;
    private int ticks = 0;                     
    private boolean waitingForCast = false;     
    private boolean waitingForResult = false;   
    private boolean showNextMessage = false;
    private Timer messageTimer;

    private GameClient client;
    private Map<String, Integer> scoreboard = new LinkedHashMap<>();
    private String lastEvent = "";

    private final Sprite[] characterImages = loadCharacters();
    private volatile Map<String, Integer> characters = new LinkedHashMap<>();

    private final JButton startButton = new JButton("Start Game");
    private String hostInfo = "";

    static final int IMG_W = 1672, IMG_H = 941;

    int sx(int v) { return v * getWidth() / IMG_W; }
    int sy(int v) { return v * getHeight() / IMG_H; }

    int barX = 52, barY = 780, barW = 1102, barH = 40;

    int barPos = 500;
    int dir = 1;
    int score = 0;
    boolean isMoving = false;

    String message = "";
    Color messageColor;

    int yellowStart = 300;
    int yellowWidth = 400;

    int greenStart = 470;
    int greenWidth = 60;

    public GamePanel(GameClient client) {
        this.client = client;
        setPreferredSize(new Dimension(800, 565));

        setLayout(null);
        startButton.setFocusable(false); 
        startButton.addActionListener(e -> client.sendStart());
        add(startButton);
        updateLobbyButton();

        client.setListener(new GameClient.Listener() {
            @Override
            public void onStart() {
                beginRound();
            }

            @Override
            public void onTime(int secondsLeft) {
                timeLeft = secondsLeft;
                repaint();
            }

            @Override
            public void onScores(Map<String, Integer> scores) {
                scoreboard = scores;
                repaint();
            }

            @Override
            public void onPlayers(Map<String, Integer> players) {
                characters = players;
                repaint();
            }

            @Override
            public void onCatch(String player, String fish, int points) {
                lastEvent = player + " caught " + fish + " +" + points;
                repaint();
            }

            @Override
            public void onCast(int speed, int yStart, int yWidth, int gStart, int gWidth) {
                SwingUtilities.invokeLater(() -> startBar(speed, yStart, yWidth, gStart, gWidth));
            }

            @Override
            public void onResult(String fish, int points, boolean excellent) {
                SwingUtilities.invokeLater(() -> showResult(fish, points, excellent));
            }

            @Override
            public void onGameOver(Map<String, Integer> finalScores) {
                scoreboard = finalScores;
                gameOver = true;
                isMoving = false;
                waitingForCast = false;
                waitingForResult = false;
                message = "Game Over!";  
                messageColor = Color.YELLOW;
                updateLobbyButton();
                repaint();
            }
        });

        timer = new Timer(10, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isMoving) {
                    ticks++;
                    barPos += dir * barSpeed;

                    if (barPos >= 1000) {
                        barPos = 1000;
                        dir = -1;
                    }
                    if (barPos <= 0) {
                        barPos = 0;
                        dir = 1;
                    }
                    repaint();
                }
            }
        });
        timer.start();

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("SPACE"), "fish");

        getActionMap().put("fish", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gameStarted || gameOver) {
                    client.sendStart();
                    return;
                }
                if (waitingForCast || waitingForResult) {
                    return; 
                }
                if (!isMoving) {
                    if (messageTimer != null) {
                        messageTimer.stop();
                    }
                    showNextMessage = false;
                    message = "";
                    waitingForCast = true;
                    client.sendCast();      
                    repaint();
                } else {
                    reel();
                }
            }
        });
    }

    private static class Sprite {
        final Image img;       
        final Rectangle crop;  

        Sprite(Image img, Rectangle crop) {
            this.img = img;
            this.crop = crop;
        }
    }

    private static Sprite[] loadCharacters() {
        Sprite[] sprites = new Sprite[GameServer.CHARACTER_COUNT];
        for (int i = 0; i < sprites.length; i++) {
            File f = new File(System.getProperty("user.dir"),"characters" + File.separator + "char" + (i + 1) + ".gif");
            if (!f.exists()) {
                f = new File(System.getProperty("user.dir"), "char" + (i + 1) + ".gif");
            }
            if (!f.exists()) {
                System.out.println("Missing character image: char" + (i + 1) + ".gif (looked in " + System.getProperty("user.dir") + " and its characters folder)");
                continue;
            }
            Image img = new ImageIcon(f.getAbsolutePath()).getImage();  
            if (img.getWidth(null) <= 0) {
                System.out.println("Cannot read character image: " + f);
                continue;
            }
            sprites[i] = new Sprite(img, findCrop(f, img.getWidth(null), img.getHeight(null)));
        }
        return sprites;
    }

    private static Rectangle findCrop(File f, int fullW, int fullH) {
        try {
            BufferedImage src = ImageIO.read(f);
            if (src != null) {
                int w = src.getWidth(), h = src.getHeight();
                int minX = w, minY = h, maxX = -1, maxY = -1;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int argb = src.getRGB(x, y);
                        int a = argb >>> 24;
                        int r = (argb >> 16) & 255, gr = (argb >> 8) & 255, b = argb & 255;
                        boolean empty = a < 16 || (r > 240 && gr > 240 && b > 240);
                        if (!empty) {
                            if (x < minX) minX = x;
                            if (x > maxX) maxX = x;
                            if (y < minY) minY = y;
                            if (y > maxY) maxY = y;
                        }
                    }
                }
                if (maxX >= 0) {
                    return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
                }
            }
        } catch (IOException | IllegalArgumentException ignored) {
        }
        return new Rectangle(0, 0, fullW, fullH);  
    }

    public void setHostInfo(String info) {
        hostInfo = info;
        repaint();
    }

    private void updateLobbyButton() {
        startButton.setVisible(!gameStarted || gameOver);
    }

    private int lobbyW() { return getWidth() / 2; }
    private int lobbyH() { return getHeight() / 2; }
    private int lobbyX() { return (getWidth() - lobbyW()) / 2; }
    private int lobbyY() { return (getHeight() - lobbyH()) / 2; }

    @Override
    public void doLayout() {
        int bw = 150, bh = 36;
        startButton.setBounds((getWidth() - bw) / 2, lobbyY() + lobbyH() - bh - 12, bw, bh);
    }

    private void startBar(int speed, int yStart, int yWidth, int gStart, int gWidth) {
        if (!gameStarted || gameOver) {
            return; 
        }
        barSpeed = speed;
        yellowStart = yStart;
        yellowWidth = yWidth;
        greenStart = gStart;
        greenWidth = gWidth;
        barPos = 0;
        dir = 1;
        ticks = 0;
        waitingForCast = false;
        isMoving = true;
        repaint();
    }

    private void beginRound() {
        gameStarted = true;
        gameOver = false;
        score = 0;
        caughtFish = null;
        caughtFishImage = null;
        message = "";
        showNextMessage = false;
        lastEvent = "";

        if (messageTimer != null) {
            messageTimer.stop();
        }

        isMoving = false;
        waitingForResult = false;
        waitingForCast = true;
        client.sendCast();  

        updateLobbyButton();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        boolean inLobby = !gameStarted || gameOver;

        if (bg != null) {
            g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }
        if (inLobby && gameOver) {
            drawCharacters(g);
            drawLobbyBox(g);
        } else if (inLobby) {
            drawLobbyBox(g);
            drawCharacters(g);
        } else {
            drawCharacters(g);
        }

        if (!inLobby) {
        g.setColor(Color.YELLOW);
        g.fillRect(
            sx(barX + barW * yellowStart / 1000),
            sy(barY),
            sx(barW * yellowWidth / 1000),
            sy(barH)
        );
        g.setColor(Color.GREEN);
        g.fillRect(
            sx(barX + barW * greenStart / 1000),
            sy(barY),
            sx(barW * greenWidth / 1000),
            sy(barH)
        );
        g.setColor(Color.RED);
        g.fillRect(
            sx(barX + barW * barPos / 1000) - 2,
            sy(barY) - 3,
            4,
            sy(barH) + 6
    );
}
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Score: " + score, sx(1250), sy(130));

        if (!inLobby) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            int rowY = sy(170);
            int rank = 1;
            for (Map.Entry<String, Integer> e : scoreboard.entrySet()) {
                g.drawString(rank + ". " + e.getKey() + "  " + e.getValue(), sx(1250), rowY);
                rowY += 24;
                rank++;
            }
            if (!lastEvent.isEmpty()) {
                g.setFont(new Font("Arial", Font.PLAIN, 14));
                g.setColor(Color.WHITE);
                g.drawString(lastEvent, sx(1290), sy(530));
            }
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));

        String timeText = String.valueOf(timeLeft);
        FontMetrics timeMetrics = g.getFontMetrics();

        int boxX = 1000;
        int boxY = 835;
        int boxW = 117;
        int boxH = 61;

        int timeX = sx(boxX) + (sx(boxW) - timeMetrics.stringWidth(timeText)) / 2;
        int timeY = sy(boxY) + (sy(boxH) - timeMetrics.getHeight()) / 2 + timeMetrics.getAscent();

        g.drawString(timeText, timeX, timeY);

        if (!message.isEmpty()) {
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.setColor(messageColor);

            FontMetrics fm = g.getFontMetrics();

            int textX = (getWidth() - fm.stringWidth(message)) / 2;
            int textY = inLobby ? lobbyY() - 16 : getHeight() / 2;

            g.drawString(message, textX, textY);
        }

        if (showNextMessage && !gameOver) {
            String nextMessage = "Press SPACE to fish again!";

            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(Color.WHITE);

            FontMetrics fm = g.getFontMetrics();
            int textX = (getWidth() - fm.stringWidth(nextMessage)) / 2;
            int textY = getHeight() / 2 + 40;

            g.drawString(nextMessage, textX, textY);
        }

        if (caughtFish != null) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Caught Fish", sx(1320), sy(480));

            if (caughtFishImage != null) {
                g.drawImage(caughtFishImage, sx(1340), sy(560), sx(180), sy(180), this);
            }
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString(caughtFish.getName(), sx(1355), sy(795));
            g.drawString("Points: " + caughtFish.getPoints(), sx(1355), sy(840));
        }
        if (inLobby) {
            drawLobbyText(g);
        }
    }

    private Sprite spriteFor(int id) {
        for (int k = 0; k < characterImages.length; k++) {
            Sprite sp = characterImages[(id + k) % characterImages.length];
            if (sp != null) {
                return sp;
            }
        }
        return null;
    }

    private void drawCharacters(Graphics g) {
        Map<String, Integer> players = characters;
        int n = players.size();
        if (n == 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        Object oldHint = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        int areaX = sx(50);
        int areaW = sx(1190);
        int size = Math.min(sx(200), areaW / n);   
        int startX = areaX + (areaW - n * size) / 2;
        int feetY = sy(600);                      
        int y = feetY - size;

        g2.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();

        int i = 0;
        for (Map.Entry<String, Integer> e : players.entrySet()) {
            int x = startX + i * size;
            Sprite sp = spriteFor(e.getValue());

            if (sp != null) {
                Rectangle c = sp.crop;
                double scale = Math.min((double) size / c.width, (double) size / c.height);
                int dw = (int) (c.width * scale), dh = (int) (c.height * scale);
                int dx = x + (size - dw) / 2;  
                int dy = y + size - dh;        
                g2.drawImage(sp.img, dx, dy, dx + dw, dy + dh,
                        c.x, c.y, c.x + c.width, c.y + c.height, this);
            }
            g2.setColor(Color.WHITE);
            g2.drawString(e.getKey(), x + (size - fm.stringWidth(e.getKey())) / 2, y + size + 14);
            i++;
        }
        if (oldHint != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
        }
    }

    private void drawLobbyBox(Graphics g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(lobbyX(), lobbyY(), lobbyW(), lobbyH(), 24, 24);
        g.setColor(Color.WHITE);
        g.drawRoundRect(lobbyX(), lobbyY(), lobbyW(), lobbyH(), 24, 24);
    }

    private void drawLobbyText(Graphics g) {
        int x = lobbyX();
        int y = lobbyY();
        int w = lobbyW();
        int h = lobbyH();
        g.setColor(Color.WHITE);

        g.setFont(new Font("Arial", Font.BOLD, 22));
        drawCentered(g, gameOver ? "Final Scores" : "Lobby", y + 32);

        g.setFont(new Font("Arial", Font.PLAIN, 12));
        drawCentered(g, hostInfo, y + 54);

        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Players (" + scoreboard.size() + ")", x + 20, y + 80);

        if (gameOver) {
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            int rowY = y + 102;
            int maxRows = Math.max(1, (h - 165) / 22);
            int shown = 0;
            for (Map.Entry<String, Integer> e : scoreboard.entrySet()) {
                if (shown == maxRows) {
                    g.drawString("+" + (scoreboard.size() - shown) + " more...", x + 20, rowY);
                    break;
                }
                g.drawString((shown + 1) + ". " + e.getKey() + "  " + e.getValue(), x + 20, rowY);
                rowY += 22;
                shown++;
            }
        }
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        drawCentered(g, "Press SPACE or click Start Game", y + h - 56);
    }

    private void drawCentered(Graphics g, String s, int baselineY) {
        FontMetrics fm = g.getFontMetrics();
        int maxW = lobbyW() - 24;
        String t = s;
        if (fm.stringWidth(t) > maxW) {
            while (t.length() > 3 && fm.stringWidth(t + "...") > maxW) {
                t = t.substring(0, t.length() - 1);
            }
            t = t + "...";
        }
        g.drawString(t, lobbyX() + (lobbyW() - fm.stringWidth(t)) / 2, baselineY);
    }

    private void reel() {
        if (!gameStarted || gameOver || !isMoving) {
            return;
        }

        isMoving = false;
        waitingForResult = true;
        caughtFish = null;
        caughtFishImage = null;
        client.sendReel(ticks);  
        repaint();
    }

    private void showResult(String fish, int points, boolean excellent) {
        waitingForResult = false;
        if (!gameStarted || gameOver) {
            return;
        }
        caughtFish = null;
        caughtFishImage = null;
        if (fish.equals("MISS")) {
            message = "Miss! +0";
            messageColor = Color.RED;
        } else {
            Fish f = fishManager.findByName(fish);
            if (f == null) {
                message = "No fish available!";
                messageColor = Color.RED;
            } else {
                caughtFish = f;
                caughtFishImage = f.getRandomImage();
                score += points;
                if (excellent) {
                    message = "Excellent! " + f.getName() + " +" + points;
                    messageColor = Color.GREEN;
                } else {
                    message = "Caught " + f.getName() + " +" + points;
                    messageColor = Color.YELLOW;
                }
            }
        }
        repaint();

        if (messageTimer != null) {
            messageTimer.stop();
        }
        messageTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameOver) {
                    return;
                }
                showNextMessage = true;
                repaint();
            }
        });
        messageTimer.setRepeats(false);
        messageTimer.start();
    }
}