import java.io.Serializable;
import java.util.ArrayList;

public class PlayerAction implements Serializable{
    
    int playerId;
    ArrayList<Character> charactersOnField;

    PlayerAction(int ID){
        playerId = ID;
        charactersOnField = new ArrayList<>();
    }
}
