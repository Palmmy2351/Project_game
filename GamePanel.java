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

    public GamePanel() {
        setPreferredSize(new Dimension(800, 565));

        // Timer: ทุก 30 ms เลื่อนเส้นแดง แล้ววาดใหม่
        Timer timer = new Timer(30, e -> {
            barPos += dir * 10;
            if (barPos >= 1000) { barPos = 1000; dir = -1; }
            if (barPos <= 0)    { barPos = 0;    dir = 1;  }
            repaint();
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
        // โซนเหลือง
        g.setColor(Color.YELLOW);
        g.fillRect(sx(barX + barW * 300 / 1000), sy(barY), sx(barW * 400 / 1000), sy(barH));
        // โซนเขียว
        g.setColor(Color.GREEN);
        g.fillRect(sx(barX + barW * 470 / 1000), sy(barY), sx(barW * 60 / 1000), sy(barH));
        // เส้นแดง
        g.setColor(Color.RED);
        g.fillRect(sx(barX + barW * barPos / 1000) - 2, sy(barY) - 3, 4, sy(barH) + 6);

        // คะแนน (ชั่วคราว ไว้เช็กว่าทำงาน)
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, sx(1316), sy(167));
    }
    // ตรวจสอบคะแนน
    public void checkScore() {
        if (barPos >= 470 && barPos <= 530) {
            // โซนสีเขียว (เช็กก่อน เพราะอยู่ข้างในโซนเหลือง)
            score += (int)(Math.random() * 10) + 1;   // คะแนนสุ่ม 1-10
                score += 5;                            // โบนัส +5
            System.out.println("เขียว  คะแนน=" + score);
        } else if (barPos >= 300 && barPos <= 700) {
            // โซนสีเหลือง
            score += (int)(Math.random() * 10) + 1;   // คะแนนสุ่ม 1-10
            System.out.println("เหลือง คะแนน=" + score);
        } else {
            // โซนสีเทา
            System.out.println("เทา    คะแนน=" + score);
        }
        repaint();
    }
}
