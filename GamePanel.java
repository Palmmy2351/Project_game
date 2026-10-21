
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;

public class GamePanel extends JPanel {

    Image bg = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir") + File.separator + "background.png");

    private int timeLeft = 60;
    private Timer timer;
    private Timer gameTimer;
    private Timer nextRound;

    private boolean gameStarted = false;
    private boolean gameOver = false;

    private FishManager fishManager = new FishManager();
    private Fish caughtFish;
    private Image caughtFishImage;
    private int barSpeed = 5;
    private boolean showNextMessage = false;
    private Timer messageTimer;

    // ขนาดภาพต้นฉบับ
    static final int IMG_W = 1847, IMG_H = 1304;

    // แปลงพิกัดภาพต้นฉบับเป็นพิกัดบนจอ
    int sx(int v) { return v * getWidth() / IMG_W; }
    int sy(int v) { return v * getHeight() / IMG_H; }

    // ตำแหน่ง Fishing Bar
    int barX = 70, barY = 986, barW = 1130, barH = 55;

    // ตำแหน่งแถบ Fishing Bar
    int barPos = 500;
    int dir = 1;
    int score = 0;
    boolean isMoving = false;

    String message = "";
    Color messageColor;

    // แถบสีเหลือง
    int yellowStart = 300;
    int yellowWidth = 400;

    // แถบสีเขียว
    int greenStart = 470;
    int greenWidth = 60;

    public GamePanel() {
        setPreferredSize(new Dimension(800, 565));

        // Timer เลื่อนเส้นแดง
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

        // กด Space เพื่อเริ่มเกมหรือตกปลา
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("SPACE"), "fish");

        getActionMap().put("fish", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameOver) {
                    return;
                }

                if (!gameStarted) {
                    startGame();
                } 
                else if (!isMoving){
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
                }else {
                    checkScore();
                }
            }
        });
    }

    // เริ่มเกม 60 วินาที
    public void startGame() {
        if (gameStarted) {
            return;
        }

        gameStarted = true;
        gameOver = false;
        timeLeft = 60;
        score = 0;
        barPos = 500;
        dir = 1;
        isMoving = true;
        message = "";

        // Timer นับถอยหลังทุก 1 วินาที
        gameTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timeLeft--;

                if (timeLeft <= 0) {
                    timeLeft = 0;
                    endGame();
                }

                repaint();
            }
        });

        gameTimer.start();
        timer.start();
        repaint();
    }

    // จบเกมเมื่อหมดเวลา
    private void endGame() {
        gameOver = true;
        isMoving = false;

        message = "Game Over! Score: " + score;
        messageColor = Color.YELLOW;

        if (gameTimer != null) {
            gameTimer.stop();
        }

        if (timer != null) {
            timer.stop();
        }

        if (nextRound != null) {
            nextRound.stop();
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // วาดภาพพื้นหลัง
        if (bg != null) {
            g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }

        // แถบสีเหลือง
        g.setColor(Color.YELLOW);
        g.fillRect(
            sx(barX + barW * yellowStart / 1000),
            sy(barY),
            sx(barW * yellowWidth / 1000),
            sy(barH)
        );

        // แถบสีเขียว
        g.setColor(Color.GREEN);
        g.fillRect(
            sx(barX + barW * greenStart / 1000),
            sy(barY),
            sx(barW * greenWidth / 1000),
            sy(barH)
        );

        // เส้นแดง
        g.setColor(Color.RED);
        g.fillRect(
            sx(barX + barW * barPos / 1000) - 2,
            sy(barY) - 3,
            4,
            sy(barH) + 6
        );

        // คะแนนด้านบน
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Score: " + score, sx(1316), sy(167));

        // เวลา
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

        // ข้อความผลการตกปลา
        if (!message.isEmpty()) {
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.setColor(messageColor);

            FontMetrics fm = g.getFontMetrics();

            int textX = (getWidth() - fm.stringWidth(message)) / 2;
            int textY = getHeight() / 2;

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

        // ข้อความก่อนเริ่มเกม
        if (!gameStarted) {
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(Color.WHITE);
            g.drawString("Press SPACE to Start", getWidth() / 2 - 100, getHeight() / 2);
        }

        // แสดงปลาที่ตกได้
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
    }

    // จับปลา
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

        System.out.println("Fish: " + caughtFish.getName());
        System.out.println("Bar Speed: " + barSpeed);
        System.out.println("Fish Speed: " + caughtFish.getSpeed());
    }

    // ตรวจสอบคะแนนเมื่อตกปลา
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

    // สุ่มตำแหน่งและขนาดแถบสีใหม่
    public void randomBar() {
        yellowWidth = (int) (Math.random() * 300) + 100;
        yellowStart = (int) (Math.random() * (1001 - yellowWidth));
        greenWidth = Math.max(1, (int) (yellowWidth * 0.10));
        greenStart = yellowStart + (int) (Math.random() * (yellowWidth - greenWidth + 1));
    }
}
