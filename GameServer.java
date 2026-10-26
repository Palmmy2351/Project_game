import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.Consumer;

public class GameServer {
    static final int PORT = 50101; // default port

    // NEW: how many character images exist (characters/char1.png ... charN.png)
    static final int CHARACTER_COUNT = 6;

    private final List<ClientHandler> clients = new ArrayList<>();
    private final Map<String, Integer> scores = new LinkedHashMap<>();
    // NEW: player name -> character id, in join order
    private final Map<String, Integer> characters = new LinkedHashMap<>();
    private final Random random = new Random();
    private boolean running = false; // true while a round is in progress
    private int timeLeft = 0;
    private int roundId = 0;         // lets an old countdown thread know it is outdated

    private ServerSocket serverSocket;

    // Callbacks so a window can show the log and the player list
    private Consumer<String> logger = System.out::println;
    private Consumer<List<String>> playerListener = names -> {};

    public void setLogger(Consumer<String> logger) {
        this.logger = logger;
    }

    public void setPlayerListener(Consumer<List<String>> playerListener) {
        this.playerListener = playerListener;
    }

    private void log(String msg) {
        logger.accept(msg);
    }

    private void notifyPlayers() {
        playerListener.accept(new ArrayList<>(scores.keySet()));
    }

    // Console mode still works: java GameServer
    public static void main(String[] args) throws IOException {
        new GameServer().start(PORT);
    }

    // IPv4 addresses of this machine, so the host can tell friends what to type
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

    // start() returns right away; accepting runs on its own thread
    public void start(int port) throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            return; // already running
        }

        serverSocket = new ServerSocket(port); // throws if the port is already in use
        final ServerSocket ss = serverSocket;
        log("Server started on port " + port);

        Thread acceptThread = new Thread(() -> {
            while (!ss.isClosed()) {
                Socket socket;
                try {
                    socket = ss.accept();
                } catch (IOException e) {
                    break; // the socket was closed by stop()
                }
                try {
                    // One thread per client
                    new Thread(new ClientHandler(socket)).start();
                } catch (IOException e) {
                    log("Could not accept a client: " + e.getMessage());
                }
            }
        });
        acceptThread.start();
    }

    // Stop the server and disconnect everyone
    public synchronized void stop() {
        roundId++;       // outdated countdown threads will exit
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
        characters.clear();   // NEW
        notifyPlayers();
        log("Server stopped");
    }

    // ---------- all methods below are synchronized because many threads call them ----------

    synchronized void broadcast(String msg) {
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

    // NEW: pick a random character nobody is using yet (random repeat if all are taken)
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

    // NEW: "name:characterId,name:characterId" in join order
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
            return; // CHANGED: ignore a second JOIN from the same client
        }
        // Remove characters that would break the protocol
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
        characters.put(name, pickCharacter());   // NEW
        log(name + " joined (" + clients.size() + " players)");
        broadcast("PLAYERS|" + playersString()); // NEW
        broadcast("SCORES|" + scoreString());
        notifyPlayers();
    }

    synchronized void leave(ClientHandler h) {
        if (!clients.remove(h)) {
            return; // already removed (for example by stop())
        }
        if (h.name != null) {
            scores.remove(h.name);
            characters.remove(h.name);               // NEW
            log(h.name + " left");
            broadcast("PLAYERS|" + playersString()); // NEW
            broadcast("SCORES|" + scoreString());
            notifyPlayers();
        }
    }

    synchronized void onCatch(String player, String fish, int points) {
        if (!running || player == null) return;
        scores.merge(player, points, Integer::sum);
        log(player + " caught " + fish + " +" + points);
        broadcast("CATCH|" + player + "|" + fish + "|" + points);
        broadcast("SCORES|" + scoreString());
    }

    synchronized void startRound() {
        if (running) return;
        running = true;
        timeLeft = 60;
        final int round = ++roundId;
        for (String k : scores.keySet()) {
            scores.put(k, 0); // reset scores (not a structural change, safe while iterating)
        }
        log("Round started");
        broadcast("START");
        broadcast("TIME|" + timeLeft);
        broadcast("SCORES|" + scoreString());

        // Countdown thread: the server owns the clock
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
                synchronized (GameServer.this) {
                    if (!running || round != roundId) {
                        return; // the server was stopped
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

    // ---------- one handler per connected client ----------
    class ClientHandler implements Runnable {
        private final Socket socket;
        private final PrintWriter out;
        String name;

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
                        case "CATCH":
                            if (p.length >= 3) {
                                onCatch(name, p[1], Integer.parseInt(p[2]));
                            }
                            break;
                    }
                }
            } catch (IOException | RuntimeException e) {
                // client disconnected or sent a bad message
            } finally {
                leave(this);
                close();
            }
        }
    }
}