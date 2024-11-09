import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class server {
    private static final int SERVER_PORT = 12345;
    private static final int WIDTH =Toolkit.getDefaultToolkit().getScreenSize().width ;
    private static final int HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
    private static final int FPS = 60;
    private static final ArrayList<ClientHandler> clients = new ArrayList<>();

    // Game state variables
    static ArrayList<Character> charactersOnField = new ArrayList<>();
    
        public static void main(String[] args) {
            Character c = new Character("Alice");
            try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
                System.out.println("Server started on port " + SERVER_PORT);
    
                while (clients.size() < 2) {
                    Socket clientSocket = serverSocket.accept();
                    int playerId = clients.size() + 1; // Assigns player ID 1 or 2
                    ClientHandler clientHandler = new ClientHandler(clientSocket, playerId);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                }
    
                //Testing Characters
                charactersOnField.add(new Character("Alice"));

                // Start game loop
                while (true) {
                    updateGameState();
                    broadcastGameState();
                    //double drawInterval = 1000000000/FPS;
                    //double nextDrawTime = System.nanoTime()+drawInterval;
                    //double remainingTime = nextDrawTime-System.nanoTime();
                    Thread.sleep(16);// roughly 60 FPS
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

    //update state of game each tick -- change variables every time step
    private static void updateGameState() {
        //update charactersOnField as necesassry
        charactersOnField.add(new Character("Alice"));
    }

    private static void broadcastGameState() {//update GameState as needed (new variables)
        GameState gameState = new GameState(charactersOnField);
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
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Player " + playerId + " connected.");
        }

        @Override
        public void run() {
            try {
                // Send initial player ID to the client
                out.writeObject(playerId);

                while (true) {
                    PlayerAction action = (PlayerAction) in.readObject();
                    handlePlayerAction(action);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        //add functionality here
        private void handlePlayerAction(PlayerAction action) {
            charactersOnField = action.charactersOnField;
            if (action.playerId == 1) {
                
            }
            if (action.playerId == 2) {

            }
        }

        public void sendGameState(GameState gameState) {
            try {
                out.writeObject(gameState);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}