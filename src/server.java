import java.io.*;
import java.net.*;
import java.util.*;

public class server {
    private static final int SERVER_PORT = 12345;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private static int ballX = WIDTH / 2, ballY = HEIGHT / 2;
    private static int ballVelX = 2, ballVelY = 2;
    private static int paddle1Y = HEIGHT / 2 - 50;
    private static int paddle2Y = HEIGHT / 2 - 50;
    private static final int PADDLE_HEIGHT = 100;
    private static final int PADDLE_SPEED = 5;
    private static int score1 = 0, score2 = 0;

    private static final List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Pong server started on port " + SERVER_PORT);

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

    private static void updateGameState() {
        // Update ball position
        ballX += ballVelX;
        ballY += ballVelY;

        // Ball collision with top and bottom walls
        if (ballY <= 0 || ballY >= HEIGHT - 20) {
            ballVelY = -ballVelY;
        }

        // Ball collision with paddles
        if (ballX <= 45 && ballY >= paddle1Y && ballY <= paddle1Y + PADDLE_HEIGHT) {
            ballVelX = -ballVelX;
        } else if (ballX >= WIDTH - 65 && ballY >= paddle2Y && ballY <= paddle2Y + PADDLE_HEIGHT) {
            ballVelX = -ballVelX;
        }

        // Scoring
        if (ballX <= 0) {
            score2++;
            resetBall();
        } else if (ballX >= WIDTH - 20) {
            score1++;
            resetBall();
        }
    }

    private static void resetBall() {
        ballX = WIDTH / 2;
        ballY = HEIGHT / 2;
        ballVelX = -ballVelX; // Change direction after each score
    }

    private static void broadcastGameState() {
        String gameState = ballX + "," + ballY + "," + paddle1Y + "," + paddle2Y + "," + score1 + "," + score2;
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
            if (parts[0].equals("MOVE")) {
                int id = Integer.parseInt(parts[1]);
                String direction = parts[2];

                if (id == 1) {
                    if (direction.equals("UP") && paddle1Y > 0) {
                        paddle1Y -= PADDLE_SPEED;
                    } else if (direction.equals("DOWN") && paddle1Y < HEIGHT - PADDLE_HEIGHT) {
                        paddle1Y += PADDLE_SPEED;
                    }
                } else if (id == 2) {
                    if (direction.equals("UP") && paddle2Y > 0) {
                        paddle2Y -= PADDLE_SPEED;
                    } else if (direction.equals("DOWN") && paddle2Y < HEIGHT - PADDLE_HEIGHT) {
                        paddle2Y += PADDLE_SPEED;
                    }
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}

