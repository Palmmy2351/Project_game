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

    public void sendCatch(String fish, int points) {
        if (out != null) out.println("CATCH|" + fish + "|" + points);
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
            case "CATCH":
                listener.onCatch(parts[1], parts[2], Integer.parseInt(parts[3]));
                break;
            case "GAMEOVER":
                Map<String, Integer> finalScores = new HashMap<>();
                listener.onGameOver(parseScores(parts.length > 1 ? parts[1] : ""));
                break;
        }
    }
    private Map<String, Integer> parseScores(String scoreString) {
        Map<String, Integer> scores = new HashMap<>();
        if (scoreString.isEmpty()) return scores;
        for (String part : scoreString.split(",")) {
            String[] kv = part.split(":");
            scores.put(kv[0], Integer.parseInt(kv[1]));
        }
        return scores;
    }
    
}
