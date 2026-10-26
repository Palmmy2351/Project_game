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
        // player name -> character id (sent whenever someone joins or leaves)
        default void onPlayers(Map<String, Integer> characters) {}
        // the server decided the speed and the zones of the fishing bar
        default void onCast(int speed, int yellowStart, int yellowWidth, int greenStart, int greenWidth) {}
        // the server's verdict: fish is a fish name, "MISS" or "NONE"
        default void onResult(String fish, int points, boolean excellent) {}
    }
    private PrintWriter out;
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }
    
    public void connect (String host, int port, String username) throws IOException {
        Socket socket = new Socket(host, port);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("JOIN|" + username);

        Thread readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    handle(line);
                }           
            } catch (IOException e) {
                System.out.println("Disconnected from server");
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void sendStart() {
        if (out != null) out.println("START");
    }

    // ask the server for a new bar (speed and zones are chosen by the server)
    public void sendCast() {
        if (out != null) out.println("CAST");
    }

    // tell the server how many steps the red line moved before SPACE was pressed
    public void sendReel(long ticks) {
        if (out != null) out.println("REEL|" + ticks);
    }
    
    private void handle(String line) {
        String[] parts = line.split("\\|");
        switch (parts[0]) {
            case "START":
                listener.onStart();
                break;
            case "TIME":
                listener.onTime(Integer.parseInt(parts[1]));
                break;
            case "SCORES":
                listener.onScores(parseScores(parts.length > 1 ? parts[1] : ""));
                break;
            case "PLAYERS":
                listener.onPlayers(parseScores(parts.length > 1 ? parts[1] : ""));
                break;
            case "CATCH":
                listener.onCatch(parts[1], parts[2], Integer.parseInt(parts[3]));
                break;
            case "CAST":
                listener.onCast(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3]), Integer.parseInt(parts[4]),
                        Integer.parseInt(parts[5]));
                break;
            case "RESULT":
                listener.onResult(parts[1], Integer.parseInt(parts[2]), "1".equals(parts[3]));
                break;
            case "GAMEOVER":
                listener.onGameOver(parseScores(parts.length > 1 ? parts[1] : ""));
                break;
        }
    }

    // Parses "name:number,name:number" and keeps the order sent by the server
    private Map<String, Integer> parseScores(String scoreString) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        if (scoreString.isEmpty()) return scores;
        for (String part : scoreString.split(",")) {
            String[] kv = part.split(":");
            scores.put(kv[0], Integer.parseInt(kv[1]));
        }
        return scores;
    }
    
}