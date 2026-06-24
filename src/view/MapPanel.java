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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    private static final Color PLAINS_COLOR = new Color(198, 178, 112);
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
    private final Timer pulseTimer;

    public MapPanel(GameController controller, GamePanel gamePanel) {
        this.controller = controller;
        this.gamePanel = gamePanel;
        setBackground(FOG_COLOR);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        pulseTimer = new Timer(40, e -> {
            selectionPulse += 0.08f;
            if (selectionPulse > 2 * Math.PI) selectionPulse -= (float) (2 * Math.PI);
            if (controller.getSelectedUnit() != null) repaint();
        });
        pulseTimer.start();
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

        drawTerritoryBorders(g2, player, map);

        for (Building b : player.getBuildings()) {
            Hex hex = map.getHex(b.getPosition());
            if (hex != null && hex.getIsExplored()) {
                drawBuilding(g2, b, hexToPixel(b.getPosition()), hex.isVisible());
            }
        }

        for (Unit u : player.getUnits()) {
            if (!u.isAlive()) continue;
            Hex hex = map.getHex(u.getPosition());
            if (hex != null && hex.isVisible()) {
                drawUnit(g2, u, hexToPixel(u.getPosition()));
            }
        }

        if (controller.getSelectedUnit() != null) {
            drawSelectionGlow(g2, hexToPixel(controller.getSelectedUnit().getPosition()));
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

        // Controls hint.
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(new Color(150, 150, 175, 170));
        g2.drawString("Drag to pan  •  Scroll to zoom  •  Right-click / ESC to deselect",
                16, getHeight() - 6);

        g2.dispose();
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
        }
    }

    private void drawTerrainDetails(Graphics2D g2, Hex hex, Point2D center) {
        int cx = (int) center.getX();
        int cy = (int) center.getY();
        switch (hex.getTerrainType()) {
            case FOREST: drawForestDetails(g2, cx, cy); break;
            case MOUNTAIN: drawMountainDetails(g2, cx, cy); break;
            case GRASSLAND: drawGrassDetails(g2, cx, cy); break;
            default: break;
        }
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
        }
    }

    private void drawTownHall(Graphics2D g2, int cx, int cy, boolean vis) {
        int s = 14;
        g2.setColor(vis ? new Color(212, 175, 55) : new Color(150, 120, 30));
        g2.fillRect(cx - s, cy - s, s * 2, s * 2);
        g2.setColor(new Color(180, 140, 30));
        g2.fillRect(cx - s, cy - s - 4, 6, 4);
        g2.fillRect(cx + s - 6, cy - s - 4, 6, 4);
        g2.fillRect(cx - 4, cy - s - 8, 8, 8);
        g2.setColor(Color.BLACK);
        g2.drawRect(cx - s, cy - s, s * 2, s * 2);
        g2.setColor(new Color(120, 90, 20));
        g2.fillRect(cx - 4, cy - 2, 8, s * 2);
        g2.setColor(new Color(255, 230, 120));
        g2.setFont(new Font("Georgia", Font.BOLD, 10));
        g2.drawString("TH", cx - 8, cy + 8);
    }

    private void drawLumberMill(Graphics2D g2, int cx, int cy, boolean vis) {
        int s = 11;
        g2.setColor(vis ? new Color(139, 90, 43) : new Color(100, 65, 30));
        g2.fillRect(cx - s, cy - s / 2, s * 2, s + 4);
        g2.setColor(vis ? new Color(205, 205, 205) : new Color(140, 140, 140));
        g2.drawOval(cx - 7, cy - s - 6, 14, 14);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.drawString("LM", cx - 8, cy + 9);
    }

    private void drawFarm(Graphics2D g2, int cx, int cy, boolean vis) {
        g2.setColor(vis ? new Color(120, 80, 40) : new Color(80, 55, 28));
        g2.fillRect(cx - 13, cy - 11, 26, 22);
        g2.setColor(vis ? new Color(100, 210, 60) : new Color(60, 130, 40));
        for (int i = 0; i < 3; i++) {
            g2.fillRect(cx - 11, cy - 9 + i * 7, 22, 4);
        }
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.drawString("FM", cx - 8, cy + 9);
    }

    private void drawStoneMine(Graphics2D g2, int cx, int cy, boolean vis) {
        g2.setColor(vis ? new Color(140, 140, 145) : new Color(90, 90, 95));
        g2.fillRect(cx - 10, cy - 8, 20, 16);
        g2.setColor(vis ? Color.WHITE : Color.GRAY);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(cx - 7, cy + 2, cx + 7, cy - 8);
        g2.drawLine(cx - 7, cy - 8, cx + 3, cy + 2);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.drawString("SM", cx - 8, cy + 7);
    }

    private void drawIronMine(Graphics2D g2, int cx, int cy, boolean vis) {
        g2.setColor(vis ? new Color(180, 60, 60) : new Color(110, 40, 40));
        g2.fillRect(cx - 10, cy - 8, 20, 16);
        g2.setColor(vis ? new Color(225, 125, 85) : new Color(150, 80, 50));
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(cx - 7, cy + 2, cx + 7, cy - 8);
        g2.drawLine(cx - 7, cy - 8, cx + 3, cy + 2);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.drawString("IM", cx - 8, cy + 7);
    }

    private void drawStable(Graphics2D g2, int cx, int cy, boolean vis) {
        g2.setColor(vis ? new Color(160, 110, 60) : new Color(100, 70, 40));
        g2.fillRect(cx - 12, cy - 8, 24, 16);
        g2.setColor(vis ? new Color(90, 58, 18) : new Color(64, 42, 14));
        g2.fillOval(cx - 6, cy - 10, 12, 9);
        g2.fillRect(cx + 4, cy - 14, 4, 12);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.drawString("ST", cx - 7, cy + 9);
    }

    private void drawTownship(Graphics2D g2, int cx, int cy, boolean vis) {
        Color c = vis ? new Color(186, 164, 84) : new Color(112, 96, 50);
        g2.setColor(c);
        g2.fillRect(cx - 14, cy - 6, 10, 12);
        g2.fillRect(cx - 2, cy - 10, 12, 16);
        g2.fillRect(cx + 6, cy - 4, 9, 10);
        g2.setColor(c.darker());
        g2.drawRect(cx - 14, cy - 6, 10, 12);
        g2.drawRect(cx - 2, cy - 10, 12, 16);
        g2.drawRect(cx + 6, cy - 4, 9, 10);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 7));
        g2.drawString("TWN", cx - 11, cy + 10);
    }

    // ---- Units -------------------------------------------------------------

    private void drawUnit(Graphics2D g2, Unit u, Point2D center) {
        int cx = (int) center.getX();
        int cy = (int) center.getY() + 6;
        int r = 14;

        g2.setColor(new Color(0, 0, 0, 110));
        g2.fillOval(cx - r + 2, cy - r + 4, r * 2, r * 2);

        Color unitColor = getUnitColor(u.getUnitType());
        boolean hasAP = u.getCurrentAP() > 0;
        if (!hasAP) unitColor = desaturate(unitColor, 0.45f);

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
        g2.drawString(icon, cx - fm.stringWidth(icon) / 2, cy + fm.getAscent() / 2 - 2);

        int barW = 26, barH = 4;
        g2.setColor(new Color(30, 30, 30, 190));
        g2.fillRect(cx - barW / 2, cy + r + 2, barW, barH);
        float apPct = u.getMaxAP() > 0 ? (float) u.getCurrentAP() / u.getMaxAP() : 0f;
        Color apColor = apPct > 0.5f ? new Color(80, 200, 80)
                : (apPct > 0.2f ? new Color(230, 180, 30) : new Color(220, 60, 60));
        g2.setColor(apColor);
        g2.fillRect(cx - barW / 2, cy + r + 2, (int) (barW * apPct), barH);

        if (u == controller.getSelectedUnit()) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(GOLD);
            String name = u.getUnitType().name().replace("_", " ");
            g2.drawString(name, cx - g2.getFontMetrics().stringWidth(name) / 2, cy - r - 4);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private Color getUnitColor(Constants.UnitType type) {
        switch (type) {
            case EXPLORER: return new Color(20, 170, 200);
            case BUILDER: return new Color(220, 140, 20);
            case WORKER: return new Color(150, 50, 205);
            case BORDER_EXPANDER: return new Color(40, 110, 225);
            default: return Color.GRAY;
        }
    }

    private String getUnitIcon(Constants.UnitType type) {
        switch (type) {
            case EXPLORER: return "E";
            case BUILDER: return "B";
            case WORKER: return "W";
            case BORDER_EXPANDER: return "X";
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
        float alpha = 0.35f + 0.35f * pulse;
        g2.setColor(new Color(1f, 0.9f, 0.2f, Math.min(1f, alpha)));
        g2.setStroke(new BasicStroke(3f + pulse * 2f));
        g2.drawOval((int) (center.getX() - r), (int) (center.getY() - r), r * 2, r * 2);
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
            case PLAINS: return PLAINS_COLOR;
            case PLAIN: return PLAIN_COLOR;
            case GRASSLAND: return GRASSLAND_COLOR;
            case FOREST: return FOREST_COLOR;
            case MOUNTAIN: return MOUNTAIN_COLOR;
            default: return PLAINS_COLOR;
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
            repaint();
        }
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
