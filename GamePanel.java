import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;

public class GamePanel extends JPanel {
    Image bg = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir") + File.separator + "background.png");

    // ขนาดภาพต้นฉบับ
    static final int IMG_W = 1847, IMG_H = 1304;

    // แปลงพิกัดภาพต้นฉบับ -> พิกเซลบนจอ ตามขนาด panel ตอนนี้
    int sx(int v) { return v * getWidth()  / IMG_W; }
    int sy(int v) { return v * getHeight() / IMG_H; }

    // ตำแหน่งแถบ Fishing Bar (พิกัดตามภาพต้นฉบับ)
    int barX = 70, barY = 985, barW = 1130, barH = 55;
    int barPos = 500;   // ตำแหน่งเส้นแดง 0..1000 (ลองเริ่มกลางแถบก่อน)
    int dir = 1;        // ทิศทางการเลื่อนของเส้นแดง (1=ขวา, -1=ซ้าย)
    int score = 0;        // คะแนนสะสม

    // สถานะเส้นแดง
    boolean isMoving = true;

    // ข้อความผลการตกปลา
    String message = "";

    // ตำแหน่งและขนาดแถบสีเหลือง
    int yellowStart = 300;
    int yellowWidth = 400;

    // ตำแหน่งและขนาดแถบสีเขียว
    int greenStart = 470;
    int greenWidth = 60;

    public GamePanel() {
        setPreferredSize(new Dimension(800, 565));

        // Timer: ทุก 30 ms เลื่อนเส้นแดง แล้ววาดใหม่
        Timer timer = new Timer(30, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (isMoving) {
                barPos += dir * 10;
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

        // กด Space -> ตรวจคะแนน
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "fish");
        getActionMap().put("fish", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                checkScore();
            }
        });
    }   
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);  
        if (bg != null) {
            g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }  
        // วาดแถบสีเหลือง
        g.setColor(Color.YELLOW);
        g.fillRect(
            sx(barX + barW * yellowStart / 1000),
            sy(barY),
            sx(barW * yellowWidth / 1000),
            sy(barH)
        );

        // วาดแถบสีเขียว
        g.setColor(Color.GREEN);
        g.fillRect(
            sx(barX + barW * greenStart / 1000),
            sy(barY),
            sx(barW * greenWidth / 1000),
            sy(barH)
        );
        // เส้นแดง
        g.setColor(Color.RED);
        g.fillRect(sx(barX + barW * barPos / 1000) - 2, sy(barY) - 3, 4, sy(barH) + 6);

        // คะแนน (ชั่วคราว ไว้เช็กว่าทำงาน)
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, sx(1316), sy(167));

        // ข้อความผลการตกปลา
        if (!message.isEmpty()) {
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(message)) / 2;
        int textY = getHeight() / 2;
        g.drawString(message, textX, textY);
}
    }
    // ตรวจสอบคะแนน
    public void checkScore() {
        // ถ้าเส้นแดงหยุดอยู่แล้ว ไม่ต้องทำอะไร
        if (!isMoving) {
            return;
        }
        // หยุดเส้นแดง
        isMoving = false;
        // ตรวจสอบสีเขียวก่อน เพราะอาจอยู่ทับกับสีเหลือง
        if (barPos >= greenStart &&
            barPos <= greenStart + greenWidth) {
            int point = (int)(Math.random() * 10) + 1;
            score += point + 5;
            message = "Excellent! +" + (point + 5);
        } else if (barPos >= yellowStart &&
                barPos <= yellowStart + yellowWidth) {
            int point = (int)(Math.random() * 10) + 1;
            score += point;
            message = "Fishing Success! +" + point;
        } else {
            message = "Miss! +0";
        }
        // สุ่มแถบสีใหม่
        randomBar();
        // วาดข้อความและคะแนนใหม่
        repaint();
        // รอ 1.5 วินาที แล้วเริ่มรอบใหม่
        Timer nextRound = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                message = "";
                isMoving = true;
                repaint();
            }
        });
        nextRound.setRepeats(false);
        nextRound.start();
    }
    // สุ่มตำแหน่งและขนาดแถบสีเหลืองและสีเขียว
    public void randomBar() {
        yellowStart = (int)(Math.random() * 700);
        yellowWidth = (int)(Math.random() * 300) + 100;

        greenStart = yellowStart + (int)(Math.random() * (yellowWidth - 20));
        greenWidth = (int)(Math.random() * (yellowWidth - (greenStart - yellowStart))) + 20;
    }
}
