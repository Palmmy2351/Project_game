import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;
public class GameClient {
    private Socket socket;
    private PrintWriter out;
    private Consumer<String> boardListener;
    private Consumer<String> statusListener;

    public GameClient(Consumer<String> boardListener,Consumer<String> statusListener){
        this.boardListener = boardListener;
        this.statusListener = statusListener;
    }

    public void connect(String ip,String name){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(ip, GameServer.PORT);
                    socket.connect(new InetSocketAddress(ip, GameServer.PORT), 3000);
                    out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    String encodedName = Base64.getUrlEncoder().withoutPadding().encodeToString(name.getBytes(StandardCharsets.UTF_8));
                    out.println("JOIN\t" + encodedName);
                    statusListener.accept("Connected to server.");
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("BOARD\t")) {
                            boardListener.accept(message.substring(6));
                        } else if (message.startsWith("ERROR\t")) {
                            statusListener.accept(message.substring(6));
                        }
                    }
                } catch (IOException e) {
                    statusListener.accept("Connection error: " + e.getMessage());
                }
            }
        };
        new Thread(task).start();
    }
    public synchronized void sendScore(int score) {
        if (out != null && score > 0 && score <= 1000) {
            out.println("SCORE\t" + score);
        }
    }
    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // Handle the exception if needed
        }
    }
    
}
