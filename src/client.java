import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class client extends JFrame {
    // Server Stuff
    private static final String SERVER_ADDRESS = "10.84.83.113"; // Server's IP address
    private static final int SERVER_PORT = 12345;
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int FPS = 60;

    // Layout Stuff
    private JPanel cards;
    private CardLayout c;
    private JPanel menu, field;
    private int open;// What frame were on

    // Menu Stuff
    private JLabel title;
    private JButton goToField;
    private BufferedImage backgroundImage;

    // Game state variables
    ArrayList<Character> charactersOnField = new ArrayList<>();

    // Client Only Variables
    public int counter = 0;
    public int spriteNum = 1;

    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    private static int playerId;

    // public static void main(String[] args) {
    //     client client = new client();
    //     client.start();
    // }

    public client(int s, int t){

    }

    public client(){
        c = new CardLayout();
        cards = new JPanel(c);
        this.getContentPane().add(cards);
        cards.setVisible(true);

        //initliaze menu image here
        //image = ???

        // Menu Intitiliazation
        menu = new ImageBackgroundPanel("src/Background.png");
        menu.setLayout(null);
        repaint();

        title = new JLabel("Welcome To Wonderland");
        Font aliceFont = new Font("Serif", Font.PLAIN, 100);
        try {
            // Load the font file from the resources or file system
            aliceFont = Font.createFont(Font.TRUETYPE_FONT, new File("src/AW-Font.ttf"));
            aliceFont = aliceFont.deriveFont(140f); // Set the font size
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }
        title.setFont(aliceFont);
        title.setForeground(Color.CYAN);// Text Color
        title.setSize(1150, 100);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setLocation(200, 150);
        title.setVisible(true);
        menu.add(title);

        goToField = new JButton("Start");
        goToField.setSize(400, 200);
        goToField.setLocation(550, 350);
        goToField.setFont(aliceFont);
        goToField.setBackground(Color.GREEN);
        goToField.setForeground(Color.MAGENTA);
        goToField.setVisible(true);
        goToField.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                c.show(cards, "2");
                //client.start();
                // Do Start Stuff
                try {
                    // Establish a connection to the server
                    socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                    out = new ObjectOutputStream(socket.getOutputStream());
                    in = new ObjectInputStream(socket.getInputStream());
        
                    // Receive player ID from server
                    playerId = (Integer) in.readObject();
                    System.out.println("Connected as Player " + playerId);
        
                    // Start listening for server messages
                    new Thread(new ServerListener(field)).start();
        
                    // Handle key events for paddle movement
                    field.addKeyListener(new KeyAdapter() {
                        ArrayList<Character> newChars = new ArrayList<>();
                        @Override
                        public void keyPressed(KeyEvent e) {
                            String action = null;
                            //CHANGE AS NEEDED
                            if (e.getKeyCode() == KeyEvent.VK_UP) {
                                newChars.add(new Character("Alice"));
                            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                                action = "DOWN";
                            }
        
                            // Send player action to the server
                            if (action != null) {
                                try {//SEND PLAYER ACTION
                                    System.out.println("SEND ACTION");
                                    PlayerAction act = new PlayerAction(playerId);
                                    act.charactersOnField = newChars;
                                    out.writeObject(act);
                                    out.flush();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    });
                } catch (IOException | ClassNotFoundException er) {
                    er.printStackTrace();
                }
            }
        });
        menu.add(goToField);

        // Field Intitilization
        field = new GamePanel();
        field.setLayout(null);
        //set background image or color ???
        field.setVisible(true);
        field.setFocusable(true);
        field.requestFocusInWindow();

        cards.add(menu, "1");
        cards.add(field, "2");
        c.show(cards, "1");
        open = 1;
        repaint();

    }

    // Thread to listen for game state updates from the server
    private class ServerListener implements Runnable {

        JPanel frame;

        ServerListener(JPanel f){
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
                Character fixer = new Character(character.Name);
                BufferedImage image = null;
                if (character.direction == 1) {
                    image = (spriteNum == 1) ? fixer.left1 : fixer.left2;
                }
                else {
                    image = (spriteNum == 1) ? fixer.right1 : fixer.right2;
                }
                g.drawImage(image, character.x, character.y, character.size * character.scale, character.size * character.scale, null);
            }
            //characters all drawn

        }
    }
}

class ImageBackgroundPanel extends JPanel {
    private Image backgroundImage;

    // Constructor to load the image
    public ImageBackgroundPanel(String imagePath) {
        try {
            // Load the image from the file system (or use a resource path)
            backgroundImage = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Override paintComponent to draw the background image
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Call the superclass method to ensure the panel is properly rendered
        if (backgroundImage != null) {
            // Draw the background image (it will be resized to fill the panel)
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}