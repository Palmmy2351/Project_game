
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.swing.*;

public class ClientWindow extends JFrame {

    private final JLabel statusLabel = new JLabel("Connecting...");
    private final JTextArea boardArea = new JTextArea();
    private final GameClient client;

    public ClientWindow(String name, String ip) {
        setTitle("Fishing Game - " + name);
        setSize(450, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        client = new GameClient(
                this::updateBoard,
                this::updateStatus
        );

        boardArea.setEditable(false);
        boardArea.setFont(new Font("Monospaced", Font.PLAIN, 16));

        JButton testButton = new JButton("Test: Earn 10 Points");
        testButton.addActionListener(e -> client.sendScore(10));

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.add(statusLabel, BorderLayout.CENTER);
        bottom.add(testButton, BorderLayout.SOUTH);

        add(new JLabel("  FISHING LEADERBOARD", JLabel.CENTER),
                BorderLayout.NORTH);
        add(new JScrollPane(boardArea), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                client.close();
            }
        });

        client.connect(ip, name);
    }

    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }

    private void updateBoard(String data) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder text = new StringBuilder();
            text.append(String.format("%-22s %s%n", "PLAYER", "SCORE"));
            text.append("--------------------------------\n");

            if (!data.isEmpty()) {
                String[] players = data.split(",");

                for (String player : players) {
                    String[] parts = player.split(":", 2);

                    if (parts.length == 2) {
                        try {
                            String playerName = new String(
                                    Base64.getUrlDecoder().decode(parts[0]),
                                    StandardCharsets.UTF_8);

                            int score = Integer.parseInt(parts[1]);
                            text.append(String.format("%-22s %d%n",
                                    playerName, score));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }

            boardArea.setText(text.toString());
        });
    }
}
