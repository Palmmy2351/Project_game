import java.awt.*;
import java.io.IOException;
import java.util.function.Consumer;
import javax.swing.*;

public class ConnectPanel extends JPanel {

    private final JTextField nameField = new JTextField("Player", 15);
    private final JTextField ipField = new JTextField("localhost", 15);
    private final JTextField portField = new JTextField(String.valueOf(GameServer.PORT), 15);
    private final JButton joinButton = new JButton("Join Game");
    private final JButton hostButton = new JButton("Host Game");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

    private final GameClient client;
    private final GameServer server = new GameServer(); // only started when the player hosts

    // Receives the text for the lobby (where to join / what we are connected to)
    private final Consumer<String> onConnected;

    public ConnectPanel(GameClient client, Consumer<String> onConnected) {
        this.client = client;
        this.onConnected = onConnected;

        setPreferredSize(new Dimension(1000, 563)); // same size as GamePanel
        setLayout(new GridBagLayout());            // centers the form

        JLabel title = new JLabel("Fishing Game", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 32));

        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.add(joinButton);
        buttons.add(hostButton);

        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
        form.add(title);
        form.add(new JLabel("Your name:"));
        form.add(nameField);
        form.add(new JLabel("Server IP (to join):"));
        form.add(ipField);
        form.add(new JLabel("Port:"));
        form.add(portField);
        form.add(buttons);
        form.add(statusLabel);
        add(form);

        joinButton.addActionListener(e -> connect(ipField.getText().trim(), false));
        hostButton.addActionListener(e -> connect("localhost", true));
        // Pressing Enter in a text field joins
        for (JTextField field : new JTextField[]{nameField, ipField, portField}) {
            field.addActionListener(e -> connect(ipField.getText().trim(), false));
        }
    }

    // Returns -1 if the port text is not a valid port number
    private int readPort() {
        try {
            int p = Integer.parseInt(portField.getText().trim());
            return (p >= 1 && p <= 65535) ? p : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        joinButton.setEnabled(enabled);
        hostButton.setEnabled(enabled);
    }

    // hosting = true: start a server inside this program, then join it as "localhost"
    private void connect(String host, boolean hosting) {
        if (!joinButton.isEnabled()) {
            return; // already connecting
        }

        String name = nameField.getText().trim();
        int port = readPort();

        if (name.isEmpty() || host.isEmpty()) {
            statusLabel.setText("Please enter your name and the server IP");
            return;
        }
        if (port < 0) {
            statusLabel.setText("Invalid port (use 1-65535)");
            return;
        }

        setButtonsEnabled(false);
        statusLabel.setText(hosting ? "Starting server..." : "Connecting...");

        if (hosting) {
            try {
                server.start(port);
            } catch (IOException ex) {
                statusLabel.setText("Cannot start server: " + ex.getMessage());
                setButtonsEnabled(true);
                return;
            }
        }

        // The lobby shows this text at the top
        final String lobbyInfo = hosting
                ? "Hosting on port " + port + " - friends join at: " + GameServer.localAddresses()
                : "Connected to " + host + ":" + port;

        // Connect on a separate thread so the window does not freeze
        new Thread(() -> {
            try {
                client.connect(host, port, name);
                SwingUtilities.invokeLater(() -> onConnected.accept(lobbyInfo));
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Cannot connect: " + ex.getMessage());
                    setButtonsEnabled(true);
                    if (hosting) {
                        server.stop();
                    }
                });
            }
        }).start();
    }
}