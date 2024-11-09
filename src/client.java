import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class client {
    private static final String SERVER_ADDRESS = "10.84.83.113"; // Server's IP address
    private static final int SERVER_PORT = 12345;
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int FPS = 60;

    // Game state variables
    ArrayList<Character> charactersOnField = new ArrayList<>();

    // Client Only Variables
    public int counter = 0;
    public int spriteNum;

    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    private static int playerId;

    public static void main(String[] args) {
        client client = new client();
        client.start();
    }

    // Start the client
    public void start() {
        try {
            // Establish a connection to the server
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Receive player ID from server
            playerId = (Integer) in.readObject();
            System.out.println("Connected as Player " + playerId);

            // Set up the game window
            JFrame frame = new JFrame("Game Title");//SET TITLE
            frame.setSize(WIDTH, HEIGHT);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new GamePanel());
            frame.setVisible(true);
            // Make sure the frame can get key events
            frame.setFocusable(true);
            frame.requestFocusInWindow();

            // Start listening for server messages
            new Thread(new ServerListener(frame)).start();

            // Handle key events for paddle movement
            frame.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    String action = null;
                    //CHANGE AS NEEDED
                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        charactersOnField.add(new Character("Alice"));
                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        action = "DOWN";
                    }

                    // Send player action to the server
                    if (action != null) {
                        try {//SEND PLAYER ACTION
                            PlayerAction act = new PlayerAction(playerId);
                            act.charactersOnField = charactersOnField;
                            out.writeObject(act);
                            out.flush();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Thread to listen for game state updates from the server
    private class ServerListener implements Runnable {

        JFrame frame;

        ServerListener(JFrame f){
            this.frame = f;
        }

        @Override
        public void run() {
            


            try {
                while (true) {
                    // Receive GameState object from the server
                    GameState gameState = (GameState) in.readObject();

                    // Update game state variables
                    charactersOnField = gameState.charactersOnField;

                    //update local animations
                    counter++;
                    if (counter >= (FPS / 2)) { // Smooth animation
                        spriteNum = (spriteNum == 1) ? 2 : 1;
                        counter = 0;
                    }

                    // Trigger a repaint on the game window
                    SwingUtilities.invokeLater(() -> {
                    //    JFrame.getFrames()[0].repaint();
                        frame.repaint();
                    });
                    //frame.repaint();
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    // Game panel to render paddles, ball, and scores
    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            //Graphics2D g2 = (Graphics2D)g;

            g.setColor(Color.GRAY);
            g.fillRect(0, 0, getWidth(), getHeight()); // Clear the background

            // Draw Game

            // Draw Characters
            for(Character character : charactersOnField){
                BufferedImage image = null;
                if (character.direction == 1) {
                    image = (spriteNum == 1) ? character.left1 : character.left2;
                } else if (character.direction == 2) {
                    image = (spriteNum == 1) ? character.right1 : character.right2;
                }
                if (image != null) {
                    int dimm = character.size * character.scale;
                    g.drawImage(image, character.x, character.y, dimm, dimm, null);
                }
            }
            //characters all drawn

        }
    }
}