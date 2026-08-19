import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import app.Main;
import controller.GameController;
import model.Building;
import model.Builder;
import model.Constants;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.Unit;
import model.season.Season;
import view.GamePanel;
import view.MainWindow;
import view.RecruitPanel;
import view.TechPanel;
import view.TribePanel;
import model.tribe.Tribe;

/**
 * Non-headless runtime verification for the real Swing entry point.
 *
 * <p>This deliberately invokes {@link Main#main(String[])}, waits for the real
 * JFrame, starts a game, exercises UI/controller paths, and observes uncaught
 * EDT/Timer failures. It is not a replacement for launching app.Main directly;
 * it makes the interaction checks repeatable after that launch check.</p>
 */
public final class RealSwingRuntimeTest {
    private static final long WINDOW_TIMEOUT_MS = 8_000L;

    private RealSwingRuntimeTest() {}

    public static void main(String[] args) throws Exception {
        require(!GraphicsEnvironment.isHeadless(), "runtime verification must not be headless");

        List<Throwable> uncaught = Collections.synchronizedList(new ArrayList<>());
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
            uncaught.add(failure);
            System.err.println("Uncaught failure on " + thread.getName());
            failure.printStackTrace(System.err);
            if (previous != null) previous.uncaughtException(thread, failure);
        });

        Main.main(new String[0]);
        MainWindow window = waitForMainWindow();

        AtomicReference<GamePanel> gamePanelRef = new AtomicReference<>();
        AtomicReference<GameState> stateRef = new AtomicReference<>();
        AtomicReference<Unit> movedUnitRef = new AtomicReference<>();
        AtomicReference<HexCoordinate> movedFromRef = new AtomicReference<>();
        AtomicReference<HexCoordinate> movedToRef = new AtomicReference<>();

        onEdt(() -> {
            require(window.isShowing(), "MainWindow is not showing");
            window.startGame();

            GamePanel gamePanel = findComponent(window, GamePanel.class);
            require(gamePanel != null && gamePanel.isShowing(), "GamePanel did not initialize");
            gamePanelRef.set(gamePanel);

            GameController controller = window.getController();
            GameState state = controller.getGameState();
            stateRef.set(state);
            verifyInitialState(state);
            verifyHudLayout(gamePanel);
            require(gamePanel.getMissionHudPanel() != null && gamePanel.getMissionHudPanel().isShowing(),
                    "dedicated mission HUD did not initialize");
            render(gamePanel.getMissionHudPanel());
            render(gamePanel);
            render(gamePanel.getMapPanel());
            writeSnapshot(gamePanel, new File("/tmp/hex-dominion-real-swing.png"));
            writeSeasonSnapshots(gamePanel, state);

            Movement movement = findMovement(state);
            require(movement != null, "no starting unit has a valid movement destination");
            controller.onHexClicked(movement.unit.getPosition());
            require(controller.getSelectedUnit() == movement.unit, "unit selection failed");
            gamePanel.repaintAll();
            window.validate();
            writeSnapshot(gamePanel, new File("/tmp/hex-dominion-selected-unit.png"));
            controller.onHexClicked(movement.destination);
            require(movement.destination.equals(movement.unit.getPosition()), "unit movement failed");
            movedUnitRef.set(movement.unit);
            movedFromRef.set(movement.origin);
            movedToRef.set(movement.destination);
            gamePanel.repaintAll();
            render(gamePanel.getMapPanel());

            Unit builder = null;
            for (Unit unit : state.getPlayer().getUnits()) {
                if (unit instanceof Builder) { builder = unit; break; }
            }
            require(builder != null, "starting Builder was not found");
            controller.deselectUnit();
            controller.onHexClicked(builder.getPosition());
            require(controller.getSelectedUnit() == builder, "Builder selection failed");
            HexCoordinate adjacentSite = null;
            for (HexCoordinate neighbour : builder.getPosition().findNeighbours()) {
                if (state.getMap().containsCoordinate(neighbour)
                        && state.getPlayer().getBuildingAt(neighbour) == null) {
                    adjacentSite = neighbour;
                    break;
                }
            }
            require(adjacentSite != null, "Builder has no adjacent demolition test site");
            Building adjacentFarm = new Building(adjacentSite, Constants.BuildingType.FARM);
            state.getPlayer().addBuilding(adjacentFarm);
            state.getMap().getHex(adjacentSite).setHasBuilding(true);
            state.getMap().buildWall(builder.getPosition(), adjacentSite);
            gamePanel.repaintAll();
            window.validate();
            require(findButtonStartingWith(gamePanel.getSidePanel(), "Demolish FARM") != null,
                    "adjacent building demolition control is missing");
            require(findButtonStartingWith(gamePanel.getSidePanel(), "Demolish wall") != null,
                    "wall-edge demolition control is missing");
            writeSnapshot(gamePanel, new File("/tmp/hex-dominion-selected-builder.png"));
            controller.deselectUnit();
            gamePanel.repaintAll();
        });

        openAndCloseDialog(gamePanelRef.get(), "RECRUIT", RecruitPanel.class);
        openAndCloseDialog(gamePanelRef.get(), "RESEARCH", TechPanel.class);
        openCampInteraction(gamePanelRef.get(), stateRef.get(), window.getController());

        int turnBefore = stateRef.get().getCurrentTurn();
        onEdt(() -> {
            JButton endTurn = findButton(gamePanelRef.get().getHudPanel(), "END TURN");
            require(endTurn != null && endTurn.isEnabled(), "END TURN button is unavailable");
            endTurn.doClick();
            require(stateRef.get().getCurrentTurn() == turnBefore + 1, "end turn did not advance the turn");
            render(gamePanelRef.get());
            render(gamePanelRef.get().getMapPanel());
            writeSnapshot(gamePanelRef.get(), new File("/tmp/hex-dominion-real-swing-after-turn.png"));
        });

        // Let animation, repaint, audio, and Swing Timer callbacks execute after the interactions.
        Thread.sleep(2_000L);
        onEdt(() -> render(gamePanelRef.get().getMapPanel()));

        require(uncaught.isEmpty(), "uncaught Swing/runtime exception(s): " + uncaught.size());
        GameState state = stateRef.get();
        System.out.println("REAL_SWING_RUNTIME_OK");
        System.out.println("frame=" + window.getTitle() + " showing=" + window.isShowing());
        System.out.println("turn=" + state.getCurrentTurn()
                + " season=" + state.getSeasonCycle().getCurrentSeason()
                + " happiness=" + state.getHappinessLevel());
        System.out.println("units=" + state.getPlayer().getUnitCount()
                + " buildings=" + state.getPlayer().getBuildingCount()
                + " tribes=" + state.getTribes().size());
        System.out.println("movement=" + movedUnitRef.get().getUnitType()
                + " " + coordinateText(movedFromRef.get()) + " -> " + coordinateText(movedToRef.get()));
        System.out.println("dialogs=RecruitPanel,TechPanel,TribePanel");
        System.out.println("uncaught=0");

        onEdt(() -> {
            for (Window openWindow : Window.getWindows()) openWindow.dispose();
        });
        System.exit(0);
    }

    private static void verifyInitialState(GameState state) {
        require(state != null, "GameState was not initialized");
        require(state.getMap() != null, "map was not initialized");
        require(state.getPlayer() != null, "player was not initialized");
        require(state.getSeasonCycle() != null && state.getSeasonCycle().getCurrentSeason() != null,
                "season was not initialized");
        require(state.getHappinessService() != null && state.getHappinessLevel() != null,
                "happiness was not initialized");
        require(state.getInfrastructureService() != null, "infrastructure service was not initialized");
        require(state.getTownHall() != null, "Phase 2 Town Hall was not initialized");
        require(state.getPhaseTwoTechnologies() != null, "Phase 2 technology registry was not initialized");
        require(state.getTribes() != null && !state.getTribes().isEmpty(), "tribes were not initialized");

        Player player = state.getPlayer();
        HexCoordinate townHallPosition = state.getTownHallPos();
        Hex townHallHex = state.getMap().getHex(townHallPosition);
        require(townHallHex != null, "Town Hall position is outside the map");
        require(townHallHex.getTerrainType() != Constants.TerrainType.SEA
                        && townHallHex.getTerrainType() != Constants.TerrainType.MOUNTAIN_RANGE,
                "Town Hall started on impassable terrain: " + townHallHex.getTerrainType());

        int townHallCount = 0;
        Building legacyTownHall = null;
        for (Building building : player.getBuildings()) {
            if (building.getType() == Constants.BuildingType.TOWN_HALL) {
                townHallCount++;
                legacyTownHall = building;
            }
        }
        require(townHallCount == 1, "expected exactly one legacy Town Hall, found " + townHallCount);
        require(legacyTownHall != null && townHallPosition.equals(legacyTownHall.getPosition()),
                "legacy Town Hall position is not synchronized");
        require(legacyTownHall.getCurrentHp() == state.getTownHall().getCurrentHp()
                        && legacyTownHall.getMaxHp() == state.getTownHall().getMaxHp(),
                "legacy and Phase 2 Town Hall health are not synchronized");

        require(player.getUnitCount() == 5, "expected five living starting units, found " + player.getUnitCount());
        for (Unit unit : player.getUnits()) {
            require(unit != null && unit.isAlive(), "starting unit is null or dead");
            require(state.getMap().containsCoordinate(unit.getPosition()), "starting unit is outside the map");
            require(unit.getCurrentHp() > 0 && unit.getCurrentHp() <= unit.getMaxHp(),
                    "starting unit has invalid HP");
            require(unit.getCurrentAP() > 0 && unit.getCurrentAP() <= unit.getMaxAP(),
                    "starting unit has invalid AP");
        }

        for (Constants.ResourceType type : Constants.ResourceType.values()) {
            int amount = player.getResources().get(type);
            int cap = player.getResources().getCap(type);
            require(amount >= 0 && amount <= cap, "invalid starting resource " + type + ": " + amount + "/" + cap);
        }

        state.getEligibleTradingPosts();
        for (model.tribe.Tribe tribe : state.getTribes()) state.getMission(tribe);
    }

    private static Movement findMovement(GameState state) {
        Player player = state.getPlayer();
        for (Unit unit : player.getUnits()) {
            for (HexCoordinate neighbour : unit.getPosition().findNeighbours()) {
                if (player.getUnitAt(neighbour) == null
                        && unit.canMoveTo(state.getMap(), neighbour, state.getMovementPolicy())) {
                    return new Movement(unit, unit.getPosition(), neighbour);
                }
            }
        }
        return null;
    }

    private static void openAndCloseDialog(GamePanel gamePanel, String buttonText,
                                           Class<? extends Window> dialogType) throws Exception {
        AtomicBoolean observed = new AtomicBoolean(false);
        onEdt(() -> {
            JButton button = findButton(gamePanel.getHudPanel(), buttonText);
            require(button != null && button.isEnabled(), buttonText + " button is unavailable");
            Timer closer = new Timer(500, event -> {
                for (Window openWindow : Window.getWindows()) {
                    if (dialogType.isInstance(openWindow) && openWindow.isShowing()) {
                        observed.set(true);
                        render(openWindow);
                        openWindow.dispose();
                    }
                }
            });
            closer.setRepeats(false);
            closer.start();
            button.doClick();
        });
        require(observed.get(), dialogType.getSimpleName() + " did not open and render");
    }

    private static void openCampInteraction(GamePanel gamePanel, GameState state,
                                            GameController controller) throws Exception {
        Tribe tribe = state.getTribes().get(0);
        AtomicBoolean observed = new AtomicBoolean(false);
        onEdt(() -> {
            tribe.discover();
            state.getMap().getHex(tribe.getCampCoordinate()).setVisible(true);
            Timer closer = new Timer(500, event -> {
                for (Window openWindow : Window.getWindows()) {
                    if (openWindow instanceof TribePanel && openWindow.isShowing()) {
                        TribePanel panel = (TribePanel) openWindow;
                        require(panel.getFocusedTribe() == tribe, "camp click focused the wrong tribe");
                        require(panel.getDisplayedTribeCount() >= 1, "discovered tribe is absent from panel");
                        List<JButton> actions = new ArrayList<>();
                        collectButtons(panel, actions);
                        require(!actions.isEmpty(), "tribe interaction has no actions");
                        for (JButton action : actions) {
                            if (action.getName() == null || !action.getName().startsWith("tribe-action-")) continue;
                            require(action.getToolTipText() != null && !action.getToolTipText().isBlank(),
                                    "tribe action lacks availability reason: " + action.getText());
                        }
                        require(findButton(panel, "GIFT…") != null, "gift form action missing");
                        require(findButton(panel, "TRADE…") != null, "trade form action missing");
                        require(findButton(panel, "DECLARE WAR") != null, "war confirmation action missing");
                        render(panel);
                        try {
                            writeSnapshot(panel, new File("/tmp/hex-dominion-tribe-panel.png"));
                        } catch (Exception failure) {
                            throw new RuntimeException(failure);
                        }
                        observed.set(true);
                        panel.dispose();
                    }
                }
            });
            closer.setRepeats(false);
            closer.start();
            controller.onHexClicked(tribe.getCampCoordinate());
            gamePanel.repaintAll();
        });
        require(observed.get(), "clicking a visible camp did not open TribePanel");
        require(controller.getLastOpenedTribe() == tribe, "controller did not retain opened tribe");
    }

    private static MainWindow waitForMainWindow() throws Exception {
        long deadline = System.currentTimeMillis() + WINDOW_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            AtomicReference<MainWindow> found = new AtomicReference<>();
            onEdt(() -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof MainWindow) found.set((MainWindow) window);
                }
            });
            if (found.get() != null) return found.get();
            Thread.sleep(50L);
        }
        throw new AssertionError("app.Main did not create MainWindow within " + WINDOW_TIMEOUT_MS + " ms");
    }

    private static JButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton && text.equals(((JButton) component).getText())) {
                return (JButton) component;
            }
            if (component instanceof Container) {
                JButton nested = findButton((Container) component, text);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static JButton findButtonStartingWith(Container root, String prefix) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton
                    && ((JButton) component).getText().startsWith(prefix)) return (JButton) component;
            if (component instanceof Container) {
                JButton nested = findButtonStartingWith((Container) component, prefix);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static <T extends Component> T findComponent(Container root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) return type.cast(component);
            if (component instanceof Container) {
                T nested = findComponent((Container) component, type);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static void render(Component component) {
        require(component.getWidth() > 0 && component.getHeight() > 0,
                component.getClass().getSimpleName() + " has no renderable size");
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        component.paint(graphics);
        graphics.dispose();
    }

    private static void writeSnapshot(Component component, File destination) throws Exception {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        component.paint(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", destination);
    }

    private static void writeSeasonSnapshots(GamePanel gamePanel, GameState state) throws Exception {
        int originalTurn = state.getSeasonCycle().getCurrentTurn();
        for (Season season : Season.values()) {
            state.getSeasonCycle().restoreTurn(season.ordinal() * 10 + 1);
            writeSnapshot(gamePanel.getMapPanel(),
                    new File("/tmp/hex-dominion-real-" + season.name().toLowerCase() + ".png"));
        }
        state.getSeasonCycle().restoreTurn(originalTurn);
    }

    private static void verifyHudLayout(GamePanel gamePanel) {
        List<JButton> buttons = new ArrayList<>();
        collectButtons(gamePanel.getHudPanel(), buttons);
        for (int i = 0; i < buttons.size(); i++) {
            JButton button = buttons.get(i);
            require(button.getX() >= 0 && button.getY() >= 0
                            && button.getX() + button.getWidth() <= gamePanel.getHudPanel().getWidth()
                            && button.getY() + button.getHeight() <= gamePanel.getHudPanel().getHeight(),
                    "HUD button is clipped: " + button.getText());
            for (int j = i + 1; j < buttons.size(); j++) {
                require(!button.getBounds().intersects(buttons.get(j).getBounds()),
                        "HUD buttons overlap: " + button.getText() + " / " + buttons.get(j).getText());
            }
        }
    }

    private static void collectButtons(Container root, List<JButton> buttons) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton) buttons.add((JButton) component);
            if (component instanceof Container) collectButtons((Container) component, buttons);
        }
    }

    private static void onEdt(CheckedRunnable action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        if (failure.get() != null) {
            if (failure.get() instanceof Exception) throw (Exception) failure.get();
            if (failure.get() instanceof Error) throw (Error) failure.get();
            throw new RuntimeException(failure.get());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String coordinateText(HexCoordinate coordinate) {
        return "(" + coordinate.getQ() + "," + coordinate.getR() + ")";
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }

    private static final class Movement {
        private final Unit unit;
        private final HexCoordinate origin;
        private final HexCoordinate destination;

        private Movement(Unit unit, HexCoordinate origin, HexCoordinate destination) {
            this.unit = unit;
            this.origin = origin;
            this.destination = destination;
        }
    }
}
