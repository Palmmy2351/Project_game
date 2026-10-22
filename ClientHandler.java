import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String playerName = "Player";
    private int score = 0;
    private boolean joined  = false;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }
    public String getEncodedName() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(playerName.getBytes(StandardCharsets.UTF_8));
    }
    public synchronized int getScore() {
        return score;
    }
    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }  
    public String getPlayerName() {
        return playerName;
    }
    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            String message;

            while ((message = in.readLine()) != null) {
                String[] parts = message.split("\t",-1);
                if (parts[0].equals("JOIN") && !joined && parts.length == 2){
                    String decoded = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8).trim();
                    if(decoded.isEmpty() || decoded.length() > 20){
                        break;
                    }
                    playerName = decoded;
                    joined = true;
                    System.out.println(playerName + " joined.");
                    GameServer.broadcastBoard();

                }else if (parts[0].equals("SCORE") && joined && parts.length == 2){
                    try {
                        int newScore = Integer.parseInt(parts[1]);
                        if (newScore > 0 && newScore <= 1000) {
                            synchronized (this) {
                                score = Math.min(1000000, score + newScore);
                            }
                            GameServer.broadcastBoard();
                        }
                    } catch (NumberFormatException e) {
                        send("ERROR\tInvalid score");
                    }
                } else {
                    System.out.println("Invalid message from " + playerName + ": " + message);
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + playerName);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {}
            if (joined) {
                 GameServer.removeClient(this);
            }
        }
    }
}
