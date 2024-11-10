import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class client extends JFrame {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int FPS = 60;

    private JPanel cards;
    private CardLayout cardLayout;
    private JPanel menu, field;
    private volatile boolean isRunning = true;

    // Game state variables
    private volatile List<Character> charactersOnField = new CopyOnWriteArrayList<>();
    private volatile int counter = 0;
    private volatile int spriteNum = 1;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private int playerId;
    private volatile boolean connected = false;

    public client() {
        setTitle("Wonderland Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        initializeUI();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }

    private void initializeUI() {
        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        
        // Initialize menu
        initializeMenu();
        
        // Initialize game field
        initializeField();
        
        cards.add(menu, "MENU");
        cards.add(field, "GAME");
        
        add(cards);
        cardLayout.show(cards, "MENU");
    }

    private void initializeMenu() {
        menu = new ImageBackgroundPanel("src/aliceTitleScreen.jpg");
        menu.setLayout(null);

        JLabel title = createTitleLabel();
        JButton startButton = createStartButton();

        menu.add(title);
        menu.add(startButton);
    }

    private JLabel createTitleLabel() {
        JLabel title = new JLabel("Welcome To Wonderland");
        Font aliceFont = loadCustomFont();
        title.setFont(aliceFont);
        title.setForeground(Color.BLACK);
        title.setSize(1150, 100);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setLocation(200, 150);
        return title;
    }

    private Font loadCustomFont() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, new File("src/AW-Font.ttf"));
            return font.deriveFont(140f);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            return new Font("Serif", Font.PLAIN, 100);
        }
    }

    private JButton createStartButton() {
        JButton startButton = new JButton("Start");
        startButton.setSize(400, 125);
        startButton.setLocation(550, 350);
        startButton.setFont(loadCustomFont().deriveFont(60f));
        startButton.setBackground(Color.CYAN);
        startButton.setForeground(Color.DARK_GRAY);
        
        startButton.addActionListener(e -> connectToServer());
        return startButton;
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // Important: flush the header
            in = new ObjectInputStream(socket.getInputStream());

            playerId = in.readInt(); // Changed from readObject to readInt
            System.out.println("Connected as Player " + playerId);
            
            connected = true;
            cardLayout.show(cards, "GAME");
            field.requestFocusInWindow();

            // Start server listener in a separate thread
            new Thread(new ServerListener()).start();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to connect to server: " + e.getMessage());
        }
    }

    private void initializeField() {
        field = new GamePanel();
        field.setFocusable(true);
        
        field.addKeyListener(new GameKeyListener());
    }

    private void disconnect() {
        isRunning = false;
        connected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ServerListener implements Runnable {
        @Override
        public void run() {
            while (isRunning && connected) {
                try {
                    Object received = in.readObject();
                    if (received instanceof GameState) {
                        GameState gameState = (GameState) received;
                        charactersOnField = new CopyOnWriteArrayList<>(gameState.charactersOnField);
                        
                        // Update animation counter
                        counter++;
                        if (counter >= (FPS / 2)) {
                            spriteNum = (spriteNum == 1) ? 2 : 1;
                            counter = 0;
                        }

                        SwingUtilities.invokeLater(field::repaint);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    if (connected) {
                        e.printStackTrace();
                        connected = false;
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(client.this, 
                                "Lost connection to server: " + e.getMessage());
                            cardLayout.show(cards, "MENU");
                        });
                        break;
                    }
                }
            }
        }
    }

    private class GameKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (!connected) return;

            try {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    ArrayList<Character> newChars = new ArrayList<>();
                    newChars.add(new Character("Alice"));
                    PlayerAction action = new PlayerAction(playerId);
                    action.charactersOnField = newChars;
                    
                    out.reset(); // Reset the stream to prevent caching
                    out.writeObject(action);
                    out.flush();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                connected = false;
            }
        }
    }

    private class GamePanel extends JPanel {
        private final Image backgroundImage;

        public GamePanel() {
            try {
                backgroundImage = ImageIO.read(new File("src/Background.png"));
            } catch (IOException e) {
                throw new RuntimeException("Failed to load background image", e);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            // Draw background
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }

            // Draw characters
            for (Character character : charactersOnField) {
                try {
                    BufferedImage sprite;
                    String imagePath = character.direction == 1 ?
                        "src/Alice-Left-" + spriteNum + ".png" :
                        "src/Alice-Right-" + spriteNum + ".png";
                    
                    sprite = ImageIO.read(new File(imagePath));
                    g.drawImage(sprite, 
                              (int)character.x, 
                              (int)character.y, 
                              character.size * character.scale, 
                              character.size * character.scale, 
                              null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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