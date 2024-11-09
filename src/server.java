import java.io.*;
import java.net.*;
import java.util.*;

public class server {
    private static final int SERVER_PORT = 12345;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    //variables go here

    private static final List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server started on port " + SERVER_PORT);

            while (clients.size() < 2) {
                Socket clientSocket = serverSocket.accept();
                int playerId = clients.size() + 1; // Assigns player ID 1 or 2
                ClientHandler clientHandler = new ClientHandler(clientSocket, playerId);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }

            // Start game loop
            while (true) {
                updateGameState();
                broadcastGameState();
                Thread.sleep(16); // Approx 60 FPS
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    //update state of game -- this is a time tick; every tick update variables by constant (physics and shit)
    private static void updateGameState() {
        


    }

    private static void broadcastGameState() {
        String gameState = "var1" + "," + "var2";
        for (ClientHandler client : clients) {
            client.sendMessage(gameState);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final PrintWriter out;
        private final int playerId;

        public ClientHandler(Socket socket, int playerId) throws IOException {
            this.socket = socket;
            this.playerId = playerId;
            this.out = new PrintWriter(socket.getOutputStream(), true);

            // Send player ID to client
            out.println(playerId);
            System.out.println("Player " + playerId + " connected.");
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message;
                while ((message = in.readLine()) != null) {
                    handleClientMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleClientMessage(String message) {
            String[] parts = message.split(",");
            int id = Integer.parseInt(parts[0]);

            //handle clients input to update game variables
            if(id == 1){

            }
            if(id == 2){

            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}

