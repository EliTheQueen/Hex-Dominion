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
    private final JButton tribesBtn;

    private static final Color BG = new Color(15, 17, 35);
    private static final Color BORDER_COLOR = new Color(80, 70, 30);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color GREEN = new Color(90, 210, 90);
    private static final Color RED = new Color(225, 85, 85);
    private static final Color YELLOW = new Color(230, 200, 60);
    private static final Color CARD_BG = new Color(27, 31, 55);
    private static final Color CARD_EDGE = new Color(74, 79, 108);
    private static final int HUD_HEIGHT = 138;
    private static final int PAD = 12;
    private static final int GAP = 8;
    private static final int TOP_Y = 10;
    private static final int TOP_H = 48;
    private static final int LOWER_Y = 68;
    private static final int LOWER_H = 56;

    public HudPanel(GameController controller, GamePanel gamePanel) {
        this.controller = controller;
        this.gamePanel = gamePanel;
        setPreferredSize(new Dimension(0, HUD_HEIGHT));
        setBackground(BG);
        setLayout(null);
        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR));

        endTurnBtn = createBtn("END TURN", new Color(140, 100, 20), new Color(185, 135, 30));
        techBtn = createBtn("RESEARCH", new Color(20, 60, 120), new Color(30, 90, 160));
        recruitBtn = createBtn("RECRUIT", new Color(60, 30, 80), new Color(90, 50, 120));
        settingsBtn = createBtn("⚙", new Color(45, 50, 70), new Color(70, 78, 105));
        saveBtn = createBtn("SAVE / LOAD", new Color(45, 65, 78), new Color(65, 92, 108));
        saveBtn.setName("save-load");
        tradeBtn = createBtn("TRADE", new Color(83, 62, 34), new Color(115, 88, 48));
        tribesBtn = createBtn("TRIBES", new Color(63, 50, 83), new Color(89, 70, 116));

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
        tribesBtn.addActionListener(e -> {
            new TribePanel(controller, (JFrame) SwingUtilities.getWindowAncestor(this)).setVisible(true);
            gamePanel.repaintAll();
        });
        update();
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
                g2.setColor(new Color(0, 0, 0, 75));
                g2.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 3, 10, 10);
                g2.setPaint(new GradientPaint(0, 0, c.brighter(), 0, getHeight(), c.darker()));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 10, 10);
                g2.setColor(hovered ? GOLD.brighter() : new Color(170, 145, 63));
                g2.setStroke(new BasicStroke(hovered ? 1.6f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, getText().length() > 8 ? 10 : 11));
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
        int right = getWidth() - PAD;
        endTurnBtn.setBounds(right - 112, 14, 112, 40);
        right -= 112 + GAP;
        techBtn.setBounds(right - 106, 14, 106, 40);
        right -= 106 + GAP;
        recruitBtn.setBounds(right - 100, 14, 100, 40);

        right = getWidth() - PAD;
        settingsBtn.setBounds(right - 40, 76, 40, 38);
        right -= 40 + GAP;
        saveBtn.setBounds(right - 104, 76, 104, 38);
        right -= 104 + GAP;
        tradeBtn.setBounds(right - 88, 76, 88, 38);
        right -= 88 + GAP;
        tribesBtn.setBounds(right - 88, 76, 88, 38);
    }

    public void update() {
        model.save.SaveAvailability availability = controller.getSaveAvailability();
        saveBtn.setToolTipText("Open Save / Load. Manual save "
                + (availability.isEnabled() ? "available: " : "disabled: ") + availability.getReason());
        repaint();
    }

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

        int x = PAD;
        x = drawResource(g2, x, "FOOD", rs.get(Constants.ResourceType.FOOD), rs.getCap(Constants.ResourceType.FOOD),
                net[Constants.ResourceType.FOOD.ordinal()], new Color(120, 220, 100));
        x = drawResource(g2, x, "WOOD", rs.get(Constants.ResourceType.WOOD), rs.getCap(Constants.ResourceType.WOOD),
                net[Constants.ResourceType.WOOD.ordinal()], new Color(195, 145, 75));
        x = drawResource(g2, x, "STONE", rs.get(Constants.ResourceType.STONE), rs.getCap(Constants.ResourceType.STONE),
                net[Constants.ResourceType.STONE.ordinal()], new Color(190, 190, 195));
        x = drawResource(g2, x, "IRON", rs.get(Constants.ResourceType.IRON), rs.getCap(Constants.ResourceType.IRON),
                net[Constants.ResourceType.IRON.ordinal()], new Color(225, 115, 115));

        x = drawUnitBox(g2, x, p);

        int primaryStart = getWidth() - PAD - 112 - GAP - 106 - GAP - 100;
        int turnX = x + 2;
        int turnW = Math.max(116, primaryStart - GAP - turnX);
        SeasonCycle cycle = gs.getSeasonCycle();
        Season season = cycle.getCurrentSeason();
        drawTurnCard(g2, turnX, TOP_Y, turnW, TOP_H, gs.getCurrentTurn(), season,
                cycle.getTurnInsideSeason(), gs.getCurrentScore());

        int utilityStart = getWidth() - PAD - 40 - GAP - 104 - GAP - 88 - GAP - 88;
        int infoRight = utilityStart - GAP;
        int happinessW = 160;
        int hallW = Math.min(260, Math.max(220, (infoRight - PAD) / 3));
        int commandX = PAD + happinessW + GAP + hallW + GAP;
        int commandW = Math.max(150, infoRight - commandX);

        HappinessLevel happiness = gs.getHappinessLevel();
        Color happinessColor = happiness == HappinessLevel.GOLDEN_AGE ? GOLD
                : happiness == HappinessLevel.NORMAL ? new Color(147, 205, 174)
                : happiness == HappinessLevel.UNHAPPY ? new Color(125, 155, 195) : RED;
        drawInfoCard(g2, PAD, LOWER_Y, happinessW, LOWER_H, "HAPPINESS",
                (gs.getHappinessScore() >= 0 ? "+" : "") + gs.getHappinessScore()
                        + "  " + happiness.name().replace('_', ' '), happinessColor);

        ProductionCommand hallCommand = gs.getTownHall().getCommandSlot().getActiveCommand();
        String hallValue = "Level " + gs.getTownHall().getLevel().getLevelNumber() + "  •  "
                + gs.getTownHall().getCurrentHp() + "/" + gs.getTownHall().getMaxHp() + " HP";
        drawInfoCard(g2, PAD + happinessW + GAP, LOWER_Y, hallW, LOWER_H,
                "TOWN HALL", hallValue, new Color(125, 185, 230));

        ProductionTask front = p.getProductionQueue().getFront();
        String commandTitle = hallCommand == null ? "COMMAND" : "COMMAND • " + hallCommand.getRemainingTurns() + " TURNS";
        String commandValue = hallCommand != null ? commandName(hallCommand)
                : front != null ? front.getLabel() : "Town Hall ready";
        int idle = gs.getIdleUnitsWithAP().size();
        String status = controller.getStatusMessage();
        String alert = gs.isStarving() ? "STARVATION"
                : status != null && !status.isEmpty() ? status
                : idle > 0 ? idle + " unit" + (idle == 1 ? "" : "s") + " ready"
                : !gs.getLastTurnEvents().isEmpty() ? gs.getLastTurnEvents().get(gs.getLastTurnEvents().size() - 1).replace('_', ' ')
                : "All systems normal";
        drawCommandCard(g2, commandX, LOWER_Y, commandW, LOWER_H, commandTitle,
                commandValue, alert, gs.isStarving() ? RED : idle > 0 ? YELLOW : new Color(150, 200, 240));

        g2.dispose();
    }

    private int drawResource(Graphics2D g2, int x, String name, int val, int cap, int rate, Color bright) {
        int boxW = 100, boxH = TOP_H, y = TOP_Y;
        drawCard(g2, x, y, boxW, boxH, bright.darker().darker());
        g2.setColor(bright);
        g2.fillOval(x + 8, y + 7, 8, 8);
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(bright.brighter());
        g2.drawString(name, x + 21, y + 15);

        String rateStr = (rate >= 0 ? "+" : "") + rate;
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.setColor(rate < 0 ? RED : (rate == 0 ? new Color(150, 150, 165) : GREEN));
        FontMetrics rfm = g2.getFontMetrics();
        g2.drawString(rateStr, x + boxW - rfm.stringWidth(rateStr) - 7, y + 15);

        String valStr = val + " / " + cap;
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        Color valColor = val < cap * 0.15 ? RED : (val < cap * 0.4 ? YELLOW : GREEN);
        g2.setColor(valColor);
        g2.drawString(valStr, x + 8, y + 33);

        g2.setColor(new Color(11, 13, 27, 190));
        g2.fillRoundRect(x + 8, y + 39, boxW - 16, 4, 4, 4);
        float pct = cap > 0 ? Math.min(1f, (float) val / cap) : 0f;
        g2.setColor(bright);
        g2.fillRoundRect(x + 8, y + 39, (int) ((boxW - 16) * pct), 4, 4, 4);
        return x + boxW + GAP;
    }

    private int drawUnitBox(Graphics2D g2, int x, Player p) {
        int boxW = 76;
        drawCard(g2, x, TOP_Y, boxW, TOP_H, p.atUnitCap() ? RED.darker() : CARD_EDGE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(new Color(180, 180, 200));
        g2.drawString("UNITS", x + 8, TOP_Y + 15);
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2.setColor(p.atUnitCap() ? RED : GOLD);
        g2.drawString(p.getUnitCount() + " / " + p.getUnitCap(), x + 8, TOP_Y + 35);
        return x + boxW + GAP;
    }

    private void drawTurnCard(Graphics2D g2, int x, int y, int w, int h, int turn,
                              Season season, int turnInsideSeason, int score) {
        drawCard(g2, x, y, w, h, seasonColor(season).darker());
        String turnText = "TURN " + turn;
        g2.setFont(new Font("Georgia", Font.BOLD, w < 170 ? 15 : 18));
        g2.setColor(GOLD);
        g2.drawString(turnText, x + 12, y + 21);

        String seasonText = season.name() + "  " + turnInsideSeason + "/" + SeasonCycle.TURNS_PER_SEASON;
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.setColor(seasonColor(season));
        g2.drawString(clipText(g2, seasonText, Math.max(60, w - 24)), x + 12, y + 39);

        if (w >= 190) {
            String scoreText = "SCORE " + score;
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(new Color(185, 175, 140));
            g2.drawString(scoreText, x + w - g2.getFontMetrics().stringWidth(scoreText) - 12, y + 21);
        }
    }

    private void drawInfoCard(Graphics2D g2, int x, int y, int w, int h,
                              String title, String value, Color accent) {
        drawCard(g2, x, y, w, h, accent.darker());
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(new Color(155, 160, 185));
        g2.drawString(title, x + 12, y + 17);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(accent);
        g2.drawString(clipText(g2, value, w - 24), x + 12, y + 39);
    }

    private void drawCommandCard(Graphics2D g2, int x, int y, int w, int h,
                                 String title, String command, String alert, Color accent) {
        drawCard(g2, x, y, w, h, accent.darker());
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(new Color(155, 160, 185));
        g2.drawString(clipText(g2, title, w - 24), x + 12, y + 16);
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(220, 222, 232));
        g2.drawString(clipText(g2, command, w - 24), x + 12, y + 33);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g2.setColor(accent);
        g2.drawString(clipText(g2, alert, w - 24), x + 12, y + 48);
    }

    private void drawCard(Graphics2D g2, int x, int y, int w, int h, Color edge) {
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillRoundRect(x + 1, y + 2, w, h, 10, 10);
        g2.setPaint(new GradientPaint(x, y, CARD_BG.brighter(), x, y + h, CARD_BG.darker()));
        g2.fillRoundRect(x, y, w, h, 10, 10);
        g2.setColor(new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), 175));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, h, 10, 10);
    }

    private Color seasonColor(Season season) {
        switch (season) {
            case SPRING: return new Color(124, 220, 146);
            case SUMMER: return new Color(250, 193, 76);
            case AUTUMN: return new Color(224, 133, 66);
            case WINTER: return new Color(150, 207, 245);
            default: return GOLD;
        }
    }

    private String commandName(ProductionCommand command) {
        String name = command.getClass().getSimpleName().replace("Command", "");
        return name.replaceAll("([a-z])([A-Z])", "$1 $2").toUpperCase();
    }

    private String clipText(Graphics2D g2, String text, int maxWidth) {
        if (text == null || text.isEmpty() || g2.getFontMetrics().stringWidth(text) <= maxWidth) return text;
        String ellipsis = "…";
        int end = text.length();
        while (end > 0 && g2.getFontMetrics().stringWidth(text.substring(0, end) + ellipsis) > maxWidth) end--;
        return end == 0 ? ellipsis : text.substring(0, end) + ellipsis;
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
