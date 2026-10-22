import java.io.*;
import java.net.*;
import java.util.*;
public class GameServer {
    public  static final int PORT = 50101;
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    public static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Fishing Server started on port  " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                synchronized (clientHandlers) {
                    clientHandlers.add(clientHandler);
                }
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
    public static void broadcastBoard(){
        StringBuilder board = new StringBuilder("BOARD\t");
        boolean first = true;

        synchronized (clientHandlers) {
            for (ClientHandler clientHandler : clientHandlers) {
                if (!first) {
                    board.append(",");
                }
                board.append(clientHandler.getPlayerName()).append(":").append(clientHandler.getScore());
                first = false;
            }
            for (ClientHandler clientHandler : clientHandlers) {
                clientHandler.send(board.toString());
            }
        }
        System.out.println("Leaderboard updated.");
    }
    public static void removeClient(ClientHandler clientHandler) {
        synchronized (clientHandlers) {
            clientHandlers.remove(clientHandler);
        }
        broadcastBoard();
    }
}
