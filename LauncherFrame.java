import java.awt.*;
import java.net.*;
import javax.swing.*;

public class LauncherFrame extends JFrame {

    private JTextField ipField;
    private JButton connectButton;

    private static final int PORT = 50101;

    public LauncherFrame() {
        setTitle("Fishing Game Launcher");
        setSize(400, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Server IP:"));

        ipField = new JTextField("127.0.0.1");
        panel.add(ipField);

        connectButton = new JButton("Connect");
        panel.add(connectButton);

        connectButton.addActionListener(e -> connectToServer());

        add(panel);
    }

    private void connectToServer() {
        String ip = ipField.getText().trim();

        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Server IP");
            return;
        }

        connectButton.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try (Socket socket = new Socket()) {
                    socket.connect(
                        new InetSocketAddress(ip, PORT), 3000
                    );
                    return true;
                }
            }

            @Override
            protected void done() {
                connectButton.setEnabled(true);

                try {
                    if (get()) {
                        dispose();
                        new GameFrame();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        LauncherFrame.this,
                        "Cannot connect to Server " + ip + ":" + PORT
                            + "\nPlease check that the Server is running.",
                        "Connection Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LauncherFrame().setVisible(true);
        });
    }
}