package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import controller.GameController;
import model.Constants;
import model.GameState;
import model.Player;
import model.ProductionTask;
import model.ResourceStorage;
import model.season.Season;
import model.season.SeasonCycle;
import model.happiness.HappinessLevel;
import model.townhall.ProductionCommand;

/** Top heads-up display: resources with net rates, unit cap, turn, queue, warnings and actions. */
public class HudPanel extends JPanel {
    private final GameController controller;
    private final GamePanel gamePanel;
    private final JButton endTurnBtn;
    private final JButton techBtn;
    private final JButton recruitBtn;
    private final JButton settingsBtn;
    private final JButton saveBtn;
    private final JButton tradeBtn;

    private static final Color BG = new Color(15, 17, 35);
    private static final Color BORDER_COLOR = new Color(80, 70, 30);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color GREEN = new Color(90, 210, 90);
    private static final Color RED = new Color(225, 85, 85);
    private static final Color YELLOW = new Color(230, 200, 60);

    public HudPanel(GameController controller, GamePanel gamePanel) {
        this.controller = controller;
        this.gamePanel = gamePanel;
        setPreferredSize(new Dimension(0, 108));
        setBackground(BG);
        setLayout(null);
        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR));

        endTurnBtn = createBtn("END TURN", new Color(140, 100, 20), new Color(185, 135, 30));
        techBtn = createBtn("RESEARCH", new Color(20, 60, 120), new Color(30, 90, 160));
        recruitBtn = createBtn("RECRUIT", new Color(60, 30, 80), new Color(90, 50, 120));
        settingsBtn = createBtn("⚙", new Color(45, 50, 70), new Color(70, 78, 105));
        saveBtn = createBtn("SAVE / LOAD", new Color(45, 65, 78), new Color(65, 92, 108));
        tradeBtn = createBtn("TRADE", new Color(83, 62, 34), new Color(115, 88, 48));

        endTurnBtn.addActionListener(e -> {
            controller.onEndTurnClicked();
            gamePanel.repaintAll();
        });
        techBtn.addActionListener(e -> showTechDialog());
        recruitBtn.addActionListener(e -> showRecruitDialog());
        settingsBtn.addActionListener(e -> {
            java.awt.Window w = SwingUtilities.getWindowAncestor(this);
            new SettingsPanel(w).setVisible(true);
        });
        saveBtn.addActionListener(e -> {
            MainWindow window = (MainWindow) SwingUtilities.getWindowAncestor(this);
            new SaveLoadDialog(window, controller, true).setVisible(true);
            gamePanel.repaintAll();
        });
        tradeBtn.addActionListener(e -> {
            new TradePanel(controller, (JFrame) SwingUtilities.getWindowAncestor(this)).setVisible(true);
            gamePanel.repaintAll();
        });
    }

    private JButton createBtn(String text, Color normal, Color hover) {
        JButton btn = new JButton(text) {
            boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Color c = hovered ? hover : normal;
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(GOLD);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(btn);
        return btn;
    }

    @Override
    public void doLayout() {
        super.doLayout();
        endTurnBtn.setBounds(getWidth() - 130, 14, 115, 40);
        techBtn.setBounds(getWidth() - 255, 14, 115, 40);
        recruitBtn.setBounds(getWidth() - 380, 14, 115, 40);
        settingsBtn.setBounds(getWidth() - 428, 14, 40, 40);
        saveBtn.setBounds(getWidth() - 548, 14, 112, 40);
        tradeBtn.setBounds(getWidth() - 660, 14, 104, 40);
    }

    public void update() { repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (controller.getGameState() == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GameState gs = controller.getGameState();
        Player p = gs.getPlayer();
        ResourceStorage rs = p.getResources();
        int[] net = gs.getNetRate();

        GradientPaint bgGrad = new GradientPaint(0, 0, new Color(18, 20, 40), getWidth(), 0, BG);
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, getWidth(), getHeight());

        int x = 12;
        x = drawResource(g2, x, "FOOD", rs.get(Constants.ResourceType.FOOD), rs.getCap(Constants.ResourceType.FOOD),
                net[Constants.ResourceType.FOOD.ordinal()], new Color(120, 220, 100));
        x = drawResource(g2, x, "WOOD", rs.get(Constants.ResourceType.WOOD), rs.getCap(Constants.ResourceType.WOOD),
                net[Constants.ResourceType.WOOD.ordinal()], new Color(195, 145, 75));
        x = drawResource(g2, x, "STONE", rs.get(Constants.ResourceType.STONE), rs.getCap(Constants.ResourceType.STONE),
                net[Constants.ResourceType.STONE.ordinal()], new Color(190, 190, 195));
        x = drawResource(g2, x, "IRON", rs.get(Constants.ResourceType.IRON), rs.getCap(Constants.ResourceType.IRON),
                net[Constants.ResourceType.IRON.ordinal()], new Color(225, 115, 115));

        // Units x/cap box.
        x = drawUnitBox(g2, x, p);

        // Turn + score block.
        int turn = gs.getCurrentTurn();
        String turnText = "TURN  " + turn;
        g2.setFont(new Font("Georgia", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(turnText);

        int areaStart = x + 10;
        int areaEnd = getWidth() - 443;
        int centerX = areaStart + Math.max(0, (areaEnd - areaStart - tw) / 2);

        g2.setColor(new Color(80, 70, 30, 110));
        g2.fillRoundRect(centerX - 12, 10, tw + 24, 28, 8, 8);
        g2.setColor(GOLD);
        g2.drawString(turnText, centerX, 32);

        SeasonCycle cycle = gs.getSeasonCycle();
        Season season = cycle.getCurrentSeason();
        String seasonText = season.name() + "  " + cycle.getTurnInsideSeason()
                + "/" + SeasonCycle.TURNS_PER_SEASON;
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        Color seasonColor;
        switch (season) {
            case SPRING: seasonColor = new Color(120, 225, 135); break;
            case SUMMER: seasonColor = new Color(255, 195, 70); break;
            case AUTUMN: seasonColor = new Color(225, 135, 65); break;
            case WINTER: seasonColor = new Color(160, 215, 255); break;
            default: seasonColor = GOLD;
        }
        g2.setColor(seasonColor);
        int sw = g2.getFontMetrics().stringWidth(seasonText);
        g2.drawString(seasonText, centerX + (tw - sw) / 2, 64);

        int score = gs.getCurrentScore();
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(new Color(170, 155, 110));
        String scoreStr = "Score: " + score;
        g2.drawString(scoreStr, centerX + (tw - g2.getFontMetrics().stringWidth(scoreStr)) / 2, 50);

        // Production queue (front task) under the turn block.
        ProductionTask front = p.getProductionQueue().getFront();
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        if (front != null) {
            g2.setColor(new Color(150, 200, 240));
            String q = "Producing: " + front.getLabel()
                    + (p.getProductionQueue().size() > 1 ? "  (+" + (p.getProductionQueue().size() - 1) + " queued)" : "");
            g2.drawString(q, areaStart, 80);
        } else {
            g2.setColor(new Color(120, 120, 140));
            g2.drawString("Production queue empty", areaStart, 80);
        }

        // Warnings (starvation, idle units) on the bottom row, prominent.
        int wy = 102;
        int wx = areaStart;
        if (gs.isStarving()) {
            wx = drawBadge(g2, wx, wy, "⚠ STARVATION", RED);
        }
        int idle = gs.getIdleUnitsWithAP().size();
        if (idle > 0) {
            wx = drawBadge(g2, wx, wy, idle + " unit" + (idle == 1 ? "" : "s") + " still have AP", YELLOW);
        }

        if (!gs.getLastTurnEvents().isEmpty()) {
            String event = gs.getLastTurnEvents().get(gs.getLastTurnEvents().size() - 1);
            drawBadge(g2, wx, wy, event.replace('_', ' '), new Color(110, 185, 235));
        }

        HappinessLevel happiness = gs.getHappinessLevel();
        Color happinessColor = happiness == HappinessLevel.GOLDEN_AGE ? GOLD
                : happiness == HappinessLevel.NORMAL ? new Color(150, 185, 170)
                : happiness == HappinessLevel.UNHAPPY ? new Color(125, 155, 195) : RED;
        drawBadge(g2, 12, 84, "HAPPINESS " + (gs.getHappinessScore() >= 0 ? "+" : "")
                + gs.getHappinessScore() + "  " + happiness.name().replace('_', ' '), happinessColor);

        ProductionCommand hallCommand = gs.getTownHall().getCommandSlot().getActiveCommand();
        String hallText = "TOWN HALL " + gs.getTownHall().getLevel().getLevelNumber()
                + "  " + gs.getTownHall().getCurrentHp() + "/" + gs.getTownHall().getMaxHp() + " HP";
        if (hallCommand != null) hallText += "  •  " + hallCommand.getRemainingTurns() + " turns";
        drawBadge(g2, 220, 84, hallText, new Color(125, 175, 220));

        drawBadge(g2, 515, 84, "MILITARY " + gs.getMilitaryUnitCount() + "/"
                + gs.getMilitaryUnitCap(gs.getTownHall().getLevel()), new Color(200, 145, 105));

        // Status message near the buttons.
        String status = controller.getStatusMessage();
        if (status != null && !status.isEmpty()) {
            g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
            g2.setColor(new Color(210, 190, 130));
            FontMetrics sfm = g2.getFontMetrics();
            g2.drawString(status, getWidth() - 130 - sfm.stringWidth(status) - 12, 70);
        }

        g2.dispose();
    }

    private int drawBadge(Graphics2D g2, int x, int y, String text, Color color) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text) + 16;
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
        g2.fillRoundRect(x, y - 14, w, 19, 8, 8);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y - 14, w, 19, 8, 8);
        g2.drawString(text, x + 8, y);
        return x + w + 10;
    }

    private int drawResource(Graphics2D g2, int x, String name, int val, int cap, int rate, Color bright) {
        int boxW = 118, boxH = 50, y = 10;
        g2.setColor(new Color(30, 35, 60));
        g2.fillRoundRect(x, y, boxW, boxH, 6, 6);
        g2.setColor(bright.darker().darker());
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, boxW, boxH, 6, 6);

        g2.setColor(bright);
        g2.fillOval(x + 8, y + 6, 10, 10);

        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.setColor(bright.brighter());
        g2.drawString(name, x + 24, y + 15);

        // Net rate, coloured by sign.
        String rateStr = (rate >= 0 ? "+" : "") + rate;
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(rate < 0 ? RED : (rate == 0 ? new Color(150, 150, 165) : GREEN));
        FontMetrics rfm = g2.getFontMetrics();
        g2.drawString(rateStr, x + boxW - rfm.stringWidth(rateStr) - 8, y + 15);

        String valStr = val + " / " + cap;
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        Color valColor = val < cap * 0.15 ? RED : (val < cap * 0.4 ? YELLOW : GREEN);
        g2.setColor(valColor);
        g2.drawString(valStr, x + 8, y + 35);

        g2.setColor(new Color(40, 40, 60));
        g2.fillRect(x + 8, y + 41, boxW - 16, 5);
        float pct = cap > 0 ? Math.min(1f, (float) val / cap) : 0f;
        g2.setColor(bright);
        g2.fillRect(x + 8, y + 41, (int) ((boxW - 16) * pct), 5);

        return x + boxW + 7;
    }

    private int drawUnitBox(Graphics2D g2, int x, Player p) {
        int boxW = 96, boxH = 50, y = 10;
        boolean atCap = p.atUnitCap();
        g2.setColor(new Color(30, 35, 60));
        g2.fillRoundRect(x, y, boxW, boxH, 6, 6);
        g2.setColor(atCap ? RED.darker() : new Color(70, 70, 100));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, boxW, boxH, 6, 6);

        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.setColor(new Color(180, 180, 200));
        g2.drawString("UNITS", x + 8, y + 15);

        String v = p.getUnitCount() + " / " + p.getUnitCap();
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.setColor(atCap ? RED : GOLD);
        g2.drawString(v, x + 8, y + 36);
        return x + boxW + 10;
    }

    private void showTechDialog() {
        TechPanel techPanel = new TechPanel(controller, (JFrame) SwingUtilities.getWindowAncestor(this));
        techPanel.setVisible(true);
        gamePanel.repaintAll();
    }

    private void showRecruitDialog() {
        RecruitPanel recruitPanel = new RecruitPanel(controller, (JFrame) SwingUtilities.getWindowAncestor(this));
        recruitPanel.setVisible(true);
        gamePanel.repaintAll();
    }
}
