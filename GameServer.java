import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    static final int PORT = 50101;
    static final Set<ClientHandler> clients = new HashSet<>();
    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PORT)){
            System.err.println("Fishing Server started on por" + PORT);
            while (true) { 
                Socket socket = server.accept();
                System.err.println("Player connected: " + socket.getInetAddress());

                ClientHandler client = new ClientHandler(socket);

                synchronized (clients) {
                    clients.add(client);
                }
                
                client.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
    static void broadcast() {
        StringBuilder board = new StringBuilder("BOARD|");

        synchronized (clients) {
            for(ClientHandler c : clients){
                if (c.name != null){
                    if (board.length() > 6){
                        board.append(",");
                    }

                    board.append(c.name).append(":").append(c.score);
                }
            }
            String data = board.toString();

            for (ClientHandler c : clients) {
                if (c.out != null){
                    c.out.println(data);
                }  
            }
        }
    }
    static class ClientHandler extends Thread{
        Socket socket;
        BufferedReader in;
        PrintWriter out;
        String name;
        int score = 0;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run(){
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(),true);
                String msg;
                while((msg = in.readLine()) != null){
                    String[] data = msg.split("\\|", 2);
                    
                    if (data.length != 2){
                        continue;
                    }

                    if (data[0].equals("JOIN")){
                        name = data[1].replaceAll("[,:|]", "_").trim();
                        if (name.isEmpty()){
                            name = "Player";
                        }
                        broadcast();
                    } else if (data[0].equals("SCORE") && name != null){
                        try {
                            int points = Integer.parseInt(data[1]);
                            if (points >= 1 && points <= 15) {   
                                score += points;
                                broadcast();       
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid score received");
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Player disconnected: " + name);
            } finally {
                synchronized (clients) {
                    clients.remove(this);
                }
                broadcast();

                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
