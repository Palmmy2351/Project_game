
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;

public class GamePanel extends JPanel {

    Image bg = Toolkit.getDefaultToolkit().createImage(
        System.getProperty("user.dir") + File.separator + "background.png"
    );

    private int timeLeft = 60;
    private Timer timer;
    private Timer gameTimer;
    private Timer nextRound;

    private boolean gameStarted = false;
    private boolean gameOver = false;

    // ขนาดภาพต้นฉบับ
    static final int IMG_W = 1847, IMG_H = 1304;

    // แปลงพิกัดภาพต้นฉบับเป็นพิกัดบนจอ
    int sx(int v) {
        return v * getWidth() / IMG_W;
    }

    int sy(int v) {
        return v * getHeight() / IMG_H;
    }

    // ตำแหน่ง Fishing Bar
    int barX = 70, barY = 985, barW = 1130, barH = 55;

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

        // Timer สำหรับขยับเส้นแดง
        timer = new Timer(10, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isMoving && !gameOver) {
                    int speed = (int) (Math.random() * 20) + 1;

                    barPos += dir * speed;

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

        // กด Space เพื่อเริ่มเกมหรือตกปลา
        getInputMap(WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("SPACE"), "fish");

        getActionMap().put("fish", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gameStarted) {
                    startGame();
                } else {
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

        // เวลาอยู่ตรงกลางช่องสี่เหลี่ยมด้านล่าง
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));

        String timeText = String.valueOf(timeLeft);
        FontMetrics timeMetrics = g.getFontMetrics();

        // พิกัดช่อง Time ในภาพต้นฉบับ
        int boxX = 870;
        int boxY = 1085;
        int boxW = 180;
        int boxH = 100;

        // คำนวณตำแหน่งกึ่งกลางแนวนอน
        int timeX = sx(boxX)
            + (sx(boxW) - timeMetrics.stringWidth(timeText)) / 2;

        // คำนวณตำแหน่งกึ่งกลางแนวตั้ง
        int timeY = sy(boxY)
            + (sy(boxH) - timeMetrics.getHeight()) / 2
            + timeMetrics.getAscent();

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

        // ข้อความก่อนเริ่มเกม
        if (!gameStarted) {
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(Color.WHITE);

            g.drawString(
                "Press SPACE to Start",
                getWidth() / 2 - 100,
                getHeight() / 2
            );
        }
    }

    // ตรวจสอบคะแนนเมื่อตกปลา
    public void checkScore() {
        if (!gameStarted || gameOver || !isMoving) {
            return;
        }

        isMoving = false;

        // อยู่ในแถบสีเขียว ได้คะแนนพิเศษ
        if (barPos >= greenStart &&
            barPos <= greenStart + greenWidth) {

            int point = (int) (Math.random() * 10) + 1;
            score += point + 5;

            message = "Excellent! +" + (point + 5);
            messageColor = Color.GREEN;

        } else if (barPos >= yellowStart &&
                   barPos <= yellowStart + yellowWidth) {

            // อยู่ในแถบสีเหลือง
            int point = (int) (Math.random() * 10) + 1;
            score += point;

            message = "Fishing Success! +" + point;
            messageColor = Color.YELLOW;

        } else {
            // พลาด
            message = "Miss! +0";
            messageColor = Color.RED;
        }

        repaint();

        // รอ 1.5 วินาที แล้วเริ่มรอบใหม่
        nextRound = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameOver) {
                    return;
                }

                randomBar();
                message = "";
                isMoving = true;

                repaint();
            }
        });

        nextRound.setRepeats(false);
        nextRound.start();
    }

    // สุ่มตำแหน่งและขนาดแถบสีใหม่
    public void randomBar() {
        yellowStart = (int) (Math.random() * 700);
        yellowWidth = (int) (Math.random() * 300) + 100;

        greenWidth = Math.max(1, (int) (yellowWidth * 0.10));

        greenStart = yellowStart
            + (int) (Math.random() * (yellowWidth - greenWidth + 1));
    }
}
