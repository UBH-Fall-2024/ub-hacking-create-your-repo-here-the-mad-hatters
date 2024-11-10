import javax.swing.WindowConstants;

public class main {
    public static void main(String[] args) {
        client clientFrame = new client();
        clientFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        clientFrame.setSize(1920,1080);
        clientFrame.setTitle("Warriors Of Wonderland");
        clientFrame.setVisible(true);
    }
}
