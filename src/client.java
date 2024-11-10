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
    private JPanel gameField; // Store reference to game field
    private boolean gameFieldInitialized = false; // Track initialization state

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
        
        // Only initialize menu at startup
        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        
        initializeMenu();
        
        // Create empty game field panel - will be initialized after connection
        gameField = new JPanel();
        
        cards.add(menu, "MENU");
        cards.add(gameField, "GAME");
        
        add(cards);
        cardLayout.show(cards, "MENU");
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
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
            
            // Initialize towers after player ID is known
            leftTower = new Tower(1);
            rightTower = new Tower(2);
            
            connected = true;
            
            // Initialize game field now that we have playerId
            if (!gameFieldInitialized) {
                field = new GamePanel();
                field.setFocusable(true);
                field.setLayout(null);
                
                // Create spawn buttons with correct character names based on known playerId
                if (playerId == 1) {
                    createSpawnButton("CHAR1", "Alice", 300, 700);
                    createSpawnButton("CHAR2", "Dragon", 525, 700);
                    createSpawnButton("CHAR3", "White Rabbit", 750, 700);
                    createSpawnButton("CHAR4", "Cheshire Cat", 975, 700);
                } else {
                    createSpawnButton("CHAR1", "Mad Hatter", 300, 700);
                    createSpawnButton("CHAR2", "Jabberwocky", 525, 700);
                    createSpawnButton("CHAR3", "Red Queen", 750, 700);
                    createSpawnButton("CHAR4", "Knave", 975, 700);
                }
                
                // Replace the empty panel with initialized game field
                cards.remove(gameField);
                cards.add(field, "GAME");
                gameFieldInitialized = true;
            }
            
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
        
        // Define character names for each player
        String[] player1Characters = {"Alice", "Dragon", "White Rabbit", "Cheshire Cat"};
        String[] player2Characters = {"Mad Hatter", "Jabberwocky", "Red Queen", "Knave"};
        
        // Use the appropriate character names based on player ID
        String[] characterNames = ((playerId == 1) ? player1Characters : player2Characters);
        
        // Create spawn buttons with correct character names
        createSpawnButton("CHAR1", characterNames[0], 300, 700);
        createSpawnButton("CHAR2", characterNames[1], 525, 700);
        createSpawnButton("CHAR3", characterNames[2], 750, 700);
        createSpawnButton("CHAR4", characterNames[3], 975, 700);
    }

    private JButton createSpawnButton(String charType, String displayName, int x, int y) {
        JButton button = new JButton("Spawn " + displayName + " (Ready)");
        button.setSize(200, 50);
        button.setLocation(x, y);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(Color.PINK);
        button.setForeground(Color.BLACK);
        button.setVisible(true);
        
        // Store character name for use in cooldown text
        button.putClientProperty("characterName", displayName);
        
        button.addActionListener(e -> handleSpawnButtonClick(charType));
        field.add(button);
        
        switch(charType) {
            case "CHAR1": spawnButton = button; break;
            case "CHAR2": spawnButton2 = button; break;
            case "CHAR3": spawnButton3 = button; break;
            case "CHAR4": spawnButton4 = button; break;
        }
        
        return button;
    }

    private void handleSpawnButtonClick(String charType) {
        if (!connected) return;
    
        long currentTime = System.currentTimeMillis();
        long timeSinceLastSpawn;
        long cooldownTime;
    
        // Get appropriate cooldown time and button
        JButton buttonToUpdate = null;
        switch(charType) {
            case "CHAR1": 
                timeSinceLastSpawn = currentTime - lastAliceSpawnTime;
                cooldownTime = ALICE_SPAWN_COOLDOWN;
                buttonToUpdate = spawnButton;
                break;
            case "CHAR2":
                timeSinceLastSpawn = currentTime - lastDragonSpawnTime;
                cooldownTime = DRAGON_SPAWN_COOLDOWN;
                buttonToUpdate = spawnButton2;
                break;
            case "CHAR3":
                timeSinceLastSpawn = currentTime - lastChar3SpawnTime;
                cooldownTime = CHARACTER3_SPAWN_COOLDOWN;
                buttonToUpdate = spawnButton3;
                break;
            case "CHAR4":
                timeSinceLastSpawn = currentTime - lastChar4SpawnTime;
                cooldownTime = CHARACTER4_SPAWN_COOLDOWN;
                buttonToUpdate = spawnButton4;
                break;
            default:
                return;
        }
    
        if (timeSinceLastSpawn >= cooldownTime && buttonToUpdate != null) {
            try {
                Character newCharacter = new Character(charType, playerId);
                
                // Set spawn position and direction based on player ID
                if (playerId == 1) {
                    newCharacter.x = 100;
                    newCharacter.direction = 2;
                } else {
                    newCharacter.x = 1275;
                    newCharacter.direction = 1;
                }
                newCharacter.y = 500;
                
                ArrayList<Character> newChars = new ArrayList<>();
                newChars.add(newCharacter);
                
                PlayerAction action = new PlayerAction(playerId);
                action.charactersOnField = newChars;
                
                out.reset();
                out.writeObject(action);
                out.flush();
    
                // Get the stored character name from the button
                String displayName = (String) buttonToUpdate.getClientProperty("characterName");
                
                buttonToUpdate.setEnabled(false);
                buttonToUpdate.setText("Spawn " + displayName + " (Cooldown)");
                
                final JButton finalButton = buttonToUpdate;
                Timer cooldownTimer = new Timer((int)cooldownTime, evt -> {
                    finalButton.setEnabled(true);
                    finalButton.setText("Spawn " + displayName + " (Ready)");
                });
                cooldownTimer.setRepeats(false);
                cooldownTimer.start();
    
                // Update the appropriate cooldown timer
                switch(charType) {
                    case "CHAR1": lastAliceSpawnTime = currentTime; break;
                    case "CHAR2": lastDragonSpawnTime = currentTime; break;
                    case "CHAR3": lastChar3SpawnTime = currentTime; break;
                    case "CHAR4": lastChar4SpawnTime = currentTime; break;
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

            // Draw ranged attack indicators
            for (Character character : charactersOnField) {
                if (character.isRanged && character.isInCombat) {
                    g.setColor(new Color(255, 0, 0, 100)); // Semi-transparent red
                    // Draw an attack line from the character to their target
                    for (Character target : charactersOnField) {
                        if (target != character && character.direction != target.direction) {
                            double distance = calculateDistance(character, target);
                            if (distance <= character.attackRange) {
                                g.drawLine(
                                    (int)(character.x + character.size * character.scale / 2),
                                    (int)(character.y + character.size * character.scale / 2),
                                    (int)(target.x + target.size * target.scale / 2),
                                    (int)(target.y + target.size * target.scale / 2)
                                );
                            }
                        }
                    }
                }
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
                if(sprite != null){
                    g.drawImage(sprite, 
                          (int)character.x, 
                          (int)character.y, 
                          character.size * character.scale, 
                          character.size * character.scale, 
                          null);
                }
                else{
                    g.fillRect((int)character.x, (int)character.y, 
                    character.size * character.scale, character.size * character.scale);
                }

                
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
            try {
                // Check for tower collision
                Rectangle charBounds = new Rectangle(
                    (int)character.x,
                    (int)character.y,
                    character.size * character.scale,
                    character.size * character.scale
                );
                
                // Tower collision (even ranged units need to be in normal range to hit tower)
                Tower targetTower = character.direction == 1 ? leftTower : rightTower;
                Rectangle towerBounds = new Rectangle(
                    targetTower.x,
                    targetTower.y,
                    targetTower.width,
                    targetTower.height
                );
                
                // If colliding with tower, stop moving and attack
                if (charBounds.intersects(towerBounds)) {
                    character.isInCombat = true;
                    targetTower.takeDamage(character.damage);
                    character.currentHealth = 0; // Units die when hitting tower
                    
                    // Send combat update to server
                    sendCombatUpdate();
                    return;
                }
                
                // Character combat
                boolean inCombat = false;
                for (Character otherChar : charactersOnField) {
                    if (otherChar != character && character.direction != otherChar.direction) {
                        double distance = calculateDistance(character, otherChar);
                        
                        // Check if characters are within their respective attack ranges
                        boolean canAttack = character.isRanged ? 
                            distance <= character.attackRange : 
                            charBounds.intersects(new Rectangle(
                                (int)otherChar.x,
                                (int)otherChar.y,
                                otherChar.size * otherChar.scale,
                                otherChar.size * otherChar.scale
                            ));
                        
                        if (canAttack) {
                            // Ranged units stop at their attack range
                            character.isInCombat = true;
                            character.attack(otherChar);
                            inCombat = true;
                            
                            // If the other character can attack back (either ranged or in melee range)
                            if (otherChar.isRanged && distance <= otherChar.attackRange || 
                                !otherChar.isRanged && charBounds.intersects(new Rectangle(
                                    (int)otherChar.x,
                                    (int)otherChar.y,
                                    otherChar.size * otherChar.scale,
                                    otherChar.size * otherChar.scale
                                ))) {
                                otherChar.isInCombat = true;
                                otherChar.attack(character);
                            }
                            
                            if(otherChar.isDead() || character.isDead()){
                                inCombat = false;
                                character.isInCombat = false;
                                otherChar.isInCombat = false;
                            }
                            
                            // Send combat update to server
                            sendCombatUpdate();
                        }
                    }
                }
                
                if (!inCombat) {
                    character.isInCombat = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                connected = false;
            }
        } 

        // Helper method to calculate distance between two characters
        private double calculateDistance(Character char1, Character char2) {
            double dx = char1.x - char2.x;
            double dy = char1.y - char2.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        // Helper method to send combat updates to server
        private void sendCombatUpdate() throws IOException {
            PlayerAction action = new PlayerAction(playerId);
            action.type = "COMBAT_UPDATE";
            action.charactersOnField = new ArrayList<>(charactersOnField);
            out.reset();
            out.writeObject(action);
            out.flush();
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