package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import controller.GameController;
import model.Building;
import model.Constants;
import model.GameMap;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.Unit;
import model.disaster.AffectedAreaDisaster;
import model.disaster.Bear;
import model.disaster.BearAttackEvent;
import model.disaster.BearActivity;
import model.disaster.DisasterEvent;
import model.disaster.DisasterType;
import model.military.MilitaryUnit;
import model.military.MilitaryUnitType;
import model.season.Season;
import model.tribe.Tribe;
import model.tribe.TribeMilitaryUnit;

/** The main hexagonal battle map. Renders terrain, fog of war, territory, buildings and units. */
public class MapPanel extends JPanel
        implements MouseListener, MouseMotionListener, MouseWheelListener {

    private final GameController controller;
    private final GamePanel gamePanel;

    private double hexSize = 46.0;
    private double offsetX = 70, offsetY = 70;
    private int dragStartX, dragStartY;
    private boolean dragging = false;
    private boolean centeredOnce = false;

    private static final Color GRASSLAND_COLOR = new Color(78, 132, 52);
    private static final Color FOREST_COLOR = new Color(36, 88, 36);
    private static final Color MOUNTAIN_COLOR = new Color(122, 107, 90);
    private static final Color PLAIN_COLOR = new Color(192, 172, 122);
    private static final Color FOG_COLOR = new Color(12, 14, 28);
    private static final Color EXPLORED_OVERLAY = new Color(0, 0, 0, 140);
    private static final Color TERRITORY_BORDER = new Color(212, 175, 55, 220);
    private static final Color REACHABLE_COLOR = new Color(110, 235, 110, 95);
    private static final Color REACHABLE_BORDER = new Color(150, 255, 150, 200);
    private static final Color GOLD = new Color(212, 175, 55);

    private HexCoordinate hoverHex = null;
    private final Set<HexCoordinate> reachableHexes = new HashSet<>();
    private float selectionPulse = 0f;
    private String animatedDisasterId = null;
    private long disasterAnimationStartedAt = 0;

    private static final long DISASTER_ANIMATION_DURATION = 3000;
    private final Timer pulseTimer;

    // Smooth movement animation: each unit's displayed position eases toward its hex (no teleport).
    private final Map<Unit, Point2D> animatedRaw = new HashMap<>();
    private final Map<Bear, Long> bearDamageRevision = new HashMap<>();
    private final Map<Bear, Long> bearDamageStartedAt = new HashMap<>();

    // Minimap geometry (bottom-right corner).
    private static final int MINI_W = 188, MINI_H = 150, MINI_PAD = 12;

    public MapPanel(GameController controller, GamePanel gamePanel) {
        this.controller = controller;
        this.gamePanel = gamePanel;
        setBackground(FOG_COLOR);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        pulseTimer = new Timer(33, e -> {
            selectionPulse += 0.08f;
            if (selectionPulse > 2 * Math.PI) selectionPulse -= (float) (2 * Math.PI);
            stepAnimations();
            repaint();
        });
        pulseTimer.start();
    }

    /** Eases every unit's displayed position toward its true hex, in zoom-independent raw space. */
    private void stepAnimations() {
        if (controller.getGameState() == null) return;
        Set<Unit> animatedUnits = new HashSet<>(controller.getGameState().getPlayer().getUnits());
        Set<Bear> activeBears = new HashSet<>();
        BearAttackEvent bearAttack = controller.getGameState().getActiveBearAttack();
        if (bearAttack != null) {
            activeBears.addAll(bearAttack.getBears());
            animatedUnits.addAll(activeBears);
        }
        animatedRaw.keySet().removeIf(unit -> !animatedUnits.contains(unit));
        bearDamageRevision.keySet().removeIf(bear -> !activeBears.contains(bear));
        bearDamageStartedAt.keySet().removeIf(bear -> !activeBears.contains(bear));

        for (Unit u : animatedUnits) {
            if (!u.isAlive()) continue;
            Point2D target = hexToPixelRaw(u.getPosition());
            Point2D cur = animatedRaw.get(u);
            if (cur == null) {
                animatedRaw.put(u, target);
            } else {
                double nx = cur.getX() + (target.getX() - cur.getX()) * 0.25;
                double ny = cur.getY() + (target.getY() - cur.getY()) * 0.25;
                animatedRaw.put(u, new Point2D.Double(nx, ny));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (controller.getGameState() == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        GameState gs = controller.getGameState();
        GameMap map = gs.getMap();
        Player player = gs.getPlayer();

        if (!centeredOnce) {
            centerOnTownHall(player);
            centeredOnce = true;
        }

        // Subtle vignette background.
        GradientPaint bgGrad = new GradientPaint(0, 0, new Color(16, 18, 36),
                getWidth(), getHeight(), new Color(8, 9, 20));
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, getWidth(), getHeight());

        reachableHexes.clear();
        reachableHexes.addAll(controller.getReachableHexes());

        for (Hex hex : map.getAllHexes()) {
            drawHex(g2, hex, player);
        }

        drawRoads(g2, map);
        drawRivers(g2, map);
        drawWalls(g2, map);

        drawTerritoryBorders(g2, player, map);

        for (Building b : player.getBuildings()) {
            Hex hex = map.getHex(b.getPosition());
            if (hex != null && hex.getIsExplored()) {
                drawBuilding(g2, b, hexToPixel(b.getPosition()), hex.isVisible());
            }
        }

        for (HexCoordinate post : map.getTradingPosts()) {
            Hex hex = map.getHex(post);
            if (hex != null && hex.getIsExplored()) {
                drawTradingPost(g2, hexToPixel(post), hex.isVisible());
            }
        }

        for (Tribe tribe : gs.getTribes()) {
            Hex camp = map.getHex(tribe.getCampCoordinate());
            if (camp != null && (camp.isVisible() || tribe.isOutpost() && camp.getIsExplored())) {
                drawTribeCamp(g2, tribe, hexToPixel(tribe.getCampCoordinate()), camp.isVisible());
            }
            for (TribeMilitaryUnit unit : tribe.getMilitaryUnits()) {
                Hex unitHex = map.getHex(unit.getPosition());
                if (unitHex != null && unitHex.isVisible()) {
                    drawTribeMilitaryUnit(g2, unit, hexToPixel(unit.getPosition()));
                }
            }
        }

        for (Unit u : player.getUnits()) {
            if (!u.isAlive()) continue;
            Hex hex = map.getHex(u.getPosition());
            if (hex != null && hex.isVisible()) {
                drawUnit(g2, u, animatedCenter(u));
            }
        }

        drawActiveBears(g2, gs, map);

        drawDisasterAnimation(g2, gs, map);

        if (controller.getSelectedUnit() != null) {
            drawSelectionGlow(g2, animatedCenter(controller.getSelectedUnit()));
        } else if (controller.getSelectedHex() != null) {
            drawHexHighlight(g2, hexToPixel(controller.getSelectedHex()), new Color(100, 150, 255, 90));
        }

        if (hoverHex != null) {
            Hex h = map.getHex(hoverHex);
            if (h != null && h.getIsExplored()) {
                drawHexHighlight(g2, hexToPixel(hoverHex), new Color(255, 255, 255, 35));
            }
        }

        if (controller.isBuildMode() && controller.getPendingBuildType() != null) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(16, getHeight() - 44, 470, 30, 8, 8);
            g2.setColor(GOLD);
            g2.setFont(new Font("Georgia", Font.BOLD, 15));
            g2.drawString("Click an adjacent territory hex to place: "
                    + controller.getPendingBuildType().name(), 26, getHeight() - 23);
        }

        drawSeasonEffects(g2, gs.getSeasonCycle().getCurrentSeason());
        drawMinimap(g2, gs, map, player);

        // Controls hint.
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(new Color(150, 150, 175, 170));
        g2.drawString("Drag to pan  •  Scroll to zoom  •  Click minimap to jump  •  Right-click / ESC to deselect",
                16, getHeight() - 6);

        g2.dispose();
    }

    private void drawDisasterAnimation(
            Graphics2D g2,
            GameState gameState,
            GameMap map
    ) {
        DisasterEvent disaster =
                gameState.getLastDisasterEvent();

        if (disaster == null) {
            return;
        }

        if (!disaster.getId().equals(animatedDisasterId)) {
            animatedDisasterId = disaster.getId();
            disasterAnimationStartedAt =
                    System.currentTimeMillis();
        }

        long elapsed =
                System.currentTimeMillis()
                        - disasterAnimationStartedAt;

        if (elapsed > DISASTER_ANIMATION_DURATION) {
            return;
        }

        Set<HexCoordinate> coordinates;

        if (disaster instanceof AffectedAreaDisaster) {
            AffectedAreaDisaster areaDisaster =
                    (AffectedAreaDisaster) disaster;

            if (areaDisaster.getAffectedArea() == null) {
                return;
            }

            coordinates =
                    areaDisaster
                            .getAffectedArea()
                            .getAffectedCoordinates();

        } else {
            coordinates =
                    Collections.singleton(
                            disaster.getOrigin()
                    );
        }

        float progress =
                elapsed
                        / (float) DISASTER_ANIMATION_DURATION;

        List<HexCoordinate> ordered = new ArrayList<>(coordinates);
        ordered.sort(Comparator.comparingInt(c -> c.distanceTo(disaster.getOrigin())));
        int count = Math.max(1, ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            HexCoordinate coordinate = ordered.get(i);

            Hex hex = map.getHex(coordinate);

            // Disaster outside fog only gives HUD notification.
            if (hex == null || !hex.isVisible()) {
                continue;
            }

            Point2D center =
                    hexToPixel(coordinate);

            float localProgress = Math.max(0f, Math.min(1f,
                    progress * 1.45f - (i / (float) count) * 0.45f));
            if (localProgress <= 0f) continue;
            drawDisasterEffect(
                    g2,
                    disaster.getType(),
                    center,
                    localProgress
            );
        }
    }

    private void drawDisasterEffect(
            Graphics2D g2,
            DisasterType type,
            Point2D center,
            float progress
    ) {
        switch (type) {

            case EARTHQUAKE:
                drawEarthquakeEffect(
                        g2,
                        center,
                        progress
                );
                break;

            case FLOOD:
                drawWaterEffect(
                        g2,
                        center,
                        progress
                );
                break;

            case TSUNAMI:
                drawTsunamiEffect(g2, center, progress);
                break;

            case SEA_STORM:
                drawSeaStormEffect(g2, center, progress);
                break;

            case VOLCANIC_ERUPTION:
                drawVolcanoEffect(
                        g2,
                        center,
                        progress
                );
                break;

            case TORNADO:
                drawTornadoEffect(
                        g2,
                        center,
                        progress
                );
                break;

            case AVALANCHE:
                drawAvalancheEffect(
                        g2,
                        center,
                        progress
                );
                break;

            case BEAR_ATTACK:
                drawBearEffect(
                        g2,
                        center,
                        progress
                );
                break;

            default:
                break;
        }
    }

    private void drawEarthquakeEffect(
            Graphics2D g2,
            Point2D center,
            float progress
    ) {
        int cx = (int) center.getX();
        int cy = (int) center.getY();

        int pulse =
                8 + (int) (
                        Math.abs(
                                Math.sin(progress * 25)
                        ) * 18
                );

        g2.setColor(
                new Color(90, 55, 35, 180)
        );

        g2.setStroke(
                new BasicStroke(3f)
        );

        g2.drawLine(
                cx - pulse,
                cy - 15,
                cx - 3,
                cy
        );

        g2.drawLine(
                cx - 3,
                cy,
                cx + pulse,
                cy + 12
        );

        g2.drawLine(
                cx,
                cy,
                cx + 8,
                cy - 15
        );
    }

    private void drawWaterEffect(
            Graphics2D g2,
            Point2D center,
            float progress
    ) {
        int cx = (int) center.getX();
        int cy = (int) center.getY();

        int radius =
                (int) (
                        hexSize
                                * (0.4 + progress * 0.5)
                );

        int alpha =
                Math.max(
                        40,
                        180 - (int) (progress * 130)
                );

        g2.setColor(
                new Color(
                        60,
                        150,
                        230,
                        alpha
                )
        );

        g2.fillOval(
                cx - radius,
                cy - radius / 2,
                radius * 2,
                radius
        );

        g2.setColor(
                new Color(
                        180,
                        230,
                        255,
                        alpha
                )
        );

        g2.setStroke(
                new BasicStroke(2f)
        );

        int wave =
                (int) (
                        Math.sin(progress * 20)
                                * 7
                );

        g2.drawArc(
                cx - radius,
                cy - 8 + wave,
                radius * 2,
                18,
                0,
                180
        );
    }

    private void drawTsunamiEffect(Graphics2D g2, Point2D center, float progress) {
        int cx = (int) center.getX(), cy = (int) center.getY();
        int radius = (int) (hexSize * (0.35 + progress * 0.8));
        int alpha = Math.max(35, 220 - (int) (progress * 160));
        g2.setColor(new Color(30, 115, 210, alpha));
        g2.fillOval(cx - radius, cy - radius / 2, radius * 2, radius);
        g2.setColor(new Color(235, 250, 255, alpha));
        g2.setStroke(new BasicStroke(5f));
        g2.drawArc(cx - radius, cy - radius / 2 - 5, radius * 2, radius, 15, 150);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawSeaStormEffect(Graphics2D g2, Point2D center, float progress) {
        drawWaterEffect(g2, center, progress);
        int cx = (int) center.getX(), cy = (int) center.getY();
        g2.setColor(new Color(55, 62, 82, 190));
        for (int i = 0; i < 3; i++) {
            int dx = (int) (Math.sin(progress * 20 + i) * 10);
            g2.fillOval(cx - 25 + i * 16 + dx, cy - 30 - (i % 2) * 5, 28, 14);
        }
        g2.setColor(new Color(180, 220, 255, 190));
        g2.setStroke(new BasicStroke(2f));
        for (int i = -2; i <= 2; i++)
            g2.drawLine(cx + i * 10, cy - 13, cx + i * 10 - 9, cy + 14);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawVolcanoEffect(
            Graphics2D g2,
            Point2D center,
            float progress
    ) {
        int cx = (int) center.getX();
        int cy = (int) center.getY();

        int radius =
                10 + (int) (
                        Math.sin(progress * 18) * 6
                );

        g2.setColor(
                new Color(255, 85, 20, 210)
        );

        g2.fillOval(
                cx - radius,
                cy - radius,
                radius * 2,
                radius * 2
        );

        for (int i = 0; i < 4; i++) {

            int rise =
                    (int) (
                            (progress * 80 + i * 13)
                                    % 55
                    );

            g2.setColor(
                    new Color(
                            255,
                            180,
                            30,
                            190
                    )
            );

            g2.fillOval(
                    cx - 12 + i * 8,
                    cy - rise,
                    6,
                    6
            );
        }
    }

    private void drawTornadoEffect(
            Graphics2D g2,
            Point2D center,
            float progress
    ) {
        int cx = (int) center.getX();
        int cy = (int) center.getY();

        g2.setStroke(
                new BasicStroke(3f)
        );

        for (int i = 0; i < 4; i++) {

            int width =
                    12 + i * 9;

            int y =
                    cy - 25 + i * 12;

            int offset =
                    (int) (
                            Math.sin(
                                    progress * 25 + i
                            ) * 8
                    );

            g2.setColor(
                    new Color(
                            210,
                            210,
                            220,
                            180 - i * 25
                    )
            );

            g2.drawOval(
                    cx - width / 2 + offset,
                    y,
                    width,
                    9
            );
        }
    }

    private void drawAvalancheEffect(
            Graphics2D g2,
            Point2D center,
            float progress
    ) {
        int cx = (int) center.getX();
        int cy = (int) center.getY();

        int size =
                15 + (int) (
                        progress * 35
                );

        g2.setColor(
                new Color(
                        245,
                        245,
                        255,
                        190
                )
        );

        g2.fillOval(
                cx - size,
                cy - size / 2,
                size * 2,
                size
        );

        g2.setColor(
                new Color(
                        150,
                        150,
                        160,
                        170
                )
        );

        for (int i = 0; i < 4; i++) {
            g2.fillOval(
                    cx - 20 + i * 12,
                    cy + (i % 2) * 8,
                    7,
                    7
            );
        }
    }

    private void drawBearEffect(
            Graphics2D g2,
            Point2D center,
            float progress
    ) {
        int cx = (int) center.getX();
        int cy = (int) center.getY();

        int bounce =
                (int) (
                        Math.sin(progress * 18) * 5
                );

        g2.setColor(
                new Color(105, 65, 35)
        );

        g2.fillOval(
                cx - 13,
                cy - 10 + bounce,
                26,
                22
        );

        g2.fillOval(
                cx - 10,
                cy - 18 + bounce,
                20,
                17
        );

        g2.fillOval(
                cx - 11,
                cy - 21 + bounce,
                7,
                7
        );

        g2.fillOval(
                cx + 4,
                cy - 21 + bounce,
                7,
                7
        );

        g2.setColor(Color.BLACK);

        g2.fillOval(
                cx - 5,
                cy - 13 + bounce,
                3,
                3
        );

        g2.fillOval(
                cx + 3,
                cy - 13 + bounce,
                3,
                3
        );
    }

    // ---- Minimap -----------------------------------------------------------

    private java.awt.Rectangle minimapRect() {
        return new java.awt.Rectangle(getWidth() - MINI_W - MINI_PAD,
                getHeight() - MINI_H - MINI_PAD - 18, MINI_W, MINI_H);
    }

    private void drawMinimap(Graphics2D g2, GameState gs, GameMap map, Player player) {
        java.awt.Rectangle r = minimapRect();
        g2.setColor(new Color(8, 9, 20, 235));
        g2.fillRoundRect(r.x - 4, r.y - 16, r.width + 8, r.height + 22, 8, 8);
        g2.setColor(GOLD);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(r.x - 4, r.y - 16, r.width + 8, r.height + 22, 8, 8);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("MINIMAP", r.x, r.y - 4);

        int maxQ = 0, maxR = 0;
        for (Hex h : map.getAllHexes()) {
            maxQ = Math.max(maxQ, h.getCoordinate().getQ());
            maxR = Math.max(maxR, h.getCoordinate().getR());
        }
        double cellW = (double) r.width / (maxQ + 1);
        double cellH = (double) r.height / (maxR + 1.5);

        for (Hex h : map.getAllHexes()) {
            HexCoordinate c = h.getCoordinate();
            int mx = r.x + (int) (c.getQ() * cellW);
            int my = r.y + (int) ((c.getR() + 0.5 * (c.getQ() & 1)) * cellH);
            int w = (int) Math.ceil(cellW) + 1, hh = (int) Math.ceil(cellH) + 1;
            Color cell;
            if (!h.getIsExplored()) {
                cell = new Color(18, 20, 34);
            } else {
                cell = getTerrainColor(h.getTerrainType());
                if (!h.isVisible()) cell = desaturate(cell, 0.4f);
            }
            g2.setColor(cell);
            g2.fillRect(mx, my, w, hh);
            if (h.getIsExplored() && player.isInTerritory(c)) {
                g2.setColor(new Color(212, 175, 55, 90));
                g2.fillRect(mx, my, w, hh);
            }
        }
        // Buildings (gold) and town hall (bright).
        for (Building b : player.getBuildings()) {
            if (!b.isActive()) continue;
            HexCoordinate c = b.getPosition();
            int mx = r.x + (int) (c.getQ() * cellW);
            int my = r.y + (int) ((c.getR() + 0.5 * (c.getQ() & 1)) * cellH);
            g2.setColor(b.getType() == Constants.BuildingType.TOWN_HALL ? Color.WHITE : new Color(240, 200, 90));
            g2.fillRect(mx - 1, my - 1, 4, 4);
        }
        // Units.
        for (Unit u : player.getUnits()) {
            if (!u.isAlive()) continue;
            HexCoordinate c = u.getPosition();
            int mx = r.x + (int) (c.getQ() * cellW);
            int my = r.y + (int) ((c.getR() + 0.5 * (c.getQ() & 1)) * cellH);
            g2.setColor(getUnitColor(u.getUnitType()));
            g2.fillOval(mx - 1, my - 1, 4, 4);
        }
        // Current viewport box.
        g2.setColor(new Color(255, 255, 255, 160));
        g2.setStroke(new BasicStroke(1f));
        double vx = (-offsetX) / (hexSize * 1.5);
        double vy = (-offsetY) / (hexSize * Math.sqrt(3));
        double vw = getWidth() / (hexSize * 1.5);
        double vh = getHeight() / (hexSize * Math.sqrt(3));
        int bx = r.x + (int) (vx * cellW);
        int by = r.y + (int) (vy * cellH);
        g2.drawRect(bx, by, (int) (vw * cellW), (int) (vh * cellH));
    }

    /** If the click landed on the minimap, recenter the main camera there and return true. */
    private boolean handleMinimapClick(int px, int py) {
        java.awt.Rectangle r = minimapRect();
        if (!r.contains(px, py)) return false;
        GameMap map = controller.getGameState().getMap();
        int maxQ = 0, maxR = 0;
        for (Hex h : map.getAllHexes()) {
            maxQ = Math.max(maxQ, h.getCoordinate().getQ());
            maxR = Math.max(maxR, h.getCoordinate().getR());
        }
        double cellW = (double) r.width / (maxQ + 1);
        double cellH = (double) r.height / (maxR + 1.5);
        int q = (int) Math.round((px - r.x) / cellW);
        int rr = (int) Math.round((py - r.y) / cellH - 0.5 * (q & 1));
        Point2D raw = hexToPixelRaw(new HexCoordinate(q, rr));
        offsetX = getWidth() / 2.0 - raw.getX();
        offsetY = getHeight() / 2.0 - raw.getY();
        repaint();
        return true;
    }

    private void centerOnTownHall(Player player) {
        for (Building b : player.getBuildings()) {
            if (b.getType() == Constants.BuildingType.TOWN_HALL) {
                Point2D p = hexToPixelRaw(b.getPosition());
                offsetX = getWidth() / 2.0 - p.getX();
                offsetY = getHeight() / 2.0 - p.getY();
                return;
            }
        }
    }

    /** Rivers are model-owned edges, so both adjoining hexes must be explored before drawing one. */
    private void drawRivers(Graphics2D g2, GameMap map) {
        for (model.HexEdge edge : map.getRiverEdges()) {
            Hex a = map.getHex(edge.getFirst()), b = map.getHex(edge.getSecond());
            if (a == null || b == null || !a.getIsExplored() || !b.getIsExplored()) continue;
            Point2D pa = hexToPixel(edge.getFirst()), pb = hexToPixel(edge.getSecond());
            double mx = (pa.getX() + pb.getX()) / 2.0;
            double my = (pa.getY() + pb.getY()) / 2.0;
            double dx = pb.getX() - pa.getX(), dy = pb.getY() - pa.getY(), len = Math.max(1, Math.hypot(dx, dy));
            double px = -dy / len, py = dx / len, half = hexSize * .48;
            double x1 = mx - px * half, y1 = my - py * half, x2 = mx + px * half, y2 = my + py * half;
            boolean visible = a.isVisible() || b.isVisible();
            java.awt.geom.Path2D river = new java.awt.geom.Path2D.Double();
            river.moveTo(x1, y1); river.quadTo(mx + dx / len * 4, my + dy / len * 4, x2, y2);
            g2.setColor(visible ? new Color(55, 155, 225, 225) : new Color(45, 85, 115, 150));
            g2.setStroke(new BasicStroke(Math.max(5f, (float) hexSize * .11f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(river);
            g2.setColor(new Color(185, 225, 250, visible ? 150 : 70));
            g2.setStroke(new BasicStroke(Math.max(1f, (float) hexSize * 0.025f)));
            g2.draw(river);
            if (edge.hasBridge()) drawBridge(g2, mx, my, dx / len, dy / len);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawRoads(Graphics2D g2, GameMap map) {
        for (Hex hex : map.getAllHexes()) {
            if (!hex.hasRoad() || !hex.getIsExplored()) continue;
            Point2D center = hexToPixel(hex.getCoordinate()); boolean connected = false;
            for (HexCoordinate neighbour : hex.getCoordinate().findNeighbours()) {
                Hex other = map.getHex(neighbour);
                if (other == null || !other.hasRoad() || !other.getIsExplored()) continue;
                Point2D end = hexToPixel(neighbour); double ex = (center.getX() + end.getX()) / 2;
                double ey = (center.getY() + end.getY()) / 2; connected = true;
                g2.setColor(new Color(75, 55, 38, 210)); g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new java.awt.geom.Line2D.Double(center.getX(), center.getY(), ex, ey));
                g2.setColor(new Color(184, 145, 91, 225)); g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new java.awt.geom.Line2D.Double(center.getX(), center.getY(), ex, ey));
            }
            if (!connected) { g2.setColor(new Color(184, 145, 91, 220)); g2.fillOval((int)center.getX()-5, (int)center.getY()-5, 10, 10); }
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawBridge(Graphics2D g2, double mx, double my, double nx, double ny) {
        double px = -ny, py = nx;
        g2.setColor(new Color(70, 48, 31)); g2.setStroke(new BasicStroke(13f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g2.draw(new java.awt.geom.Line2D.Double(mx - nx * 12, my - ny * 12, mx + nx * 12, my + ny * 12));
        g2.setColor(new Color(188, 132, 71)); g2.setStroke(new BasicStroke(3f));
        for (int i = -2; i <= 2; i++) { double ox = nx * i * 5, oy = ny * i * 5;
            g2.draw(new java.awt.geom.Line2D.Double(mx + ox - px * 7, my + oy - py * 7, mx + ox + px * 7, my + oy + py * 7)); }
    }

    private void drawWalls(Graphics2D g2, GameMap map) {
        for (model.HexEdge edge : map.getWallEdges()) {
            if (!edge.hasWall()) continue;
            Hex a = map.getHex(edge.getFirst()), b = map.getHex(edge.getSecond());
            if (a == null || b == null || !a.getIsExplored() || !b.getIsExplored()) continue;
            Point2D pa = hexToPixel(edge.getFirst()), pb = hexToPixel(edge.getSecond());
            double mx = (pa.getX()+pb.getX())/2, my=(pa.getY()+pb.getY())/2;
            double dx=pb.getX()-pa.getX(), dy=pb.getY()-pa.getY(), len=Math.max(1,Math.hypot(dx,dy));
            double px=-dy/len, py=dx/len, half=hexSize*.48;
            float hp=edge.getWall().getCurrentHp()/(float)edge.getWall().getMaxHp();
            Color stone = hp > .5f ? new Color(188, 187, 177) : new Color(153, 111, 94);
            java.awt.geom.AffineTransform old = g2.getTransform();
            g2.translate(mx, my);
            g2.rotate(Math.atan2(py, px));
            g2.setColor(new Color(28, 26, 28, 115));
            g2.fillRoundRect((int)-half, -2, (int)(half * 2), 10, 4, 4);
            int blockW = Math.max(7, (int)(half * 2 / 5.4));
            for (int i = -2; i <= 2; i++) {
                int bx = (int)(i * half / 2.45) - blockW / 2;
                g2.setPaint(new GradientPaint(bx, -7, stone.brighter(), bx, 6, stone.darker()));
                g2.fillRoundRect(bx, -7 + Math.abs(i % 2), blockW, 12, 3, 3);
                g2.setColor(new Color(63, 59, 57, 185));
                g2.drawRoundRect(bx, -7 + Math.abs(i % 2), blockW, 12, 3, 3);
            }
            g2.setColor(new Color(226, 222, 205, 105));
            g2.drawLine((int)-half + 3, -6, (int)half - 3, -6);
            g2.setTransform(old);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawActiveBears(Graphics2D g2, GameState state, GameMap map) {
        BearAttackEvent attack = state.getActiveBearAttack();
        if (attack == null) return;
        for (Bear bear : attack.getBears()) {
            Hex hex = map.getHex(bear.getPosition());
            if (hex == null || !hex.isVisible()) continue;

            long revision = bear.getDamageRevision();
            if (bearDamageRevision.getOrDefault(bear, -1L) != revision) {
                bearDamageRevision.put(bear, revision);
                if (revision > 0) {
                    bearDamageStartedAt.put(bear, System.currentTimeMillis());
                }
            }

            Point2D center = animatedCenter(bear);
            if (bear.isAlive()) {
                drawBearAt(g2, bear, center);
                if (bear.getActivity() == BearActivity.RETURNING
                        || bear.getActivity() == BearActivity.RETURNED) {
                    drawBearReturnEffect(g2, bear, center);
                }
            }
            drawBearDamageEffect(g2, bear, center);
        }
    }

    private void drawBearReturnEffect(Graphics2D g2, Bear bear, Point2D center) {
        Point2D den = hexToPixel(bear.getDen());
        float pulse = (float) ((Math.sin(selectionPulse * 2.2) + 1) / 2.0);
        g2.setColor(new Color(231, 193, 104, 90 + (int) (pulse * 80)));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{6f, 6f}, selectionPulse * 4));
        g2.drawLine((int) center.getX(), (int) center.getY(),
                (int) den.getX(), (int) den.getY());
        g2.setStroke(new BasicStroke(1f));
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.drawString(bear.hasReturnedToDen() ? "DEN" : "RETURNING",
                (int) center.getX() - 22, (int) center.getY() - 27);
    }

    private void drawBearDamageEffect(Graphics2D g2, Bear bear, Point2D center) {
        Long started = bearDamageStartedAt.get(bear);
        if (started == null) return;
        long elapsed = System.currentTimeMillis() - started;
        if (elapsed > 750) {
            bearDamageStartedAt.remove(bear);
            return;
        }
        float progress = elapsed / 750f;
        int radius = 14 + (int) (progress * 24);
        int alpha = Math.max(0, 220 - (int) (progress * 220));
        g2.setColor(new Color(255, 74, 48, alpha));
        g2.setStroke(new BasicStroke(3f));
        g2.drawOval((int) center.getX() - radius, (int) center.getY() - radius,
                radius * 2, radius * 2);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawBearAt(Graphics2D g2, Bear bear, Point2D center) {
        int cx = (int) center.getX(), cy = (int) center.getY() + 4;
        int bob = (int) (Math.sin(selectionPulse * 1.8) * 2);
        g2.setColor(new Color(45, 25, 12, 100));
        g2.fillOval(cx - 17, cy + 9, 34, 10);
        g2.setColor(new Color(112, 70, 38));
        g2.fillOval(cx - 15, cy - 9 + bob, 30, 24);
        g2.fillOval(cx - 11, cy - 18 + bob, 22, 18);
        g2.fillOval(cx - 13, cy - 21 + bob, 8, 8);
        g2.fillOval(cx + 5, cy - 21 + bob, 8, 8);
        g2.setColor(Color.BLACK);
        g2.fillOval(cx - 5, cy - 12 + bob, 3, 3);
        g2.fillOval(cx + 3, cy - 12 + bob, 3, 3);
        drawHealthBar(g2, cx, cy + 20, bear.getCurrentHp(), bear.getMaxHp());
    }

    private void drawSeasonEffects(Graphics2D g2, Season season) {
        long now = System.currentTimeMillis();
        if (season == Season.SUMMER) {
            g2.setPaint(new java.awt.RadialGradientPaint(getWidth() * .72f, getHeight() * .15f,
                    Math.max(getWidth(), getHeight()) * .8f,
                    new float[]{0f, .42f, 1f}, new Color[]{new Color(255, 226, 142, 42),
                    new Color(255, 183, 65, 16), new Color(80, 45, 20, 7)}));
            g2.fillRect(0, 0, getWidth(), getHeight());
            drawSummerLight(g2, now);
        } else if (season == Season.SPRING) {
            g2.setPaint(new GradientPaint(0, 0, new Color(174, 236, 188, 18),
                    getWidth(), getHeight(), new Color(245, 204, 222, 9)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            drawFloweringBranch(g2);
            drawSpringLandAccents(g2);
            for (int i = 0; i < 23; i++) {
                double speed = 70.0 + (i % 7) * 10;
                double x = Math.floorMod(i * 173 + (int) (now / speed), Math.max(1, getWidth() + 70)) - 35;
                double y = Math.floorMod(i * 109 + (int) (now / (speed * .55)), Math.max(1, getHeight() + 55)) - 28;
                x += Math.sin(now / 1050.0 + i * 1.37) * (10 + i % 7);
                drawPetal(g2, x, y, .55 + (i % 5) * .11, now / 1250.0 + i * .73,
                        i % 3, 75 + (i % 4) * 18);
            }
        } else if (season == Season.AUTUMN) {
            g2.setPaint(new GradientPaint(0, 0, new Color(67, 76, 92, 32),
                    getWidth(), getHeight(), new Color(151, 82, 35, 25)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            double gust = .5 + .5 * Math.sin(now / 1450.0);
            drawAutumnRain(g2, now, gust);
            drawWindBands(g2, now, gust, new Color(220, 226, 218, 28));
            for (int i = 0; i < 28; i++) {
                double speed = 42 + (i % 8) * 8;
                double x = Math.floorMod(i * 127 + (int) (now / speed * (1.25 + gust)),
                        Math.max(1, getWidth() + 70)) - 35;
                double y = Math.floorMod(i * 73 + (int) (now / (speed * .55)),
                        Math.max(1, getHeight() + 60)) - 30;
                x += Math.sin(now / 720.0 + i) * (12 + gust * 18);
                Color leaf = i % 3 == 0 ? new Color(190, 73, 35) : i % 3 == 1
                        ? new Color(220, 137, 39) : new Color(139, 91, 39);
                drawLeaf(g2, x, y, .55 + (i % 5) * .12, now / 680.0 + i * .9,
                        leaf, 105 + i % 4 * 20);
            }
        } else if (season == Season.WINTER) {
            g2.setPaint(new GradientPaint(0, 0, new Color(183, 216, 244, 42),
                    0, getHeight(), new Color(65, 91, 130, 30)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            drawWindBands(g2, now, .35, new Color(230, 244, 255, 22));
            for (int i = 0; i < 52; i++) {
                int speed = 22 + i % 15;
                double y = Math.floorMod(i * 79 + (int) (now / speed), Math.max(1, getHeight() + 30)) - 15;
                double x = Math.floorMod(i * 131 + (int) (Math.sin(now / 760.0 + i) * 22),
                        Math.max(1, getWidth() + 30)) - 15;
                drawSnowflake(g2, x, y, 1.7 + i % 4, 95 + i % 5 * 23);
            }
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawSummerLight(Graphics2D g2, long now) {
        double shimmer = .65 + .35 * Math.sin(now / 1800.0);
        java.awt.geom.Path2D beam = new java.awt.geom.Path2D.Double();
        beam.moveTo(getWidth() * .58, 0); beam.lineTo(getWidth() * .82, 0);
        beam.lineTo(getWidth() * .62, getHeight()); beam.lineTo(getWidth() * .34, getHeight()); beam.closePath();
        g2.setColor(new Color(255, 234, 160, (int) (8 + shimmer * 7)));
        g2.fill(beam);
    }

    private void drawFloweringBranch(Graphics2D g2) {
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(69, 45, 36, 150));
        java.awt.geom.CubicCurve2D main = new java.awt.geom.CubicCurve2D.Double(-18, 72, 35, 67, 96, 23, 166, 8);
        g2.draw(main);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new java.awt.geom.CubicCurve2D.Double(51, 54, 63, 37, 70, 17, 82, -5));
        g2.draw(new java.awt.geom.CubicCurve2D.Double(104, 24, 125, 26, 142, 43, 158, 52));
        int[][] flowers = {{8,68},{20,65},{34,60},{47,54},{59,45},{70,34},{78,18},{91,30},
                {105,23},{119,20},{133,16},{146,12},{142,41},{155,49},{163,8}};
        for (int i = 0; i < flowers.length; i++) {
            drawBlossom(g2, flowers[i][0], flowers[i][1], .58 + (i % 4) * .1, i * .47, 175 + i % 3 * 18);
        }
    }

    private void drawSpringLandAccents(Graphics2D g2) {
        GameState state = controller.getGameState();
        if (state == null) return;
        int drawn = 0;
        for (Hex hex : state.getMap().getAllHexes()) {
            if (!hex.isVisible() || (hex.getTerrainType() != Constants.TerrainType.GRASSLAND
                    && hex.getTerrainType() != Constants.TerrainType.FOREST)) continue;
            HexCoordinate coordinate = hex.getCoordinate();
            if (Math.floorMod(coordinate.getQ() * 17 + coordinate.getR() * 31, 5) != 0) continue;
            Point2D center = hexToPixel(coordinate);
            if (center.getX() < 0 || center.getX() > getWidth() || center.getY() < 0 || center.getY() > getHeight()) continue;
            drawBlossom(g2, center.getX() - hexSize * .35, center.getY() + hexSize * .28,
                    .34, coordinate.getQ() * .7, 70);
            drawBlossom(g2, center.getX() - hexSize * .22, center.getY() + hexSize * .34,
                    .27, coordinate.getR() * .6, 58);
            if (++drawn >= 9) break;
        }
    }

    private void drawPetal(Graphics2D g2, double x, double y, double scale,
                           double rotation, int palette, int alpha) {
        java.awt.geom.AffineTransform old = g2.getTransform();
        g2.translate(x, y); g2.rotate(rotation); g2.scale(scale, scale);
        java.awt.geom.Path2D petal = new java.awt.geom.Path2D.Double();
        petal.moveTo(0, -8); petal.curveTo(5, -5, 6, 2, 0, 8);
        petal.curveTo(-4, 3, -5, -4, 0, -8); petal.closePath();
        Color[] colors = {new Color(255, 205, 222), new Color(255, 232, 237), new Color(244, 179, 207)};
        Color color = colors[Math.floorMod(palette, colors.length)];
        g2.setPaint(new GradientPaint(-3, -7,
                new Color(255, 250, 252, Math.min(255, alpha + 30)), 4, 7,
                new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha)));
        g2.fill(petal);
        g2.setColor(new Color(184, 105, 139, Math.max(35, alpha / 2)));
        g2.setStroke(new BasicStroke(.65f)); g2.draw(petal);
        g2.setTransform(old);
    }

    private void drawBlossom(Graphics2D g2, double x, double y, double scale, double rotation, int alpha) {
        java.awt.geom.AffineTransform old = g2.getTransform();
        g2.translate(x, y); g2.rotate(rotation); g2.scale(scale, scale);
        for (int p = 0; p < 5; p++) {
            double angle = p * Math.PI * 2 / 5;
            java.awt.geom.Path2D petal = new java.awt.geom.Path2D.Double();
            petal.moveTo(0, -2); petal.curveTo(-4, -5, -4, -11, 0, -13);
            petal.curveTo(5, -10, 5, -5, 0, -2); petal.closePath();
            java.awt.geom.AffineTransform turn = java.awt.geom.AffineTransform.getRotateInstance(angle);
            java.awt.Shape shape = turn.createTransformedShape(petal);
            g2.setPaint(new GradientPaint(0, -12, new Color(255, 250, 252, Math.min(255, alpha + 25)),
                    0, -2, new Color(246, p % 2 == 0 ? 173 : 196, 211, alpha)));
            g2.fill(shape);
            g2.setColor(new Color(177, 99, 132, Math.max(30, alpha / 3))); g2.draw(shape);
        }
        g2.setColor(new Color(247, 207, 85, Math.min(255, alpha + 35))); g2.fillOval(-2, -2, 4, 4);
        g2.setTransform(old);
    }

    private void drawAutumnRain(Graphics2D g2, long now, double gust) {
        for (int i = 0; i < 58; i++) {
            double x = Math.floorMod(i * 83 + (int) (now / (21 + i % 5)), Math.max(1, getWidth() + 90)) - 45;
            double y = Math.floorMod(i * 139 + (int) (now / (12 + i % 4)), Math.max(1, getHeight() + 55)) - 28;
            int length = 9 + i % 5 * 2;
            int slant = 4 + (int) (gust * 7);
            g2.setColor(new Color(174, 207, 225, 28 + i % 4 * 8));
            g2.setStroke(new BasicStroke(i % 7 == 0 ? 1.2f : .75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int) x, (int) y, (int) x + slant, (int) y + length);
        }
    }

    private void drawWindBands(Graphics2D g2, long now, double gust, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double travel = Math.floorMod((int) (now / 18), Math.max(1, getWidth() + 340)) - 170;
        for (int i = 0; i < 4; i++) {
            double y = getHeight() * (.18 + i * .2) + Math.sin(now / 1200.0 + i) * 20;
            double x = travel + i * 230;
            g2.draw(new java.awt.geom.CubicCurve2D.Double(x - 120, y + 10, x - 40, y - 18 * gust,
                    x + 55, y + 18 * gust, x + 145, y));
        }
    }

    private void drawLeaf(Graphics2D g2, double x, double y, double scale, double rotation, Color color, int alpha) {
        java.awt.geom.AffineTransform old = g2.getTransform();
        g2.translate(x, y); g2.rotate(rotation); g2.scale(scale, scale);
        java.awt.geom.Path2D leaf = new java.awt.geom.Path2D.Double();
        leaf.moveTo(-1, 9); leaf.curveTo(-11, 2, -9, -8, 0, -12);
        leaf.curveTo(10, -7, 11, 2, -1, 9); leaf.closePath();
        g2.setPaint(new GradientPaint(-6, -9, new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha),
                6, 8, new Color(color.darker().getRed(), color.darker().getGreen(), color.darker().getBlue(), alpha)));
        g2.fill(leaf);
        g2.setColor(new Color(92, 54, 25, alpha)); g2.setStroke(new BasicStroke(1.1f));
        g2.drawLine(0, -9, -1, 12);
        g2.setStroke(new BasicStroke(.65f));
        g2.drawLine(0, -4, -5, -7); g2.drawLine(0, 0, 6, -3); g2.drawLine(-1, 4, -6, 1);
        g2.setTransform(old);
    }

    private void drawSnowflake(Graphics2D g2, double x, double y, double radius, int alpha) {
        g2.setColor(new Color(246, 251, 255, alpha));
        g2.setStroke(new BasicStroke(radius > 3 ? 1.2f : .8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int arm = 0; arm < 3; arm++) {
            double angle = arm * Math.PI / 3;
            int dx = (int) Math.round(Math.cos(angle) * radius), dy = (int) Math.round(Math.sin(angle) * radius);
            g2.drawLine((int)x - dx, (int)y - dy, (int)x + dx, (int)y + dy);
        }
    }

    // ---- Hex drawing -------------------------------------------------------

    private void drawHex(Graphics2D g2, Hex hex, Player player) {
        HexCoordinate coord = hex.getCoordinate();
        Point2D center = hexToPixel(coord);

        // Cull off-screen hexes for performance.
        if (center.getX() < -hexSize * 2 || center.getX() > getWidth() + hexSize * 2
                || center.getY() < -hexSize * 2 || center.getY() > getHeight() + hexSize * 2) {
            return;
        }

        Polygon poly = createHexPolygon(center);

        if (!hex.getIsExplored()) {
            g2.setColor(FOG_COLOR);
            g2.fillPolygon(poly);
            g2.setColor(new Color(20, 22, 45));
            g2.drawPolygon(poly);
            return;
        }

        Color baseColor = getTerrainColor(hex.getTerrainType());
        if (!hex.isVisible()) {
            baseColor = desaturate(baseColor, 0.35f);
        }

        GradientPaint terrainGrad = new GradientPaint(
                (float) (center.getX() - hexSize * 0.5), (float) (center.getY() - hexSize * 0.8),
                baseColor.brighter(),
                (float) (center.getX() + hexSize * 0.4), (float) (center.getY() + hexSize * 0.8),
                baseColor.darker());
        g2.setPaint(terrainGrad);
        g2.fillPolygon(poly);

        if (hex.isVisible()) {
            drawTerrainDetails(g2, hex, center);
        }

        if (reachableHexes.contains(coord)) {
            g2.setColor(REACHABLE_COLOR);
            g2.fillPolygon(poly);
            g2.setColor(REACHABLE_BORDER);
            g2.setStroke(new BasicStroke(2f));
            g2.drawPolygon(poly);
        }

        if (player.isInTerritory(coord)) {
            g2.setColor(new Color(212, 175, 55, 20));
            g2.fillPolygon(poly);
        }

        if (!hex.isVisible()) {
            g2.setColor(EXPLORED_OVERLAY);
            g2.fillPolygon(poly);
        }

        g2.setColor(new Color(0, 0, 0, hex.isVisible() ? 80 : 60));
        g2.setStroke(new BasicStroke(hex.isVisible() ? 1.0f : 0.5f));
        g2.drawPolygon(poly);

        if (hex.isVisible() && hex.hasNaturalResource()) {
            drawNaturalResources(g2, hex, center);
        } else if (hex.isVisible() && hex.isDepleted()) {
            drawDepletedMarker(g2, hex, center);
        }

        if (hex.isVisible() && hex.isBlocked()) drawBlockedOverlay(g2, hex, center);
    }

    private void drawBlockedOverlay(Graphics2D g2, Hex hex, Point2D center) {
        Polygon p = createHexPolygon(center);
        g2.setColor(new Color(50, 45, 42, 115));
        g2.fillPolygon(p);
        g2.setColor(new Color(220, 215, 205, 190));
        g2.setStroke(new BasicStroke(3f));
        int cx = (int) center.getX(), cy = (int) center.getY();
        for (int i = -1; i <= 1; i++)
            g2.drawLine(cx - 20, cy + i * 9 - 7, cx + 20, cy + i * 9 + 7);
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.drawString("BLOCKED " + hex.getBlockedTurns(), cx - 25, cy + 29);
        g2.setStroke(new BasicStroke(1f));
    }

    /** Distinct rendering for a hex whose resource has been exhausted (empty forest/mine/farm). */
    private void drawDepletedMarker(Graphics2D g2, Hex hex, Point2D center) {
        // Grey out the tile slightly so it reads as spent.
        Polygon poly = createHexPolygon(center);
        g2.setColor(new Color(40, 40, 45, 90));
        g2.fillPolygon(poly);

        int cx = (int) center.getX();
        int cy = (int) center.getY();
        g2.setColor(new Color(120, 110, 100));
        switch (hex.getTerrainType()) {
            case FOREST: // tree stumps
                for (int[] t : new int[][]{{-10, 4}, {8, -2}, {2, 10}}) {
                    g2.fillRect(cx + t[0] - 3, cy + t[1] - 2, 6, 5);
                    g2.setColor(new Color(90, 70, 50));
                    g2.drawRect(cx + t[0] - 3, cy + t[1] - 2, 6, 5);
                    g2.setColor(new Color(120, 110, 100));
                }
                break;
            case MOUNTAIN: // collapsed mine entrance
                g2.setColor(new Color(60, 55, 50));
                g2.fillArc(cx - 9, cy - 4, 18, 16, 0, 180);
                g2.setColor(new Color(30, 28, 26));
                g2.fillArc(cx - 5, cy, 10, 8, 0, 180);
                break;
            default: // tilled-out empty field
                g2.setColor(new Color(110, 95, 70));
                for (int i = -1; i <= 1; i++) g2.drawLine(cx - 10, cy + i * 5, cx + 10, cy + i * 5);
                break;
        }
        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.setColor(new Color(220, 120, 120, 200));
        String t = "EMPTY";
        g2.drawString(t, cx - g2.getFontMetrics().stringWidth(t) / 2, cy + (int) (hexSize * 0.55));
    }

    private void drawTerrainDetails(Graphics2D g2, Hex hex, Point2D center) {
        int cx = (int) center.getX();
        int cy = (int) center.getY();
        switch (hex.getTerrainType()) {
            case FOREST: drawForestDetails(g2, cx, cy); break;
            case MOUNTAIN: drawMountainDetails(g2, cx, cy); break;
            case MOUNTAIN_RANGE: drawMountainRangeDetails(g2, cx, cy); break;
            case VOLCANO: drawVolcanoTerrain(g2, cx, cy); break;
            case SEA: drawSeaDetails(g2, cx, cy); break;
            case GRASSLAND: drawGrassDetails(g2, cx, cy); break;
            default: break;
        }
    }

    private void drawMountainRangeDetails(Graphics2D g2, int cx, int cy) {
        for (int i = -1; i <= 1; i++) {
            int x = cx + i * 15, top = cy - 25 + Math.abs(i) * 7;
            g2.setColor(new Color(72, 68, 70));
            g2.fillPolygon(new int[]{x, x - 18, x + 18}, new int[]{top, cy + 17, cy + 17}, 3);
            g2.setColor(new Color(235, 240, 245));
            g2.fillPolygon(new int[]{x, x - 6, x + 7}, new int[]{top, top + 12, top + 11}, 3);
        }
    }

    private void drawVolcanoTerrain(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(70, 58, 54));
        g2.fillPolygon(new int[]{cx, cx - 25, cx + 25}, new int[]{cy - 19, cy + 18, cy + 18}, 3);
        g2.setColor(new Color(35, 28, 27));
        g2.fillOval(cx - 10, cy - 22, 20, 9);
        g2.setColor(new Color(245, 76, 20));
        g2.fillOval(cx - 7, cy - 20, 14, 5);
        g2.drawLine(cx, cy - 16, cx + 9, cy + 14);
    }

    private void drawSeaDetails(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(185, 225, 245, 145));
        int drift = (int) (Math.sin(selectionPulse + cx * .02) * 3);
        for (int i = -1; i <= 1; i++) g2.drawArc(cx - 21 + i * 8 + drift, cy - 7 + i * 7, 25, 10, 10, 155);
    }

    private void drawForestDetails(Graphics2D g2, int cx, int cy) {
        int[][] trees = {{-12, 2}, {6, -8}, {10, 8}};
        for (int[] t : trees) {
            int tx = cx + t[0], ty = cy + t[1];
            int s = 9;
            g2.setColor(new Color(20, 100, 20));
            g2.fillPolygon(new int[]{tx, tx - s, tx + s}, new int[]{ty - s - 3, ty + 3, ty + 3}, 3);
            g2.setColor(new Color(32, 122, 32));
            g2.fillPolygon(new int[]{tx, tx - s + 3, tx + s - 3},
                    new int[]{ty - s - 8, ty - s + 2, ty - s + 2}, 3);
            g2.setColor(new Color(96, 58, 22));
            g2.fillRect(tx - 2, ty + 3, 4, 5);
        }
    }

    private void drawMountainDetails(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(100, 90, 75));
        g2.fillPolygon(new int[]{cx, cx - 20, cx + 20}, new int[]{cy - 18, cy + 12, cy + 12}, 3);
        g2.setColor(new Color(232, 236, 242));
        g2.fillPolygon(new int[]{cx, cx - 8, cx + 8}, new int[]{cy - 18, cy - 6, cy - 6}, 3);
        g2.setColor(new Color(88, 78, 64));
        g2.fillPolygon(new int[]{cx + 14, cx + 4, cx + 24}, new int[]{cy - 4, cy + 12, cy + 12}, 3);
        g2.setColor(new Color(240, 242, 248));
        g2.fillPolygon(new int[]{cx + 14, cx + 9, cx + 19}, new int[]{cy - 4, cy + 2, cy + 2}, 3);
    }

    private void drawGrassDetails(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(54, 162, 54, 130));
        g2.setStroke(new BasicStroke(1.5f));
        int[][] blades = {{-9, 5}, {0, -4}, {11, 3}, {-4, -9}, {7, -7}};
        for (int[] b : blades) {
            int bx = cx + b[0], by = cy + b[1];
            g2.drawLine(bx, by + 5, bx - 2, by);
            g2.drawLine(bx, by + 5, bx + 2, by);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawNaturalResources(Graphics2D g2, Hex hex, Point2D center) {
        int rx = (int) (center.getX() + hexSize * 0.38);
        int ry = (int) (center.getY() - hexSize * 0.55);
        for (Map.Entry<Constants.NaturalResourceType, Integer> e : hex.getNaturalResources().entrySet()) {
            drawResourceIcon(g2, e.getKey(), rx, ry, 10);
            ry += 14;
        }
    }

    private void drawResourceIcon(Graphics2D g2, Constants.NaturalResourceType type, int x, int y, int s) {
        switch (type) {
            case WOOD:
                g2.setColor(new Color(139, 90, 43));
                g2.fillRect(x, y, s, s / 2);
                g2.setColor(new Color(101, 67, 33));
                g2.drawRect(x, y, s, s / 2);
                break;
            case STONE:
                g2.setColor(new Color(150, 150, 155));
                g2.fill3DRect(x, y, s, s, true);
                break;
            case IRON:
                g2.setColor(new Color(185, 85, 85));
                g2.fillOval(x, y, s, s);
                g2.setColor(new Color(130, 40, 40));
                g2.drawOval(x, y, s, s);
                break;
            case WHEAT:
                g2.setColor(new Color(240, 200, 40));
                for (int i = 0; i < 3; i++) {
                    g2.drawLine(x + i * 3, y + s, x + i * 3, y);
                    g2.fillOval(x + i * 3 - 2, y - 2, 5, 5);
                }
                break;
            case RICE:
                g2.setColor(new Color(80, 200, 80));
                g2.fillOval(x, y + s / 2, s / 2, s / 2);
                g2.fillOval(x + s / 2, y, s / 2, s / 2);
                break;
            case COW:
                g2.setColor(new Color(180, 140, 80));
                g2.fillOval(x, y + s / 3, s, s * 2 / 3);
                g2.fillOval(x + s / 4, y, s / 2, s / 2);
                break;
            case SHEEP:
                g2.setColor(new Color(225, 225, 225));
                g2.fillOval(x, y, s, s * 2 / 3);
                g2.setColor(new Color(60, 60, 60));
                g2.fillOval(x + s / 4, y + s / 4, s / 3, s / 3);
                break;
            case FISH:
                g2.setColor(new Color(125, 215, 235));
                g2.fillOval(x, y + 2, s, s / 2);
                g2.fillPolygon(new int[]{x, x - 5, x - 5}, new int[]{y + 4, y, y + 8}, 3);
                g2.setColor(new Color(25, 85, 120)); g2.fillOval(x + s - 3, y + 3, 2, 2);
                break;
            default:
                break;
        }
    }

    // ---- Buildings ---------------------------------------------------------

    private void drawBuilding(Graphics2D g2, Building b, Point2D center, boolean visible) {
        int cx = (int) center.getX();
        int cy = (int) center.getY();
        switch (b.getType()) {
            case TOWN_HALL: drawTownHall(g2, cx, cy, visible); break;
            case LUMBER_MILL: drawLumberMill(g2, cx, cy, visible); break;
            case FARM: drawFarm(g2, cx, cy, visible); break;
            case STONE_MINE: drawStoneMine(g2, cx, cy, visible); break;
            case IRON_MINE: drawIronMine(g2, cx, cy, visible); break;
            case STABLE: drawStable(g2, cx, cy, visible); break;
            case TOWNSHIP: drawTownship(g2, cx, cy, visible); break;
            case DOCK: drawDock(g2, cx, cy, visible); break;
            case MONUMENT: drawMonument(g2, cx, cy, visible); break;
            case BAZAAR: drawBazaar(g2, cx, cy, visible); break;
            default: break;
        }
        if (visible) {
            int wc = b.getWorkerCount();
            for (int i = 0; i < wc; i++) {
                g2.setColor(new Color(180, 120, 255));
                g2.fillOval(cx - 15 + i * 8, cy + 18, 6, 6);
                g2.setColor(Color.WHITE);
                g2.drawOval(cx - 15 + i * 8, cy + 18, 6, 6);
            }
            drawHealthBar(g2, cx, cy + 27, b.getCurrentHp(), b.getMaxHp());
            if (b.isRuined()) drawRuinedOverlay(g2, cx, cy);
        }
    }

    private void drawDock(Graphics2D g2, int cx, int cy, boolean vis) {
        g2.setColor(vis ? new Color(155, 104, 55) : new Color(92, 70, 48));
        g2.setStroke(new BasicStroke(4f));
        for (int i = -1; i <= 1; i++) g2.drawLine(cx - 16, cy + i * 6, cx + 17, cy + i * 6);
        g2.setColor(vis ? new Color(220, 190, 125) : new Color(125, 110, 85));
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(cx - 16, cy - 10, cx - 16, cy + 14);
        g2.drawLine(cx + 17, cy - 10, cx + 17, cy + 14);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawMonument(Graphics2D g2, int cx, int cy, boolean vis) {
        Color stone = vis ? new Color(214, 208, 184) : new Color(125, 125, 116);
        g2.setColor(new Color(0, 0, 0, 85)); g2.fillOval(cx - 15, cy + 12, 30, 8);
        java.awt.geom.Path2D obelisk = new java.awt.geom.Path2D.Double();
        obelisk.moveTo(cx, cy - 22); obelisk.lineTo(cx + 7, cy - 12);
        obelisk.lineTo(cx + 6, cy + 13); obelisk.lineTo(cx - 6, cy + 13);
        obelisk.lineTo(cx - 7, cy - 12); obelisk.closePath();
        g2.setPaint(new GradientPaint(cx - 7, cy, stone.brighter(), cx + 7, cy, stone.darker()));
        g2.fill(obelisk); g2.setColor(stone.darker()); g2.draw(obelisk);
        g2.fillRoundRect(cx - 12, cy + 12, 24, 6, 3, 3);
        if (vis) { g2.setColor(new Color(255, 215, 90, 90)); g2.drawOval(cx - 13, cy - 26, 26, 26); }
    }

    private void drawBazaar(Graphics2D g2, int cx, int cy, boolean vis) {
        Color red = vis ? new Color(194, 63, 66) : new Color(112, 63, 66);
        Color cream = vis ? new Color(239, 211, 148) : new Color(145, 132, 104);
        for (int i = 0; i < 5; i++) { g2.setColor(i % 2 == 0 ? red : cream); g2.fillRect(cx - 18 + i * 7, cy - 15, 7, 10); }
        g2.setColor(new Color(125, 80, 43)); g2.fillRoundRect(cx - 16, cy - 5, 32, 20, 4, 4);
        g2.setColor(cream); g2.fillOval(cx - 9, cy + 1, 7, 7); g2.fillOval(cx + 3, cy, 8, 8);
        g2.setColor(new Color(75, 45, 28)); g2.drawRoundRect(cx - 16, cy - 5, 32, 20, 4, 4);
    }

    private void drawTradingPost(Graphics2D g2, Point2D center, boolean vis) {
        int cx = (int) center.getX(), cy = (int) center.getY();
        Color wood = vis ? new Color(154, 111, 66) : new Color(92, 76, 58);
        g2.setColor(new Color(0, 0, 0, 80)); g2.fillOval(cx - 18, cy + 10, 36, 9);
        g2.setColor(wood); g2.fillRoundRect(cx - 15, cy - 7, 30, 20, 5, 5);
        g2.setColor(new Color(70, 110, 126)); g2.fillRect(cx - 17, cy - 14, 34, 8);
        g2.setColor(new Color(215, 205, 172)); g2.setFont(new Font("Georgia", Font.BOLD, 9));
        g2.drawString("POST", cx - 12, cy + 6);
        g2.setColor(new Color(90, 90, 95)); g2.drawRoundRect(cx - 15, cy - 7, 30, 20, 5, 5);
    }

    private void drawTribeCamp(Graphics2D g2, Tribe tribe, Point2D center, boolean vis) {
        int cx = (int) center.getX(), cy = (int) center.getY();
        if (tribe.isOutpost()) {
            drawOutpost(g2, cx, cy, vis);
            return;
        }
        Color accent;
        switch (tribe.getType()) {
            case FARMER: accent = new Color(112, 181, 88); break;
            case WARRIOR: accent = new Color(196, 70, 62); break;
            case MERCHANT: accent = new Color(215, 166, 66); break;
            case MOUNTAIN: accent = new Color(155, 163, 176); break;
            case COASTAL: accent = new Color(70, 157, 190); break;
            default: accent = Color.GRAY;
        }
        if (!vis) accent = accent.darker();
        g2.setColor(new Color(0, 0, 0, 100)); g2.fillOval(cx - 21, cy + 12, 42, 9);
        java.awt.geom.Path2D tent = new java.awt.geom.Path2D.Double();
        tent.moveTo(cx, cy - 22); tent.lineTo(cx + 20, cy + 14); tent.lineTo(cx - 20, cy + 14); tent.closePath();
        g2.setPaint(new GradientPaint(cx - 18, cy, accent.brighter(), cx + 18, cy, accent.darker()));
        g2.fill(tent); g2.setColor(new Color(58, 42, 36)); g2.setStroke(new BasicStroke(2f)); g2.draw(tent);
        g2.drawLine(cx, cy - 20, cx, cy + 14);
        g2.setColor(new Color(35, 27, 25)); g2.fillArc(cx - 6, cy + 2, 12, 18, 0, 180);
        g2.setColor(accent.brighter()); g2.fillOval(cx - 3, cy - 14, 6, 6);
        drawHealthBar(g2, cx, cy + 21, tribe.getCurrentHp(), tribe.getMaxHp());
    }

    private void drawOutpost(Graphics2D g2, int cx, int cy, boolean vis) {
        Color timber = vis ? new Color(176, 131, 75) : new Color(98, 80, 59);
        g2.setColor(new Color(0, 0, 0, 95));
        g2.fillOval(cx - 19, cy + 12, 38, 8);
        g2.setColor(timber);
        g2.fillRect(cx - 15, cy - 9, 30, 22);
        g2.setColor(timber.darker());
        g2.drawRect(cx - 15, cy - 9, 30, 22);
        g2.drawLine(cx, cy - 22, cx, cy + 13);
        g2.setColor(new Color(212, 175, 55));
        g2.fillPolygon(new int[]{cx, cx + 14, cx}, new int[]{cy - 22, cy - 16, cy - 10}, 3);
        g2.setColor(new Color(35, 28, 24));
        g2.fillRect(cx - 4, cy + 2, 8, 11);
    }

    private void drawTribeMilitaryUnit(Graphics2D g2, TribeMilitaryUnit unit, Point2D center) {
        int cx = (int) center.getX(), cy = (int) center.getY();
        Color color = switch (unit.getMilitaryUnitType()) {
            case SWORDSMAN -> new Color(204, 86, 72);
            case ARCHER -> new Color(112, 175, 91);
            case CAVALRY -> new Color(190, 145, 64);
            case CATAPULT -> Color.GRAY;
        };
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillOval(cx - 10, cy + 7, 20, 6);
        g2.setColor(color);
        g2.fillOval(cx - 8, cy - 8, 16, 16);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.drawString(unit.getMilitaryUnitType().name().substring(0, 1), cx - 3, cy + 4);
        g2.setColor(color.darker());
        g2.drawOval(cx - 8, cy - 8, 16, 16);
    }

    private void drawRuinedOverlay(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(30, 25, 24, 145));
        g2.fillOval(cx - 19, cy - 18, 38, 36);
        g2.setColor(new Color(230, 95, 70, 220));
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(cx - 14, cy - 13, cx + 14, cy + 13);
        g2.drawLine(cx + 14, cy - 13, cx - 14, cy + 13);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawHealthBar(Graphics2D g2, int cx, int y, int hp, int maxHp) {
        int w = 36, h = 6;
        float pct = maxHp > 0 ? Math.max(0f, Math.min(1f, hp / (float) maxHp)) : 0f;
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRoundRect(cx - w / 2 + 1, y + 2, w, h, 6, 6);
        g2.setColor(new Color(18, 18, 25, 225));
        g2.fillRoundRect(cx - w / 2, y, w, h, 6, 6);
        Color fill = pct > .55f ? new Color(70, 205, 85) : pct > .25f
                ? new Color(235, 175, 45) : new Color(225, 65, 60);
        g2.setPaint(new GradientPaint(cx - w / 2, y, fill.brighter(), cx + w / 2, y, fill.darker()));
        g2.fillRoundRect(cx - w / 2, y, (int) (w * pct), h, 6, 6);
        g2.setColor(new Color(245, 245, 245, 115));
        g2.drawRoundRect(cx - w / 2, y, w, h, 6, 6);
    }

    private void drawTownHall(Graphics2D g2, int cx, int cy, boolean vis) {
        int level = controller.getGameState().getTownHall().getLevel().getLevelNumber();
        Color wall = vis ? new Color(205, 184, 132) : new Color(126, 116, 91);
        g2.setColor(new Color(0,0,0,90)); g2.fillOval(cx-24,cy+13,48,10);
        if (level == 1) {
            java.awt.geom.Path2D tent = new java.awt.geom.Path2D.Double();
            tent.moveTo(cx,cy-22);tent.lineTo(cx+21,cy+15);tent.lineTo(cx-21,cy+15);tent.closePath();
            g2.setPaint(new GradientPaint(cx-18,cy,new Color(188,138,77),cx+18,cy,new Color(115,70,42)));
            g2.fill(tent);g2.setColor(new Color(70,45,31));g2.draw(tent);g2.drawLine(cx,cy-20,cx,cy+15);
            g2.fillArc(cx-5,cy+3,10,15,0,180);
        } else {
            int s=level==2?15:18; g2.setColor(wall);g2.fillRoundRect(cx-s,cy-10,s*2,27,4,4);
            g2.setColor(new Color(126,72,45));
            g2.fillPolygon(new int[]{cx-s-4,cx,cx+s+4},new int[]{cy-10,cy-26-(level-2)*4,cy-10},3);
            g2.setColor(new Color(77,61,49));g2.fillRoundRect(cx-4,cy+3,8,14,4,4);
            if(level==3){g2.setColor(wall.brighter());g2.fillRect(cx-22,cy-5,7,22);g2.fillRect(cx+15,cy-5,7,22);
                for(int x:new int[]{cx-22,cx-15,cx+15,cx+22})g2.fillRect(x-2,cy-10,5,7);}
            g2.setColor(new Color(235,193,66));g2.fillOval(cx-3,cy-20-(level-2)*4,6,6);
        }
    }

    private void drawLumberMill(Graphics2D g2, int cx, int cy, boolean vis) {
        Color wood=vis?new Color(146,91,48):new Color(92,66,43);
        g2.setColor(wood);g2.fillRoundRect(cx-15,cy-8,24,22,3,3);
        g2.setColor(wood.darker());g2.fillPolygon(new int[]{cx-18,cx-3,cx+12},new int[]{cy-8,cy-20,cy-8},3);
        g2.setColor(vis?new Color(204,207,202):new Color(130,132,130));g2.fillOval(cx+2,cy-11,18,18);
        g2.setColor(new Color(90,94,92));g2.drawOval(cx+2,cy-11,18,18);
        for(int i=0;i<8;i++){double a=i*Math.PI/4;g2.drawLine(cx+11,cy-2,cx+11+(int)(Math.cos(a)*8),cy-2+(int)(Math.sin(a)*8));}
        g2.setColor(new Color(103,61,31));for(int i=0;i<3;i++)g2.fillRoundRect(cx-20+i*5,cy+12-i*2,18,5,4,4);
    }

    private void drawFarm(Graphics2D g2, int cx, int cy, boolean vis) {
        Color soil = vis ? new Color(132, 86, 47) : new Color(82, 61, 39);
        Color crop = vis ? new Color(142, 190, 69) : new Color(81, 116, 56);
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(cx - 22, cy + 12, 44, 8);
        java.awt.geom.Path2D field = new java.awt.geom.Path2D.Double();
        field.moveTo(cx - 20, cy - 12); field.lineTo(cx + 16, cy - 9);
        field.lineTo(cx + 21, cy + 14); field.lineTo(cx - 17, cy + 12); field.closePath();
        g2.setPaint(new GradientPaint(cx, cy - 12, soil.brighter(), cx, cy + 15, soil.darker()));
        g2.fill(field); g2.setColor(soil.darker()); g2.draw(field);
        g2.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 4; i++) {
            int y = cy - 7 + i * 6;
            g2.setColor(new Color(72, 49, 33, 170)); g2.drawLine(cx - 15, y + 2, cx + 16, y + 4);
            g2.setColor(crop); g2.drawLine(cx - 14, y, cx + 15, y + 2);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawStoneMine(Graphics2D g2, int cx, int cy, boolean vis) {
        drawMine(g2,cx,cy,vis,false);
    }

    private void drawIronMine(Graphics2D g2, int cx, int cy, boolean vis) {
        drawMine(g2,cx,cy,vis,true);
    }

    private void drawMine(Graphics2D g2,int cx,int cy,boolean vis,boolean iron){
        Color rock=vis?new Color(135,132,130):new Color(87,86,88);
        g2.setColor(rock);g2.fillPolygon(new int[]{cx-23,cx-10,cx,cx+13,cx+23},new int[]{cy+16,cy-10,cy-20,cy-8,cy+16},5);
        g2.setColor(new Color(45,40,39));g2.fillArc(cx-10,cy-2,20,20,0,180);
        g2.setColor(new Color(111,72,42));g2.setStroke(new BasicStroke(3f));g2.drawLine(cx-10,cy+7,cx-10,cy-1);g2.drawArc(cx-10,cy-2,20,20,0,180);g2.drawLine(cx+10,cy+7,cx+10,cy-1);
        g2.setColor(iron?new Color(205,83,61):new Color(208,205,192));
        for(int i=0;i<3;i++)g2.fillPolygon(new int[]{cx+11+i*4,cx+14+i*4,cx+12+i*4},new int[]{cy+7-i*4,cy+10-i*4,cy+12-i*4},3);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawStable(Graphics2D g2, int cx, int cy, boolean vis) {
        Color barn=vis?new Color(164,91,59):new Color(101,69,52);
        g2.setColor(barn);g2.fillRoundRect(cx-17,cy-7,34,23,3,3);
        g2.setColor(new Color(91,54,37));g2.fillPolygon(new int[]{cx-21,cx,cx+21},new int[]{cy-7,cy-23,cy-7},3);
        g2.setColor(new Color(62,44,35));g2.fillRoundRect(cx-6,cy+1,12,15,6,6);
        g2.setColor(new Color(229,205,153));g2.setStroke(new BasicStroke(2f));g2.drawArc(cx-4,cy+4,8,9,0,180);g2.drawLine(cx-4,cy+8,cx-4,cy+14);g2.drawLine(cx+4,cy+8,cx+4,cy+14);g2.setStroke(new BasicStroke(1f));
    }

    private void drawTownship(Graphics2D g2, int cx, int cy, boolean vis) {
        Color plaster = vis ? new Color(211, 188, 126) : new Color(126, 116, 83);
        Color roof = vis ? new Color(151, 73, 54) : new Color(91, 61, 52);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(cx - 25, cy + 11, 50, 9);
        int[][] homes = {{-15, 0, 11, 14}, {-2, -7, 14, 21}, {13, 2, 10, 12}};
        for (int[] home : homes) {
            int x = cx + home[0], y = cy + home[1], w = home[2], h = home[3];
            g2.setPaint(new GradientPaint(x, y, plaster.brighter(), x + w, y + h, plaster.darker()));
            g2.fillRoundRect(x - w / 2, y - h / 2, w, h, 2, 2);
            g2.setColor(roof);
            g2.fillPolygon(new int[]{x - w / 2 - 2, x, x + w / 2 + 2},
                    new int[]{y - h / 2, y - h / 2 - 7, y - h / 2}, 3);
            g2.setColor(new Color(68, 49, 39));
            g2.fillRoundRect(x - 2, y + h / 2 - 7, 4, 7, 2, 2);
            if (vis && w > 10) {
                g2.setColor(new Color(244, 202, 87));
                g2.fillRect(x + 3, y - 1, 3, 3);
            }
        }
    }

    // ---- Units -------------------------------------------------------------

    private void drawUnit(Graphics2D g2, Unit u, Point2D center) {
        int cx = (int) center.getX();
        int cy = (int) center.getY() + 6;
        int r = 14;

        Hex unitHex = controller.getGameState().getMap().getHex(u.getPosition());
        if (unitHex != null && unitHex.getTerrainType() == Constants.TerrainType.SEA
                && !(u instanceof model.disaster.Bear)) drawBoat(g2, cx, cy);

        g2.setColor(new Color(0, 0, 0, 110));
        g2.fillOval(cx - r + 2, cy - r + 4, r * 2, r * 2);

        Color unitColor = getUnitColor(u.getUnitType());
        boolean hasAP = u.getCurrentAP() > 0;
        if (!hasAP) unitColor = desaturate(unitColor, 0.45f);

        if (u instanceof MilitaryUnit) {
            drawMilitaryToken(g2, ((MilitaryUnit) u).getMilitaryUnitType(), cx, cy, hasAP);
        } else {
            GradientPaint unitGrad = new GradientPaint(cx - r, cy - r, unitColor.brighter(),
                    cx + r, cy + r, unitColor.darker());
            g2.setPaint(unitGrad);
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);

            g2.setColor(hasAP ? Color.WHITE : new Color(150, 150, 150));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            String icon = getUnitIcon(u.getUnitType());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(icon, cx - fm.stringWidth(icon) / 2,
                    cy + fm.getAscent() / 2 - 2);
        }

        int barW = 26, barH = 4;
        g2.setColor(new Color(30, 30, 30, 190));
        g2.fillRect(cx - barW / 2, cy + r + 2, barW, barH);
        float apPct = u.getMaxAP() > 0 ? (float) u.getCurrentAP() / u.getMaxAP() : 0f;
        Color apColor = apPct > 0.5f ? new Color(80, 200, 80)
                : (apPct > 0.2f ? new Color(230, 180, 30) : new Color(220, 60, 60));
        g2.setColor(apColor);
        g2.fillRect(cx - barW / 2, cy + r + 2, (int) (barW * apPct), barH);

        if (u instanceof MilitaryUnit) {
            MilitaryUnit military = (MilitaryUnit) u;
            drawHealthBar(g2, cx, cy + r + 9, military.getCurrentHp(), military.getMaxHp());
        }

        if (u == controller.getSelectedUnit()) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(GOLD);
            String name = u instanceof MilitaryUnit
                    ? ((MilitaryUnit) u).getMilitaryUnitType().name().replace("_", " ")
                    : u.getUnitType().name().replace("_", " ");
            g2.drawString(name, cx - g2.getFontMetrics().stringWidth(name) / 2, cy - r - 4);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawMilitaryToken(Graphics2D g2, MilitaryUnitType type,
                                   int cx, int cy, boolean hasAP) {
        Color base = switch (type) {
            case SWORDSMAN -> new Color(185, 57, 52);
            case ARCHER -> new Color(67, 139, 83);
            case CAVALRY -> new Color(56, 105, 169);
            case CATAPULT -> new Color(151, 101, 45);
        };
        if (!hasAP) base = desaturate(base, 0.45f);
        g2.setPaint(new GradientPaint(cx - 14, cy - 14, base.brighter(),
                cx + 14, cy + 14, base.darker()));

        switch (type) {
            case SWORDSMAN -> {
                Polygon shield = new Polygon(
                        new int[]{cx - 13, cx + 13, cx + 10, cx, cx - 10},
                        new int[]{cy - 12, cy - 12, cy + 7, cy + 15, cy + 7}, 5);
                g2.fillPolygon(shield);
                g2.setColor(new Color(242, 235, 210));
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - 7, cy + 8, cx + 8, cy - 8);
                g2.setColor(new Color(84, 54, 35));
                g2.drawLine(cx - 9, cy - 1, cx + 2, cy + 10);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(Color.WHITE);
                g2.drawPolygon(shield);
            }
            case ARCHER -> {
                g2.fillOval(cx - 14, cy - 14, 28, 28);
                g2.setColor(new Color(237, 218, 156));
                g2.setStroke(new BasicStroke(2.4f));
                g2.drawArc(cx - 10, cy - 12, 18, 25, -75, 150);
                g2.drawLine(cx - 2, cy - 10, cx - 2, cy + 10);
                g2.setColor(new Color(245, 245, 232));
                g2.drawLine(cx - 9, cy, cx + 10, cy);
                g2.fillPolygon(new int[]{cx + 10, cx + 5, cx + 5},
                        new int[]{cy, cy - 3, cy + 3}, 3);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(Color.WHITE);
                g2.drawOval(cx - 14, cy - 14, 28, 28);
            }
            case CAVALRY -> {
                Polygon banner = new Polygon(
                        new int[]{cx - 14, cx + 11, cx + 14, cx, cx - 14},
                        new int[]{cy - 12, cy - 12, cy + 4, cy + 14, cy + 4}, 5);
                g2.fillPolygon(banner);
                g2.setColor(new Color(232, 224, 194));
                Polygon horse = new Polygon(
                        new int[]{cx - 7, cx - 2, cx + 2, cx + 8, cx + 6, cx, cx - 5},
                        new int[]{cy + 8, cy - 8, cy - 12, cy - 5, cy + 7, cy + 10, cy + 4}, 7);
                g2.fillPolygon(horse);
                g2.setColor(new Color(55, 46, 42));
                g2.fillOval(cx + 3, cy - 5, 3, 3);
                g2.setColor(new Color(246, 210, 91));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(cx - 11, cy + 10, cx + 11, cy - 11);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawPolygon(banner);
            }
            case CATAPULT -> {
                g2.fillRoundRect(cx - 15, cy - 13, 30, 27, 5, 5);
                g2.setColor(new Color(71, 45, 28));
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(cx - 9, cy + 5, cx + 9, cy + 5);
                g2.drawLine(cx - 7, cy + 5, cx + 3, cy - 7);
                g2.drawLine(cx + 3, cy - 7, cx + 11, cy - 10);
                g2.setColor(new Color(211, 190, 143));
                g2.fillOval(cx + 8, cy - 13, 7, 7);
                g2.setColor(new Color(48, 38, 31));
                g2.fillOval(cx - 12, cy + 7, 8, 8);
                g2.fillOval(cx + 5, cy + 7, 8, 8);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(Color.WHITE);
                g2.drawRoundRect(cx - 15, cy - 13, 30, 27, 5, 5);
            }
        }
    }

    private void drawBoat(Graphics2D g2, int cx, int cy) {
        java.awt.geom.Path2D hull = new java.awt.geom.Path2D.Double();
        hull.moveTo(cx - 24, cy + 8); hull.quadTo(cx, cy + 22, cx + 24, cy + 8);
        hull.lineTo(cx + 18, cy + 17); hull.quadTo(cx, cy + 29, cx - 18, cy + 17); hull.closePath();
        g2.setColor(new Color(75, 44, 25, 220)); g2.fill(hull); g2.setColor(new Color(222, 163, 88)); g2.draw(hull);
        g2.setColor(new Color(81, 55, 35)); g2.fillRect(cx - 2, cy - 23, 3, 35);
        java.awt.geom.Path2D sail = new java.awt.geom.Path2D.Double();
        sail.moveTo(cx + 1, cy - 21); sail.lineTo(cx + 17, cy + 2); sail.lineTo(cx + 1, cy + 2); sail.closePath();
        g2.setColor(new Color(233, 222, 188, 225)); g2.fill(sail); g2.setColor(new Color(126, 79, 52)); g2.draw(sail);
    }

    private Color getUnitColor(Constants.UnitType type) {
        switch (type) {
            case EXPLORER: return new Color(20, 170, 200);
            case BUILDER: return new Color(220, 140, 20);
            case WORKER: return new Color(150, 50, 205);
            case BORDER_EXPANDER: return new Color(40, 110, 225);
            case MILITARY: return new Color(185, 55, 50);
            case BEAR: return new Color(105, 65, 35);
            default: return Color.GRAY;
        }
    }

    private String getUnitIcon(Constants.UnitType type) {
        switch (type) {
            case EXPLORER: return "E";
            case BUILDER: return "B";
            case WORKER: return "W";
            case BORDER_EXPANDER: return "X";
            case MILITARY: return "⚔";
            case BEAR: return "BR";
            default: return "?";
        }
    }

    // ---- Overlays ----------------------------------------------------------

    private void drawTerritoryBorders(Graphics2D g2, Player player, GameMap map) {
        g2.setColor(TERRITORY_BORDER);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (HexCoordinate coord : player.getTerritory()) {
            Hex hex = map.getHex(coord);
            if (hex == null || !hex.getIsExplored()) continue;
            Point2D center = hexToPixel(coord);
            List<HexCoordinate> neighbors = coord.findNeighbours();
            Polygon poly = createHexPolygon(center);
            for (int i = 0; i < 6; i++) {
                if (!player.isInTerritory(neighbors.get(i))) {
                    int j = (i + 1) % 6;
                    g2.drawLine(poly.xpoints[i], poly.ypoints[i], poly.xpoints[j], poly.ypoints[j]);
                }
            }
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawSelectionGlow(Graphics2D g2, Point2D center) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(selectionPulse);
        int r = (int) (hexSize * 0.72);
        float alpha = 0.3f + 0.34f * pulse;
        g2.setColor(new Color(1f, .84f, .12f, .07f + .08f * pulse));
        g2.fillOval((int) (center.getX() - r - 7), (int) (center.getY() - r - 7),
                (r + 7) * 2, (r + 7) * 2);
        g2.setColor(new Color(1f, 0.88f, 0.2f, Math.min(1f, alpha)));
        g2.setStroke(new BasicStroke(2.2f + pulse * 1.6f));
        g2.drawOval((int) (center.getX() - r), (int) (center.getY() - r), r * 2, r * 2);
        g2.setColor(new Color(255, 239, 130, 180));
        g2.setStroke(new BasicStroke(1.2f));
        int cx = (int) center.getX(), cy = (int) center.getY();
        g2.drawLine(cx - r - 5, cy, cx - r + 3, cy);
        g2.drawLine(cx + r - 3, cy, cx + r + 5, cy);
        g2.drawLine(cx, cy - r - 5, cx, cy - r + 3);
        g2.drawLine(cx, cy + r - 3, cx, cy + r + 5);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawHexHighlight(Graphics2D g2, Point2D center, Color color) {
        g2.setColor(color);
        g2.fillPolygon(createHexPolygon(center));
    }

    // ---- Coordinate transforms --------------------------------------------

    private Point2D hexToPixelRaw(HexCoordinate coord) {
        double q = coord.getQ();
        double r = coord.getR();
        double x = hexSize * 1.5 * q;
        double y = hexSize * Math.sqrt(3) * (r + 0.5 * (coord.getQ() & 1));
        return new Point2D.Double(x, y);
    }

    public Point2D hexToPixel(HexCoordinate coord) {
        Point2D raw = hexToPixelRaw(coord);
        return new Point2D.Double(raw.getX() + offsetX, raw.getY() + offsetY);
    }

    /** Screen position of a unit using its eased animation position (falls back to its hex). */
    private Point2D animatedCenter(Unit u) {
        Point2D raw = animatedRaw.get(u);
        if (raw == null) raw = hexToPixelRaw(u.getPosition());
        return new Point2D.Double(raw.getX() + offsetX, raw.getY() + offsetY);
    }

    public HexCoordinate pixelToHex(int px, int py) {
        GameMap map = controller.getGameState().getMap();
        HexCoordinate best = null;
        double bestDist = Double.MAX_VALUE;
        for (Hex hex : map.getAllHexes()) {
            Point2D center = hexToPixel(hex.getCoordinate());
            double dist = Math.hypot(px - center.getX(), py - center.getY());
            if (dist < bestDist) {
                bestDist = dist;
                best = hex.getCoordinate();
            }
        }
        return (bestDist < hexSize * 1.1) ? best : null;
    }

    private Polygon createHexPolygon(Point2D center) {
        int[] xPoints = new int[6];
        int[] yPoints = new int[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 180.0 * (60 * i);
            xPoints[i] = (int) (center.getX() + hexSize * Math.cos(angle));
            yPoints[i] = (int) (center.getY() + hexSize * Math.sin(angle));
        }
        return new Polygon(xPoints, yPoints, 6);
    }

    private Color getTerrainColor(Constants.TerrainType terrain) {
        switch (terrain) {
            case PLAIN: return PLAIN_COLOR;
            case GRASSLAND: return GRASSLAND_COLOR;
            case FOREST: return FOREST_COLOR;
            case MOUNTAIN: return MOUNTAIN_COLOR;
            case MOUNTAIN_RANGE: return new Color(76, 72, 78);
            case VOLCANO: return new Color(92, 57, 46);
            case SEA: return new Color(40, 112, 166);
            default: return PLAIN_COLOR;
        }
    }

    private Color desaturate(Color c, float factor) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1] * factor, hsb[2] * 0.6f);
    }

    // ---- Mouse handling ----------------------------------------------------

    @Override
    public void mouseClicked(MouseEvent e) {
        if (controller.getGameState() == null || dragging) return;
        if (SwingUtilities.isLeftMouseButton(e)) {
            HexCoordinate coord = pixelToHex(e.getX(), e.getY());
            if (coord != null) {
                controller.onHexClicked(coord);
                gamePanel.repaintAll();
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            controller.deselectUnit();
            gamePanel.repaintAll();
            return;
        }
        if (SwingUtilities.isLeftMouseButton(e) && handleMinimapClick(e.getX(), e.getY())) {
            dragging = true; // suppress the click-as-select that would otherwise follow
            return;
        }
        dragStartX = e.getX();
        dragStartY = e.getY();
        dragging = false;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Reset on a short delay-free basis; click handler checks the flag.
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int dx = e.getX() - dragStartX;
        int dy = e.getY() - dragStartY;
        if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
            dragging = true;
            offsetX += dx;
            offsetY += dy;
            dragStartX = e.getX();
            dragStartY = e.getY();
            repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (controller.getGameState() == null) return;
        HexCoordinate coord = pixelToHex(e.getX(), e.getY());
        if (!Objects.equals(coord, hoverHex)) {
            hoverHex = coord;
            setToolTipText(buildTooltip(coord));
            repaint();
        }
    }

    /** Rich hover tooltip: terrain, resource (and remaining), building and stationed workers. */
    private String buildTooltip(HexCoordinate coord) {
        if (coord == null) return null;
        GameState gs = controller.getGameState();
        Hex h = gs.getMap().getHex(coord);
        if (h == null) return null;
        if (!h.getIsExplored()) return "Unexplored";

        Player player = gs.getPlayer();
        StringBuilder sb = new StringBuilder("<html>");
        sb.append("<b>Terrain:</b> ").append(h.getTerrainType().name());
        if (h.hasNaturalResource()) {
            sb.append("<br><b>Resource:</b> ");
            boolean first = true;
            for (Map.Entry<Constants.NaturalResourceType, Integer> en : h.getNaturalResources().entrySet()) {
                if (!first) sb.append(", ");
                sb.append(en.getKey().name()).append(" (").append(en.getValue()).append(" left)");
                first = false;
            }
        } else if (h.isDepleted()) {
            sb.append("<br><b>Resource:</b> depleted (empty)");
        } else {
            sb.append("<br><b>Resource:</b> none");
        }
        Building b = player.getBuildingAt(coord);
        if (b != null) {
            sb.append("<br><b>Building:</b> ").append(b.getType().name().replace("_", " "));
            if (b.isRuined()) sb.append(" (RUINED)");
            else if (b.getWorkerCap() > 0) sb.append(" — workers ").append(b.getWorkerCount()).append("/").append(b.getWorkerCap());
        }
        Unit u = player.getUnitAt(coord);
        if (u != null) {
            sb.append("<br><b>Unit:</b> ").append(u.getUnitType().name().replace("_", " "))
              .append(" (AP ").append(u.getCurrentAP()).append("/").append(u.getMaxAP()).append(")");
        }
        if (player.isInTerritory(coord)) sb.append("<br><i>Your territory</i>");
        sb.append("</html>");
        return sb.toString();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double oldSize = hexSize;
        double factor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
        hexSize = Math.max(22, Math.min(82, hexSize * factor));
        // Zoom toward the cursor.
        double scale = hexSize / oldSize;
        offsetX = e.getX() - (e.getX() - offsetX) * scale;
        offsetY = e.getY() - (e.getY() - offsetY) * scale;
        repaint();
    }

    @Override public void mouseEntered(MouseEvent e) {}

    @Override public void mouseExited(MouseEvent e) {
        hoverHex = null;
        repaint();
    }
}
