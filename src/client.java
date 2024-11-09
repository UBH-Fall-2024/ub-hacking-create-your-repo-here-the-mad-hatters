import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
public class client {
    private static final String SERVER_ADDRESS = "10.84.83.113"; // Change to the server's IP address if remote
    private static final int SERVER_PORT = 12345;
    private static final int WIDTH =Toolkit.getDefaultToolkit().getScreenSize().width ;
    private static final int HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
    private static final int FPS = 60;

    // Game state variables
    
    private static int playerId = -1; // Will be set by server (1 or 2)

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) {
        client client = new client();
        client.start();
    }

    // Start the client
    public void start() {
        try {
            // Establish a connection to the server
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Read and set the player's ID (1 or 2) as assigned by the server
            playerId = Integer.parseInt(in.readLine());
            System.out.println("Connected as Player " + playerId);

            // Set up the game window
            JFrame frame = new JFrame("Pong Multiplayer");
            frame.setSize(WIDTH, HEIGHT);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new GamePanel());
            frame.setVisible(true);

            // Start listening for server messages
            new Thread(new ServerListener()).start();

            // Handle key events for paddle movement
            frame.addMouseListener(new MouseAdapter() {
              

            }); 
                
           
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Thread to listen for game state updates from the server
    private class ServerListener implements Runnable {
        @Override
        public void run() {
            


            try {
                String message;
                while ((message = in.readLine()) != null) {
                    // Split the message to extract game state values
                    String[] parts = message.split(",");
                   
                    // Trigger a repaint on the game window
                    SwingUtilities.invokeLater(() -> {
                        JFrame.getFrames()[0].repaint();
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Game panel to render paddles, ball, and scores
    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
        }
    }
}
