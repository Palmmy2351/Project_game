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
    private int ticks = 0;                      // how many steps the red line moved (sent to the server)
    private boolean waitingForCast = false;     // asked the server for a bar, no answer yet
    private boolean waitingForResult = false;   // sent the reel, no verdict yet
    private boolean showNextMessage = false;
    private Timer messageTimer;

    // Networking state
    private GameClient client;
    private Map<String, Integer> scoreboard = new LinkedHashMap<>();
    private String lastEvent = "";

    // one character per player (name -> character id), images are characters/char1.gif ... charN.gif
    private final Sprite[] characterImages = loadCharacters();
    private volatile Map<String, Integer> characters = new LinkedHashMap<>();

    // Lobby UI
    private final JButton startButton = new JButton("Start Game");
    private String hostInfo = "";

    // Original image size
    static final int IMG_W = 1847, IMG_H = 1304;

    // Convert original-image coordinates to screen coordinates
    int sx(int v) { return v * getWidth() / IMG_W; }
    int sy(int v) { return v * getHeight() / IMG_H; }

    // Fishing bar position
    int barX = 70, barY = 986, barW = 1130, barH = 55;

    // Red line position
    int barPos = 500;
    int dir = 1;
    int score = 0;
    boolean isMoving = false;

    String message = "";
    Color messageColor;

    // Yellow zone
    int yellowStart = 300;
    int yellowWidth = 400;

    // Green zone
    int greenStart = 470;
    int greenWidth = 60;

    public GamePanel(GameClient client) {
        this.client = client;
        setPreferredSize(new Dimension(800, 565));

        // The lobby button is positioned by hand (see doLayout)
        setLayout(null);
        startButton.setFocusable(false); // so SPACE is always handled by the key binding below
        startButton.addActionListener(e -> client.sendStart());
        add(startButton);
        updateLobbyButton();

        // React to messages coming from the server
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

            // the server tells us who is in the room and which character each one has
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

            // the server chose the bar speed and the zones
            @Override
            public void onCast(int speed, int yStart, int yWidth, int gStart, int gWidth) {
                SwingUtilities.invokeLater(() -> startBar(speed, yStart, yWidth, gStart, gWidth));
            }

            // the server decided what we caught
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
                message = "Game Over!";   // the lobby box shows the scores and the hint
                messageColor = Color.YELLOW;
                updateLobbyButton();
                repaint();
            }
        });

        // Timer that moves the red line
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

        // Press SPACE to start a round or to fish
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("SPACE"), "fish");

        getActionMap().put("fish", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Before the first round or after Game Over, ask the server to start
                if (!gameStarted || gameOver) {
                    client.sendStart();
                    return;
                }

                if (waitingForCast || waitingForResult) {
                    return; // waiting for the server
                }

                if (!isMoving) {
                    if (messageTimer != null) {
                        messageTimer.stop();
                    }

                    showNextMessage = false;
                    message = "";
                    waitingForCast = true;
                    client.sendCast();      // the server answers with the speed and zones
                    repaint();
                } else {
                    reel();
                }
            }
        });
    }

    // an animated GIF plus the part of it that is not empty (the empty border is skipped when drawing)
    private static class Sprite {
        final Image img;       // loaded with ImageIcon, so the GIF animation keeps playing
        final Rectangle crop;  // visible area inside the image

        Sprite(Image img, Rectangle crop) {
            this.img = img;
            this.crop = crop;
        }
    }

    // load the character GIFs (characters/char1.gif, char2.gif, ...)
    private static Sprite[] loadCharacters() {
        Sprite[] sprites = new Sprite[GameServer.CHARACTER_COUNT];
        for (int i = 0; i < sprites.length; i++) {
            File f = new File(System.getProperty("user.dir"),
                    "characters" + File.separator + "char" + (i + 1) + ".gif");
            if (!f.exists()) {
                // also accept the file directly in the project folder (next to background.png)
                f = new File(System.getProperty("user.dir"), "char" + (i + 1) + ".gif");
            }
            if (!f.exists()) {
                System.out.println("Missing character image: char" + (i + 1) + ".gif (looked in "
                        + System.getProperty("user.dir") + " and its characters folder)");
                continue;
            }
            Image img = new ImageIcon(f.getAbsolutePath()).getImage();  // ImageIcon waits until the GIF is loaded
            if (img.getWidth(null) <= 0) {
                System.out.println("Cannot read character image: " + f);
                continue;
            }
            sprites[i] = new Sprite(img, findCrop(f, img.getWidth(null), img.getHeight(null)));
        }
        return sprites;
    }

    // Finds the area that is not empty (transparent or pure white), using the first frame of the GIF
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
        return new Rectangle(0, 0, fullW, fullH);  // could not measure: use the whole image
    }

    // Called by GameFrame with the text shown at the top of the lobby
    public void setHostInfo(String info) {
        hostInfo = info;
        repaint();
    }

    // The Start Game button is only visible in the lobby (before a round and after Game Over)
    private void updateLobbyButton() {
        startButton.setVisible(!gameStarted || gameOver);
    }

    // Lobby box geometry (centered, half of the panel size)
    private int lobbyW() { return getWidth() / 2; }
    private int lobbyH() { return getHeight() / 2; }
    private int lobbyX() { return (getWidth() - lobbyW()) / 2; }
    private int lobbyY() { return (getHeight() - lobbyH()) / 2; }

    // Put the button at the bottom of the lobby box
    @Override
    public void doLayout() {
        int bw = 150, bh = 36;
        startButton.setBounds((getWidth() - bw) / 2, lobbyY() + lobbyH() - bh - 12, bw, bh);
    }

    // Speed and zones come from the server, red line back to the start, and let it move
    private void startBar(int speed, int yStart, int yWidth, int gStart, int gWidth) {
        if (!gameStarted || gameOver) {
            return; // a late answer from the previous round
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

    // Called when the server sends START
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
        client.sendCast();   // the server answers with the first bar

        updateLobbyButton();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        boolean inLobby = !gameStarted || gameOver;

        // Background
        if (bg != null) {
            g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }

        // Layers, bottom to top: background, lobby box, characters, then all the text.
        // After Game Over the box goes above the characters so the final ranking stays easy to read.
        if (inLobby && gameOver) {
            drawCharacters(g);
            drawLobbyBox(g);
        } else if (inLobby) {
            drawLobbyBox(g);
            drawCharacters(g);
        } else {
            drawCharacters(g);
        }

        // Yellow zone
        g.setColor(Color.YELLOW);
        g.fillRect(
            sx(barX + barW * yellowStart / 1000),
            sy(barY),
            sx(barW * yellowWidth / 1000),
            sy(barH)
        );

        // Green zone
        g.setColor(Color.GREEN);
        g.fillRect(
            sx(barX + barW * greenStart / 1000),
            sy(barY),
            sx(barW * greenWidth / 1000),
            sy(barH)
        );

        // Red line
        g.setColor(Color.RED);
        g.fillRect(
            sx(barX + barW * barPos / 1000) - 2,
            sy(barY) - 3,
            4,
            sy(barH) + 6
        );

        // Own score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Score: " + score, sx(1316), sy(167));

        // The side scoreboard and latest catch are only drawn during a round
        // (in the lobby, the lobby box lists the players and scores)
        if (!inLobby) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            int rowY = sy(250);
            int rank = 1;
            for (Map.Entry<String, Integer> e : scoreboard.entrySet()) {
                g.drawString(rank + ". " + e.getKey() + "  " + e.getValue(), sx(1316), rowY);
                rowY += 24;
                rank++;
            }

            if (!lastEvent.isEmpty()) {
                g.setFont(new Font("Arial", Font.PLAIN, 14));
                g.setColor(Color.WHITE);
                g.drawString(lastEvent, sx(1316), sy(560));
            }
        }

        // Time left
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));

        String timeText = String.valueOf(timeLeft);
        FontMetrics timeMetrics = g.getFontMetrics();

        int boxX = 870;
        int boxY = 1085;
        int boxW = 180;
        int boxH = 100;

        int timeX = sx(boxX) + (sx(boxW) - timeMetrics.stringWidth(timeText)) / 2;
        int timeY = sy(boxY) + (sy(boxH) - timeMetrics.getHeight()) / 2 + timeMetrics.getAscent();

        g.drawString(timeText, timeX, timeY);

        // Catch result message
        if (!message.isEmpty()) {
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.setColor(messageColor);

            FontMetrics fm = g.getFontMetrics();

            int textX = (getWidth() - fm.stringWidth(message)) / 2;
            // in the lobby the message goes above the lobby box
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

        // Caught fish panel
        if (caughtFish != null) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Caught Fish", sx(1310), sy(650));

            if (caughtFishImage != null) {
                g.drawImage(caughtFishImage, sx(1320), sy(690), sx(180), sy(180), this);
            }

            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString(caughtFish.getName(), sx(1310), sy(900));
            g.drawString("Points: " + caughtFish.getPoints(), sx(1310), sy(940));
        }

        // The lobby text goes on top of everything, including the characters
        if (inLobby) {
            drawLobbyText(g);
        }
    }

    // the sprite for a character id. If that image is missing, use the next one that exists,
    // so a player is never left without a character (returns null only if no image was loaded at all).
    private Sprite spriteFor(int id) {
        for (int k = 0; k < characterImages.length; k++) {
            Sprite sp = characterImages[(id + k) % characterImages.length];
            if (sp != null) {
                return sp;
            }
        }
        return null;
    }

    // draw one character (with the player's name) per connected player, standing on the dock.
    // Change areaX / areaW / feetY below to move them somewhere else in the scene.
    private void drawCharacters(Graphics g) {
        Map<String, Integer> players = characters;
        int n = players.size();
        if (n == 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        Object oldHint = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        // pixel art stays sharp when scaled
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // The wooden dock in background.png (original-image coordinates): x 35..1245, y 650..840
        int areaX = sx(50);
        int areaW = sx(1190);
        int size = Math.min(sx(200), areaW / n);   // cell size, shrinks when there are many players
        int startX = areaX + (areaW - n * size) / 2;
        int feetY = sy(770);                       // the line the characters stand on
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
                int dx = x + (size - dw) / 2;   // centered horizontally
                int dy = y + size - dh;         // feet on the same line
                // draw only the cropped part; "this" as observer repaints the panel on every GIF frame
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

    // Lobby box background (dark rounded rectangle, no text)
    private void drawLobbyBox(Graphics g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(lobbyX(), lobbyY(), lobbyW(), lobbyH(), 24, 24);
        g.setColor(Color.WHITE);
        g.drawRoundRect(lobbyX(), lobbyY(), lobbyW(), lobbyH(), 24, 24);
    }

    // Lobby text: title, host info, player count / final ranking and the hint (the button is a real JButton)
    private void drawLobbyText(Graphics g) {
        int x = lobbyX();
        int y = lobbyY();
        int w = lobbyW();
        int h = lobbyH();
        g.setColor(Color.WHITE);

        // Title
        g.setFont(new Font("Arial", Font.BOLD, 22));
        drawCentered(g, gameOver ? "Final Scores" : "Lobby", y + 32);

        // Where to join / what we are connected to
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        drawCentered(g, hostInfo, y + 54);

        // Player list (after Game Over it shows the final ranking)
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Players (" + scoreboard.size() + ")", x + 20, y + 80);

        // Before a round the names are already shown under the characters,
        // so the text list is only needed for the final ranking
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

        // Hint above the button
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        drawCentered(g, "Press SPACE or click Start Game", y + h - 56);
    }

    // Draw text centered in the lobby box (adds "..." if it is too long)
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

    // SPACE while the bar is moving: stop the line and let the server decide the result
    private void reel() {
        if (!gameStarted || gameOver || !isMoving) {
            return;
        }

        isMoving = false;
        waitingForResult = true;
        caughtFish = null;
        caughtFishImage = null;
        client.sendReel(ticks);   // the server replays the movement and checks the zones
        repaint();
    }

    // The server's verdict: fish is a fish name, "MISS" or "NONE"
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