import java.util.ArrayList;

public class PlayerAction {
    
    int playerId;
    ArrayList<Character> charactersOnField;

    PlayerAction(int ID){
        playerId = ID;
        charactersOnField = new ArrayList<>();
    }
}
