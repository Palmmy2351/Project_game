import java.io.*;
import java.net.*;
import java.util.*;

public class GameClient {

    public interface Listener {
        void onStart();
        void onTime(int secondsLeft);
        void onScores(Map<String, Integer> scores);
        void onCatch(String player, String fish, int points);
        void onGameOver(Map<String, Integer> finalScores);

        default void onPlayers(Map<String, Integer> characters) { }

        default void onCast(int speed, int yellowStart, int yellowWidth, int greenStart, int greenWidth) { }

        default void onResult(String fish, int points, boolean excellent) { }
    }

    private PrintWriter out;
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port, String username) throws IOException {
        Socket socket = new Socket(host, port);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("JOIN|" + username);

        Thread reader = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) handle(line);
            } catch (IOException e) {
                System.out.println("Disconnected from server");
            } finally {
                try { in.close(); } catch (IOException e) { }
                try { socket.close(); } catch (IOException e) { }
            }
        });
        reader.setDaemon(true);
        reader.start();
    }

    public void sendStart() {
        if (out != null) out.println("START");
    }

    public void sendCast() {
        if (out != null) out.println("CAST");
    }

    public void sendReel(long ticks) {
        if (out != null) out.println("REEL|" + ticks);
    }

    private void handle(String line) {
        String[] p = line.split("\\|");
        if (p.length == 0 || listener == null) return;

        try {
            switch (p[0]) {
                case "START":
                    listener.onStart();
                    break;
                case "TIME":
                    if (p.length >= 2) listener.onTime(Integer.parseInt(p[1]));
                    break;
                case "SCORES":
                    listener.onScores(parseScores(p.length > 1 ? p[1] : ""));
                    break;
                case "PLAYERS":
                    listener.onPlayers(parseScores(p.length > 1 ? p[1] : ""));
                    break;
                case "CATCH":
                    if (p.length >= 4) listener.onCatch(p[1], p[2], Integer.parseInt(p[3]));
                    break;
                case "CAST":
                    if (p.length >= 6) {
                        listener.onCast(Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                                Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]));
                    }
                    break;
                case "RESULT":
                    if (p.length >= 4) listener.onResult(p[1], Integer.parseInt(p[2]), "1".equals(p[3]));
                    break;
                case "GAMEOVER":
                    listener.onGameOver(parseScores(p.length > 1 ? p[1] : ""));
                    break;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid message received from server: " + line);
        }
    }

    private Map<String, Integer> parseScores(String s) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (s.isEmpty()) return result;

        for (String part : s.split(",")) {
            String[] kv = part.split(":");
            if (kv.length < 2) continue;
            try {
                result.put(kv[0], Integer.parseInt(kv[1]));
            } catch (NumberFormatException e) {
                System.out.println("Invalid score: " + part);
            }
        }
        return result;
    }
}