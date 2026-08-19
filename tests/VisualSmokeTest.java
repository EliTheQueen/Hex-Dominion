import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import controller.GameController;
import model.HexCoordinate;
import view.MapPanel;

/** Headless render check catches paint-time regressions in procedural map art. */
public final class VisualSmokeTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        GameController controller = new GameController(); controller.startNewGame();
        HexCoordinate center = controller.getGameState().getTownHallPos();
        controller.getGameState().getMap().buildRoad(center);
        HexCoordinate neighbour = center.findNeighbours().get(0);
        controller.getGameState().getMap().buildRoad(neighbour);
        controller.getGameState().getMap().buildWall(center, neighbour);
        MapPanel panel = new MapPanel(controller, null); panel.setSize(1000, 680);
        BufferedImage image = new BufferedImage(1000, 680, BufferedImage.TYPE_INT_ARGB);
        panel.paint(image.createGraphics());
        ImageIO.write(image, "png", new File(System.getProperty("java.io.tmpdir"), "hex-dominion-visual-smoke.png"));
        require(image.getRGB(500, 340) != 0, "map render produced pixels");
        System.out.println("VisualSmokeTest passed");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
