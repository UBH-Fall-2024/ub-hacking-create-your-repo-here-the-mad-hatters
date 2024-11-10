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

    // Towers
    private Tower leftTower;   // Tower on the left side (Player 1's tower)
    private Tower rightTower;  // Tower on the right side (Player 2's tower)
    private BufferedImage towerImage;

    public client() {
        setTitle("Wonderland Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Load tower image
       // try {
            //towerImage = ImageIO.read(new File("src/tower.png"));  // Load tower image
            towerImage = null;
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
        
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
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            playerId = in.readInt();
            System.out.println("Connected as Player " + playerId);
            
            // Initialize both towers with consistent positions
            leftTower = new Tower(1);   // Player 1's tower always on left
            rightTower = new Tower(2);  // Player 2's tower always on right
            
            connected = true;
            cardLayout.show(cards, "GAME");
            field.requestFocusInWindow();

            new Thread(new ServerListener()).start();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to connect to server: " + e.getMessage());
        }
    }

    // Separate cooldown timers for each character type
    private long lastAliceSpawnTime = 0;
    private long lastDragonSpawnTime = 0;
    private static final long ALICE_SPAWN_COOLDOWN = 2000; // 2 seconds cooldown
    private static final long DRAGON_SPAWN_COOLDOWN = 2000; // 2 seconds cooldown
    private JButton spawnButton, spawnButton2;
    // Characters 3 and 4 variables
    private JButton spawnButton3, spawnButton4;
    private static final long CHARACTER3_SPAWN_COOLDOWN = 2000;
    private static final long CHARACTER4_SPAWN_COOLDOWN = 2000;
    private long lastChar3SpawnTime = 0;
    private long lastChar4SpawnTime = 0;

    private void initializeField() {
        field = new GamePanel();
        field.setFocusable(true);
        field.setLayout(null);
        
        // Create all spawn buttons
        createSpawnButton("Alice", 300, 700);
        createSpawnButton("Dragon", 525, 700);
        createSpawnButton("Character3", 750, 700);
        createSpawnButton("Character4", 975, 700);
        
        field.addKeyListener(new GameKeyListener());
    }

    private JButton createSpawnButton(String characterName, int x, int y) {
        JButton button = new JButton("Spawn " + characterName + " (Ready)");
        button.setSize(200, 50);
        button.setLocation(x, y);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(Color.PINK);
        button.setForeground(Color.BLACK);
        button.setVisible(true);
        button.addActionListener(e -> handleSpawnButtonClick(characterName));
        field.add(button);
        
        switch(characterName) {
            case "Alice": spawnButton = button; break;
            case "Dragon": spawnButton2 = button; break;
            case "Character3": spawnButton3 = button; break;
            case "Character4": spawnButton4 = button; break;
        }
        
        return button;
    }

    private void handleSpawnButtonClick(String name) {
        if (!connected) return;

        long currentTime = System.currentTimeMillis();
        long timeSinceLastSpawn;
        long cooldownTime;

        // Determine which cooldown to use
        if (name.equals("Alice")) {
            timeSinceLastSpawn = currentTime - lastAliceSpawnTime;
            cooldownTime = ALICE_SPAWN_COOLDOWN;
        } else {
            timeSinceLastSpawn = currentTime - lastDragonSpawnTime;
            cooldownTime = DRAGON_SPAWN_COOLDOWN;
        }

        if (timeSinceLastSpawn >= cooldownTime) {
            try {
                Character newCharacter = new Character(name);
                
                // Set spawn position and direction based on player ID
                System.out.println("ID: " + playerId);
                if (playerId == 1) {
                    newCharacter.x = 100;  // Player 1 spawns on left
                    newCharacter.direction = 2;  // Moving right
                } else {
                    newCharacter.x = 1275;  // Player 2 spawns on right
                    newCharacter.direction = 1;  // Moving left
                }
                newCharacter.y = 500;  // Vertical position remains the same
                
                ArrayList<Character> newChars = new ArrayList<>();
                newChars.add(newCharacter);
                
                PlayerAction action = new PlayerAction(playerId);
                action.charactersOnField = newChars;
                
                System.out.println("Spawning new " + name);
                out.reset();
                out.writeObject(action);
                out.flush();

                // Update the appropriate cooldown timer
                if (name.equals("Alice")) {
                    lastAliceSpawnTime = currentTime;
                    spawnButton.setEnabled(false);
                    spawnButton.setText("Spawn Alice (Cooldown)");
                    Timer cooldownTimer = new Timer((int)ALICE_SPAWN_COOLDOWN, evt -> {
                        spawnButton.setEnabled(true);
                        spawnButton.setText("Spawn Alice (Ready)");
                    });
                    cooldownTimer.setRepeats(false);
                    cooldownTimer.start();
                } else {
                    lastDragonSpawnTime = currentTime;
                    spawnButton2.setEnabled(false);
                    spawnButton2.setText("Spawn Dragon (Cooldown)");
                    Timer cooldownTimer = new Timer((int)DRAGON_SPAWN_COOLDOWN, evt -> {
                        spawnButton2.setEnabled(true);
                        spawnButton2.setText("Spawn Dragon (Ready)");
                    });
                    cooldownTimer.setRepeats(false);
                    cooldownTimer.start();
                }
                
            } catch (IOException ex) {
                ex.printStackTrace();
                connected = false;
            }
        }
        
        field.requestFocusInWindow();
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
                //towerImage = ImageIO.read(new File("src/tower.png"));
                towerImage = null;
            } catch (IOException e) {
                throw new RuntimeException("Failed to load images", e);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            // Draw background
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }

            // Draw towers
            drawTower(g, leftTower);
            drawTower(g, rightTower);

            // Draw characters and their health bars
            for (Character character : charactersOnField) {
                drawCharacter(g, character);
                handleCharacterCombat(character);
            }
            
            // Clean up dead characters
            charactersOnField.removeIf(Character::isDead);
        }

        private void drawCharacter(Graphics g, Character character) {
            try {
                // Draw character sprite
                BufferedImage sprite;
                String imagePath = character.direction == 1 ?
                    "src/" + character.Name + "-Left-" + spriteNum + ".png" :
                    "src/" + character.Name + "-Right-" + spriteNum + ".png";
                
                sprite = ImageIO.read(new File(imagePath));
                g.drawImage(sprite, 
                          (int)character.x, 
                          (int)character.y, 
                          character.size * character.scale, 
                          character.size * character.scale, 
                          null);
                
                // Draw health bar above character
                int healthBarWidth = 60;
                int healthBarHeight = 8;
                int healthBarX = (int)character.x + (character.size * character.scale - healthBarWidth) / 2;
                int healthBarY = (int)character.y - 15;
                
                // Background
                g.setColor(Color.RED);
                g.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
                
                // Current health
                g.setColor(Color.GREEN);
                int currentHealthWidth = (int)((character.currentHealth / (float)character.maxHealth) * healthBarWidth);
                g.fillRect(healthBarX, healthBarY, currentHealthWidth, healthBarHeight);
                
                // Border
                g.setColor(Color.BLACK);
                g.drawRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleCharacterCombat(Character character) {
            // Check for tower collision
            Rectangle charBounds = new Rectangle(
                (int)character.x,
                (int)character.y,
                character.size * character.scale,
                character.size * character.scale
            );
            
            // Tower collision
            Tower targetTower = character.direction == 1 ? leftTower : rightTower;
            Rectangle towerBounds = new Rectangle(
                targetTower.x,
                targetTower.y,
                targetTower.width,
                targetTower.height
            );
            
            if (charBounds.intersects(towerBounds)) {
                targetTower.takeDamage(character.damage);
                character.currentHealth = 0; // Die on impact
                return;
            }
            
            // Character combat
            for (Character otherChar : charactersOnField) {
                if (otherChar != character && character.direction != otherChar.direction) {
                    Rectangle otherBounds = new Rectangle(
                        (int)otherChar.x,
                        (int)otherChar.y,
                        otherChar.size * otherChar.scale,
                        otherChar.size * otherChar.scale
                    );
                    
                    if (charBounds.intersects(otherBounds)) {
                        // Both characters attack each other
                        character.attack(otherChar);
                        otherChar.attack(character);
                    }
                }
            }
        }

        private void drawTower(Graphics g, Tower tower) {
            // Draw tower base
            if (towerImage != null) {
                g.drawImage(towerImage, tower.x, tower.y, tower.width, tower.height, null);
            } else {
                // Fallback rectangle if image fails to load
                g.setColor(tower.playerId == 1 ? new Color(0, 0, 255, 180) : new Color(255, 0, 0, 180));
                g.fillRect(tower.x, tower.y, tower.width, tower.height);
            }

            // Draw health bar
            int healthBarWidth = 150;  // Made wider for better visibility
            int healthBarHeight = 25;
            int healthBarX = tower.x + (tower.width - healthBarWidth) / 2;
            int healthBarY = tower.y - 40;  // Moved up slightly

            // Health bar background
            g.setColor(new Color(0, 0, 0, 100));  // Semi-transparent black
            g.fillRect(healthBarX - 2, healthBarY - 2, healthBarWidth + 4, healthBarHeight + 4);

            // Empty health bar
            g.setColor(new Color(80, 80, 80));
            g.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

            // Current health
            float healthPercentage = tower.currentHealth / (float)tower.maxHealth;
            Color healthColor = new Color(
                (int)(255 * (1 - healthPercentage)),  // Red component
                (int)(255 * healthPercentage),        // Green component
                0                                     // Blue component
            );
            g.setColor(healthColor);
            int currentHealthWidth = (int)(healthPercentage * healthBarWidth);
            g.fillRect(healthBarX, healthBarY, currentHealthWidth, healthBarHeight);

            // Health bar border
            g.setColor(Color.WHITE);
            g.drawRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

            // Health text
            String healthText = tower.currentHealth + "/" + tower.maxHealth;
            g.setFont(new Font("Arial", Font.BOLD, 16));
            FontMetrics fm = g.getFontMetrics();
            int textX = healthBarX + (healthBarWidth - fm.stringWidth(healthText)) / 2;
            int textY = healthBarY + ((healthBarHeight + fm.getAscent()) / 2) - 2;
            
            // Text outline for better visibility
            g.setColor(Color.BLACK);
            g.drawString(healthText, textX - 1, textY - 1);
            g.drawString(healthText, textX - 1, textY + 1);
            g.drawString(healthText, textX + 1, textY - 1);
            g.drawString(healthText, textX + 1, textY + 1);
            
            // Text
            g.setColor(Color.WHITE);
            g.drawString(healthText, textX, textY);
            
            // Draw tower owner label
            String ownerText = "Player " + tower.playerId;
            textX = tower.x + (tower.width - fm.stringWidth(ownerText)) / 2;
            textY = tower.y - 60;
            g.setColor(tower.playerId == 1 ? Color.BLUE : Color.RED);
            g.drawString(ownerText, textX, textY);
        }

        private void checkTowerCollisions(Character character) {
            Rectangle characterBounds = new Rectangle(
                (int)character.x,
                (int)character.y,
                character.size * character.scale,
                character.size * character.scale
            );

            // Determine which tower to check based on character's direction
            Tower targetTower = character.direction == 1 ? leftTower : rightTower;
            Rectangle towerBounds = new Rectangle(
                targetTower.x,
                targetTower.y,
                targetTower.width,
                targetTower.height
            );

            if (characterBounds.intersects(towerBounds)) {
                targetTower.takeDamage(1);
                charactersOnField.remove(character);
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

class Tower implements Serializable {
    private static final long serialVersionUID = 1L;
    int x;
    int y;
    int width = 150;
    int height = 300;
    int maxHealth = 1000;
    int currentHealth;
    int playerId;  // 1 for left tower, 2 for right tower

    public Tower(int playerId) {
        this.playerId = playerId;
        this.currentHealth = maxHealth;
        
        // Consistent positions for all players
        if (playerId == 1) {
            x = 50;  // Left tower always at left side
        } else {
            x = 1325;  // Right tower always at right side
        }
        y = 400;
    }

    public void takeDamage(int damage) {
        currentHealth -= damage;
        if (currentHealth < 0) currentHealth = 0;
    }

    public boolean isDestroyed() {
        return currentHealth <= 0;
    }
}