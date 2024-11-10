import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.*;

public class Character implements Serializable {
    private static final long serialVersionUID = 1L;
    String Name;
    double x, y;
    int direction;  // 1 for left, 2 for right
    int size = 32;
    int scale = 3;
    
    // Combat stats
    int maxHealth;
    int currentHealth;
    int damage;
    int attackSpeed = 500; // milliseconds between attacks
    long lastAttackTime = 0;
    boolean isAttacking = false;
    Character currentTarget = null;
    
    public Character(String name) {
        this.Name = name;
        initializeStats(name);
    }
    
    private void initializeStats(String name) {
        switch(name) {
            case "Alice":
                maxHealth = 100;
                damage = 20;
                break;
            case "Dragon":
                maxHealth = 200;
                damage = 35;
                break;
            case "Character3":  // Add your third character stats
                maxHealth = 150;
                damage = 25;
                break;
            case "Character4":  // Add your fourth character stats
                maxHealth = 175;
                damage = 30;
                break;
            default:
                maxHealth = 100;
                damage = 20;
        }
        currentHealth = maxHealth;
    }
    
    public boolean attack(Character target) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime >= attackSpeed) {
            target.takeDamage(damage);
            lastAttackTime = currentTime;
            return true;
        }
        return false;
    }
    
    public void takeDamage(int amount) {
        currentHealth -= amount;
    }
    
    public boolean isDead() {
        return currentHealth <= 0;
    }
}