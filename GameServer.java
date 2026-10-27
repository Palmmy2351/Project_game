import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    static final int PORT = 50101; 

    static final int CHARACTER_COUNT = 6;

    private final List<ClientHandler> clients = new ArrayList<>();
    private final Map<String, Integer> scores = new LinkedHashMap<>();
    private final Map<String, Integer> characters = new LinkedHashMap<>();
    private final Random random = new Random();
    private final FishManager fishManager = new FishManager();
    private boolean running = false; 
    private int timeLeft = 0;
    private int roundId = 0;       

    private ServerSocket serverSocket;

    private void log(String msg) {
        System.out.println(msg);
    }

    public static void main(String[] args) throws IOException {
        new GameServer().start(PORT);
    }

    public static String localAddresses() {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(a.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return sb.length() == 0 ? "localhost" : sb.toString();
    }

    public void start(int port) throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            return; 
        }

        serverSocket = new ServerSocket(port); 
        final ServerSocket ss = serverSocket;
        log("Server started on port " + port);

        Thread acceptThread = new Thread(() -> {
            while (!ss.isClosed()) {
                Socket socket;
                try {
                    socket = ss.accept();
                } catch (IOException e) {
                    break; 
                }
                try {
                    new Thread(new ClientHandler(socket)).start();
                } catch (IOException e) {
                    log("Could not accept a client: " + e.getMessage());
                }
            }
        });
        acceptThread.start();
    }

    public synchronized void stop() {
        roundId++;       
        running = false;

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }

        for (ClientHandler c : new ArrayList<>(clients)) {
            c.close();
        }
        clients.clear();
        scores.clear();
        characters.clear();
        log("Server stopped");
    }

    private synchronized void broadcast(String msg) {
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }

    private String scoreString() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : list) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        return sb.toString();
    }

    private int pickCharacter() {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < CHARACTER_COUNT; i++) {
            if (!characters.containsValue(i)) {
                free.add(i);
            }
        }
        if (free.isEmpty()) {
            return random.nextInt(CHARACTER_COUNT);
        }
        return free.get(random.nextInt(free.size()));
    }

    private String playersString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : characters.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        return sb.toString();
    }

    synchronized void join(ClientHandler h, String rawName) {
        if (h.name != null) {
            return; 
        }
        String base = rawName.replaceAll("[|:,]", "").trim();
        if (base.isEmpty()) base = "Player";
        String name = base;
        int i = 2;
        while (scores.containsKey(name)) {
            name = base + i++;
        }
        h.name = name;
        clients.add(h);
        scores.put(name, 0);
        characters.put(name, pickCharacter());
        log(name + " joined (" + clients.size() + " players)");
        broadcast("PLAYERS|" + playersString());
        broadcast("SCORES|" + scoreString());
    }

    synchronized void leave(ClientHandler h) {
        if (!clients.remove(h)) {
            return; // already removed (for example by stop())
        }
        if (h.name != null) {
            scores.remove(h.name);
            characters.remove(h.name);
            log(h.name + " left");
            broadcast("PLAYERS|" + playersString());
            broadcast("SCORES|" + scoreString());
            }
    }

    synchronized void onCast(ClientHandler h) {
        if (!running || h.name == null) return;
        if (h.castPending) return;

        h.castSpeed = random.nextInt(46) + 5;
        h.yellowWidth = random.nextInt(300) + 100;
        h.yellowStart = random.nextInt(1001 - h.yellowWidth);
        h.greenWidth = Math.max(1, h.yellowWidth / 10);
        h.greenStart = h.yellowStart + random.nextInt(h.yellowWidth - h.greenWidth + 1);
        h.castTimeNanos = System.nanoTime();
        h.castPending = true;

        h.send("CAST|" + h.castSpeed + "|" + h.yellowStart + "|" + h.yellowWidth + "|" + h.greenStart + "|" + h.greenWidth);
    }

    synchronized void onReel(ClientHandler h, long ticks) {
        if (!running || h.name == null || !h.castPending) return;
        h.castPending = false;

        long elapsedMs = (System.nanoTime() - h.castTimeNanos) / 1_000_000;
        if (ticks < 0 || ticks > elapsedMs / 10 + 10) {
            log(h.name + " sent an impossible reel (" + ticks + " ticks in " + elapsedMs + " ms)");
            h.send("RESULT|MISS|0|0");
            return;
        }

        int pos = 0;
        int dir = 1;
        for (long t = 0; t < ticks; t++) {
            pos += dir * h.castSpeed;
            if (pos >= 1000) {
                pos = 1000;
                dir = -1;
            }
            if (pos <= 0) {
                pos = 0;
                dir = 1;
            }
        }

        boolean excellent = pos >= h.greenStart && pos <= h.greenStart + h.greenWidth;
        boolean hit = pos >= h.yellowStart && pos <= h.yellowStart + h.yellowWidth;
        if (!excellent && !hit) {
            h.send("RESULT|MISS|0|0");
            return;
        }

        Fish fish = fishManager.catchFish(h.castSpeed);
        if (fish == null) {
            h.send("RESULT|NONE|0|0");
            return;
        }

        int points = fish.getPoints() + (excellent ? 5 : 0);
        scores.merge(h.name, points, Integer::sum);
        log(h.name + " caught " + fish.getName() + " +" + points);
        h.send("RESULT|" + fish.getName() + "|" + points + "|" + (excellent ? 1 : 0));
        broadcast("CATCH|" + h.name + "|" + fish.getName() + "|" + points);
        broadcast("SCORES|" + scoreString());
    }

    synchronized void startRound() {
        if (running) return;
        running = true;
        timeLeft = 60;
        final int round = ++roundId;
        for (String k : scores.keySet()) {
            scores.put(k, 0); 
        }
        for (ClientHandler c : clients) {
            c.castPending = false;
        }
        log("Round started");
        broadcast("START");
        broadcast("TIME|" + timeLeft);
        broadcast("SCORES|" + scoreString());

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
                synchronized (GameServer.this) {
                    if (!running || round != roundId) {
                        return; 
                    }
                    timeLeft--;
                    broadcast("TIME|" + timeLeft);
                    if (timeLeft <= 0) {
                        running = false;
                        log("Round finished");
                        broadcast("GAMEOVER|" + scoreString());
                        return;
                    }
                }
            }
        }).start();
    }

    class ClientHandler implements Runnable {
        private final Socket socket;
        private final PrintWriter out;
        String name;
        boolean castPending = false;
        int castSpeed, yellowStart, yellowWidth, greenStart, greenWidth;
        long castTimeNanos;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        }

        void send(String msg) {
            out.println(msg);
        }

        void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] p = line.split("\\|");
                    switch (p[0]) {
                        case "JOIN":
                            join(this, p.length > 1 ? p[1] : "Player");
                            break;
                        case "START":
                            startRound();
                            break;
                        case "CAST":
                            onCast(this);
                            break;
                        case "REEL":
                            if (p.length >= 2) {
                                onReel(this, Long.parseLong(p[1]));
                            }
                            break;
                    }
                }
            } catch (IOException | RuntimeException e) {
            } finally {
                leave(this);
                close();
            }
        }
    }
}