import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlayerAction implements Serializable {
    private static final long serialVersionUID = 1L;
    int playerId;
    List<Character> charactersOnField;
    
    public PlayerAction(int playerId) {
        this.playerId = playerId;
        this.charactersOnField = new ArrayList<>();
    }
}