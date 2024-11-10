import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class server {  // Changed from lowercase 'server' to uppercase 'Server'
    private static final int SERVER_PORT = 12345;
    private static final int WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
    private static final int HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
    private static final int FPS = 60;
    private static final ArrayList<ClientHandler> clients = new ArrayList<>();

    // Game state variables
    static List<Character> charactersOnField = new CopyOnWriteArrayList<>();
    
    public static void main(String[] args) {
        // Testing Characters
        Character c = new Character("Alice");
        charactersOnField.add(c);

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server started on port " + SERVER_PORT);

            while (clients.size() < 2) {
                Socket clientSocket = serverSocket.accept();
                int playerId = clients.size() + 1;
                ClientHandler clientHandler = new ClientHandler(clientSocket, playerId);
                clients.add(clientHandler);
                new Thread(clientHandler).start();

                // Send initial game state
                GameState initialState = new GameState(new ArrayList<>(charactersOnField));  // Create new ArrayList to avoid sharing references
                clientHandler.sendGameState(initialState);
            }

            // Start game loop
            long lastTime = System.nanoTime();
            double ns = 1000000000.0 / FPS;
            double delta = 0;

            while (true) {
                long now = System.nanoTime();
                delta += (now - lastTime) / ns;
                lastTime = now;

                if (delta >= 1) {
                    updateGameState();
                    broadcastGameState();
                    delta--;
                }

                Thread.sleep(1);  // Small sleep to prevent CPU overload
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void updateGameState() {
        for (Character ch : charactersOnField) {
            if (ch.direction == 2) {
                ch.x += 2;
            } else if (ch.direction == 1) {
                ch.x -= 2;
            }
        }
    }

    private static void broadcastGameState() {
        GameState gameState = new GameState(new ArrayList<>(charactersOnField));  // Create new ArrayList to avoid sharing references
        for (ClientHandler client : clients) {
            client.sendGameState(gameState);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final ObjectOutputStream out;
        private final ObjectInputStream in;
        private final int playerId;

        public ClientHandler(Socket socket, int playerId) throws IOException {
            this.socket = socket;
            this.playerId = playerId;
            
            // Important: Create ObjectOutputStream first and flush it
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();  // This is crucial
            this.in = new ObjectInputStream(socket.getInputStream());

            // Inform the client about its player ID
            out.writeInt(playerId);  // Changed from writeObject to writeInt for primitive types
            out.flush();
            System.out.println("Player " + playerId + " connected.");
        }

        @Override
        public void run() {
            try {
                while (!socket.isClosed()) {
                    Object input = in.readObject();
                    if (input instanceof PlayerAction) {
                        handlePlayerAction((PlayerAction) input);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client disconnected: " + e.getMessage());
                clients.remove(this);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handlePlayerAction(PlayerAction action) {
            synchronized (charactersOnField) {
                for (Character c : action.charactersOnField) {
                    charactersOnField.add(c);  // Create a deep copy of the character
                }
            }
        }

        public void sendGameState(GameState gameState) {
            try {
                out.reset();  // Reset the object output stream to prevent caching
                out.writeObject(gameState);
                out.flush();
                System.out.println("Sending state to player " + playerId);
            } catch (IOException e) {
                System.err.println("Error sending game state to player " + playerId + ": " + e.getMessage());
            }
        }
    }
}