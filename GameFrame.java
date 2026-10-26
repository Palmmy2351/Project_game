import java.awt.*;
import javax.swing.*;

public class GameFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameFrame::createAndShow);
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("Fishing Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        CardLayout cards = new CardLayout();
        JPanel root = new JPanel(cards);

        GameClient client = new GameClient();
        GamePanel gamePanel = new GamePanel(client);   // sets the listener before connecting

        ConnectPanel connectPanel = new ConnectPanel(client, lobbyInfo -> {
            gamePanel.setHostInfo(lobbyInfo);          // text shown at the top of the lobby
            cards.show(root, "game");                  // switch to the game screen
            gamePanel.requestFocusInWindow();
        });

        root.add(connectPanel, "connect");
        root.add(gamePanel, "game");

        frame.add(root);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}