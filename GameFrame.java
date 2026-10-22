import javax.swing.*;

public class GameFrame extends JFrame {

    private GamePanel gamePanel;

    public GameFrame() {
        setTitle("Fishing Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gamePanel = new GamePanel();
        add(gamePanel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameFrame());
    }
}