import java.io.Serializable;
import java.util.UUID;

public class Character implements Serializable {
    private static final long serialVersionUID = 1L;
    String Name;
    String type;
    double x;
    double y;
    int direction; // 1 for left, 2 for right
    int size = 64;
    int scale = 2;
    int maxHealth;
    double currentHealth;
    int damage;
    int speed;
    int attackRange;
    boolean isRanged;
    boolean isInCombat;
    int towerDamageScale;
    public String id;
    
    // Attack speed related fields
    private int attackSpeed; // Frames between attacks
    private int attackCooldown = 0; // Current cooldown counter
    
    public Character(String type, int playerId) {
        this.type = type;
        this.id = UUID.randomUUID().toString();
        
        if (playerId == 1) {
            switch (type) {
                case "CHAR1": // Alice
                    Name = "Alice";
                    maxHealth = 100;
                    damage = 20;
                    speed = 4;
                    attackRange = 50;
                    isRanged = false;
                    towerDamageScale = 1;
                    attackSpeed = 60; // Attack once per second at 60 FPS
                    break;
                case "CHAR2": // Mad Hatter (ranged)
                    Name = "Mad Hatter";
                    maxHealth = 80;
                    damage = 1;
                    speed = 2;
                    attackRange = 300;
                    isRanged = true;
                    towerDamageScale = 30;
                    attackSpeed = 60;
                    break;
                case "CHAR3": // Tweedles
                    Name = "Tweedle Twins";
                    maxHealth = 150;
                    damage = 40;
                    speed = 2;
                    attackRange = 50;
                    isRanged = false;
                    towerDamageScale = 2;
                    attackSpeed = 60;
                    break;
                case "CHAR4": // Bandersnatch
                    Name = "Bandersnatch";
                    maxHealth = 300;
                    damage = 10;
                    speed = 1;
                    attackRange = 50;
                    isRanged = false;
                    towerDamageScale = 10;
                    attackSpeed = 60;
                    break;
            }
        } else {
            switch (type) {
                case "CHAR1": // Guard
                    Name = "Guard";
                    maxHealth = 100;
                    damage = 20;
                    speed = 4;
                    attackRange = 50;
                    isRanged = false;
                    towerDamageScale = 1;
                    attackSpeed = 60;
                    break;
                case "CHAR2": // Jabberwocky (ranged)
                    Name = "Jabberwocky";
                    maxHealth = 80;
                    damage = 1;
                    speed = 2;
                    attackRange = 300;
                    isRanged = true;
                    towerDamageScale = 30;
                    attackSpeed = 60;
                    scale = 3;
                    break;
                case "CHAR3": // Queen
                    Name = "Red Queen";
                    maxHealth = 150;
                    damage = 40;
                    speed = 2;
                    attackRange = 50;
                    isRanged = false;
                    towerDamageScale = 2;
                    attackSpeed = 60;
                    break;
                case "CHAR4": // Knave
                    Name = "Knave";
                    maxHealth = 300;
                    damage = 10;
                    speed = 1;
                    attackRange = 50;
                    isRanged = false;
                    towerDamageScale = 10;
                    attackSpeed = 60;
                    break;
            }
        }
        currentHealth = maxHealth;
    }
    
    public void attack(Character target) {
        // Only attack if cooldown is 0
        if (attackCooldown <= 0 && currentHealth > 0 && target.currentHealth > 0) {
            target.currentHealth -= damage;
            if (target.currentHealth < 0) target.currentHealth = 0;
            // Reset cooldown after attack
            attackCooldown = attackSpeed;
        }
    }
    
    public void updateCooldowns() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }
    
    public boolean canAttack() {
        return attackCooldown <= 0;
    }
    
    public boolean isDead() {
        return currentHealth <= 0;
    }
    
    public void move() {
        if (!isInCombat) {
            if (direction == 1) {
                x -= speed;
            } else {
                x += speed;
            }
        }
    }
}