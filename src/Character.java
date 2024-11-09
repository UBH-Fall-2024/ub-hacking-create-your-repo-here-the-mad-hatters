import java.awt.image.BufferedImage;

public class Character{

    public int x, y;
    public int movement_speed;
    public int health;
    public int damage;
    public String Name;

    public String direction;
    public BufferedImage left, right;

    public Character(){
        this.x=0;
        this.y=0;
        this.movement_speed=0;
        this.health=100;
        this.damage= 0;
        this.Name="";
        this.direction ="R";
        this.left = null;
        this.right = null;

    }
    
}

