import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
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
    private boolean showNextMessage = false;
    private Timer messageTimer;

    // Networking state
    private GameClient client;
    private Map<String, Integer> scoreboard = new LinkedHashMap<>();
    private String lastEvent = "";

    // NEW: lobby UI
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

        // NEW: the lobby button is positioned by hand (see doLayout)
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

            @Override
            public void onCatch(String player, String fish, int points) {
                lastEvent = player + " caught " + fish + " +" + points;
                repaint();
            }

            @Override
            public void onGameOver(Map<String, Integer> finalScores) {
                scoreboard = finalScores;
                gameOver = true;
                isMoving = false;
                message = "Game Over!";   // CHANGED: the lobby box shows the scores and the hint
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

                if (!isMoving) {
                    if (messageTimer != null) {
                        messageTimer.stop();
                    }

                    showNextMessage = false;
                    barSpeed = (int) (Math.random() * 46) + 5;
                    randomBar();
                    barPos = 0;
                    dir = 1;
                    message = "";
                    isMoving = true;
                    repaint();
                } else {
                    checkScore();
                }
            }
        });
    }

    // NEW: called by Main with the text shown at the top of the lobby
    public void setHostInfo(String info) {
        hostInfo = info;
        repaint();
    }

    // NEW: the Start Game button is only visible in the lobby (before a round and after Game Over)
    private void updateLobbyButton() {
        startButton.setVisible(!gameStarted || gameOver);
    }

    // NEW: lobby box geometry (centered, half of the panel size)
    private int lobbyW() { return getWidth() / 2; }
    private int lobbyH() { return getHeight() / 2; }
    private int lobbyX() { return (getWidth() - lobbyW()) / 2; }
    private int lobbyY() { return (getHeight() - lobbyH()) / 2; }

    // NEW: put the button at the bottom of the lobby box
    @Override
    public void doLayout() {
        int bw = 150, bh = 36;
        startButton.setBounds((getWidth() - bw) / 2, lobbyY() + lobbyH() - bh - 12, bw, bh);
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

        barSpeed = (int) (Math.random() * 46) + 5;
        randomBar();
        barPos = 0;
        dir = 1;
        isMoving = true;

        updateLobbyButton();
        timer.start();
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

        // CHANGED: the side scoreboard and latest catch are only drawn during a round
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
            // CHANGED: in the lobby the message goes above the lobby box
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

        // NEW: the lobby is drawn last so it sits on top
        if (inLobby) {
            drawLobby(g);
        }
    }

    // NEW: lobby box with host info, player list and the hint (the button is a real JButton)
    private void drawLobby(Graphics g) {
        int x = lobbyX();
        int y = lobbyY();
        int w = lobbyW();
        int h = lobbyH();

        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(x, y, w, h, 24, 24);
        g.setColor(Color.WHITE);
        g.drawRoundRect(x, y, w, h, 24, 24);

        // Title
        g.setFont(new Font("Arial", Font.BOLD, 22));
        drawCentered(g, gameOver ? "Final Scores" : "Lobby", y + 32);

        // Where to join / what we are connected to
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        drawCentered(g, hostInfo, y + 54);

        // Player list (after Game Over it shows the final ranking)
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Players (" + scoreboard.size() + ")", x + 20, y + 80);

        g.setFont(new Font("Arial", Font.PLAIN, 14));
        int rowY = y + 102;
        int maxRows = Math.max(1, (h - 165) / 22);
        int shown = 0;
        for (Map.Entry<String, Integer> e : scoreboard.entrySet()) {
            if (shown == maxRows) {
                g.drawString("+" + (scoreboard.size() - shown) + " more...", x + 20, rowY);
                break;
            }
            String line = gameOver
                    ? (shown + 1) + ". " + e.getKey() + "  " + e.getValue()
                    : e.getKey();
            g.drawString(line, x + 20, rowY);
            rowY += 22;
            shown++;
        }

        // Hint above the button
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        drawCentered(g, "Press SPACE or click Start Game", y + h - 56);
    }

    // NEW: draw text centered in the lobby box (adds "..." if it is too long)
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

    // Catch a fish
    private void catchFish(boolean excellent) {
        caughtFish = fishManager.catchFish(barSpeed);

        if (caughtFish == null) {
            caughtFishImage = null;
            message = "No fish available!";
            messageColor = Color.RED;
            return;
        }

        caughtFishImage = caughtFish.getRandomImage();

        int point = caughtFish.getPoints();

        if (excellent) {
            point += 5;
            message = "Excellent! " + caughtFish.getName() + " +" + point;
            messageColor = Color.GREEN;
        } else {
            message = "Caught " + caughtFish.getName() + " +" + point;
            messageColor = Color.YELLOW;
        }

        score += point;

        // Tell the server so everyone's scoreboard updates
        client.sendCatch(caughtFish.getName(), point);

        System.out.println("Fish: " + caughtFish.getName());
        System.out.println("Bar Speed: " + barSpeed);
        System.out.println("Fish Speed: " + caughtFish.getSpeed());
    }

    // Check the result when the player presses SPACE while the bar is moving
    public void checkScore() {
        if (!gameStarted || gameOver || !isMoving) {
            return;
        }

        isMoving = false;
        caughtFish = null;
        caughtFishImage = null;

        if (barPos >= greenStart && barPos <= greenStart + greenWidth) {
            catchFish(true);
        } else if (barPos >= yellowStart && barPos <= yellowStart + yellowWidth) {
            catchFish(false);
        } else {
            message = "Miss! +0";
            messageColor = Color.RED;
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

    // Randomize the position and size of the colored zones
    public void randomBar() {
        yellowWidth = (int) (Math.random() * 300) + 100;
        yellowStart = (int) (Math.random() * (1001 - yellowWidth));
        greenWidth = Math.max(1, (int) (yellowWidth * 0.10));
        greenStart = yellowStart + (int) (Math.random() * (yellowWidth - greenWidth + 1));
    }
}