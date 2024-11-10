import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.*;

public class Character implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public String Name;
    public double x = 100;  // Default starting position
    public double y = 500;  // Default starting position
    public int direction = 2; // Default direction (right)
    public int size = 100;    // Default size
    public int scale = 1;     // Default scale
    public double movement_speed = 5; // Default speed
    
    public Character(String name) {
        this.Name = name;
    }
}