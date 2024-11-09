import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.*;

public class Character{

    public int x, y;
    public int movement_speed;
    public int health;
    public int damage;
    public String Name;
    public int direction;//1 = left, 2 = right
    public BufferedImage left1, left2, right1, right2;
    public int size;
    public int scale;

    public Character(){
        this.x=200;
        this.y=200;
        this.movement_speed=0;
        this.health=100;
        this.damage= 0;
        this.Name="";
        this.direction =2;
        this.left1=null;
        this.left2=null;
        this.right1=null;
        this.right2=null;
        this.size=20;
        this.scale=2;
    }

    public Character(String N){
        this.x=200;
        this.y=200;
        this.movement_speed=0;
        this.health=100;
        this.damage= 0;
        this.Name=N;
        this.direction =2;
        System.out.println("NIGGA");
        try{
            if(N.equals("Alice")){
                this.left1 = ImageIO.read(new File("src/Alice-Left-1.png"));
                this.left2 = ImageIO.read(new File("src/Alice-Left-2.png"));
                this.right1 = ImageIO.read(new File("src/Alice-Right-1.png"));
                this.right2 = ImageIO.read(new File("src/Alice-Right-2.png"));
                this.size = 20;
                this.scale = 3;
                System.out.println("SHIT WORKS");
            }
            //if(N.equals("CHARACTER_NAME")){}
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
    
}

