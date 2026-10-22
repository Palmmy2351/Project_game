import javax.swing.*;

public class GameFrame extends JFrame {

    private GamePanel GamePanel;

    public GameFrame() {
        setTitle("Fishing Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GamePanel = new GamePanel();
        add(GamePanel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameFrame());
    }
}
