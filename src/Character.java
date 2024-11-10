import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import javax.imageio.*;

public class Character implements Serializable {
    private static final long serialVersionUID = 1L;
    public String Name;
    public double x;
    public double y;
    public int direction;
    public int size = 64;
    public int scale = 2;
    public int maxHealth;
    public int currentHealth;
    public int damage;
    public boolean isInCombat;
    public String id; // Add unique identifier
    
    public Character(String name) {
        this.Name = name;
        this.id = UUID.randomUUID().toString(); // Generate unique ID
        
        // Set stats based on character type
        switch(name) {
            case "Alice":
                this.maxHealth = 100;
                this.damage = 10;
                break;
            case "Dragon":
                this.maxHealth = 200;
                this.damage = 20;
                break;
            default:
                this.maxHealth = 100;
                this.damage = 10;
        }
        this.currentHealth = this.maxHealth;
    }
    
    public void attack(Character target) {
        if (!isDead() && !target.isDead()) {
            target.currentHealth -= this.damage;
        }
    }
    
    public boolean isDead() {
        return currentHealth <= 0;
    }
}