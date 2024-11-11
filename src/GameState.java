import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    List<Character> charactersOnField;
    
    public GameState(List<Character> characters) {
        this.charactersOnField = new ArrayList<>(characters);
    }
}
