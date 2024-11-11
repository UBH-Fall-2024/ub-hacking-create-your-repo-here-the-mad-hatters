import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlayerAction implements Serializable {
    private static final long serialVersionUID = 1L;
    public int playerId;
    public String type; // "SPAWN" or "COMBAT_UPDATE"
    public ArrayList<Character> charactersOnField;
    
    public PlayerAction(int playerId) {
        this.playerId = playerId;
        this.type = "SPAWN"; // Default type
    }
}