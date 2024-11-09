import java.io.Serializable;
import java.util.ArrayList;

public class GameState implements Serializable{
    
    public ArrayList<Character> charactersOnField;

    GameState(){
        charactersOnField = new ArrayList<>();
    }
    GameState(ArrayList<Character> COF){
        charactersOnField = COF;
    }
    @Override
    public String toString() {
        return "GameState{charactersOnField=" + charactersOnField + "}";
    }
}
