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
import model.Player;
import model.ResourceStorage;

/** Top heads-up display: resources, turn counter, score and global action buttons. */
public class HudPanel extends JPanel {
    private final GameController controller;
    private final GamePanel gamePanel;
    private final JButton endTurnBtn;
    private final JButton techBtn;
    private final JButton recruitBtn;

    private static final Color BG = new Color(15, 17, 35);
    private static final Color BORDER_COLOR = new Color(80, 70, 30);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color GREEN = new Color(80, 200, 80);
    private static final Color RED = new Color(220, 80, 80);
    private static final Color YELLOW = new Color(230, 200, 60);

    public HudPanel(GameController controller, GamePanel gamePanel) {
        this.controller = controller;
        this.gamePanel = gamePanel;
        setPreferredSize(new Dimension(0, 66));
        setBackground(BG);
        setLayout(null);
        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR));

        endTurnBtn = createBtn("END TURN", new Color(140, 100, 20), new Color(185, 135, 30));
        techBtn = createBtn("RESEARCH", new Color(20, 60, 120), new Color(30, 90, 160));
        recruitBtn = createBtn("RECRUIT", new Color(60, 30, 80), new Color(90, 50, 120));

        endTurnBtn.addActionListener(e -> {
            controller.onEndTurnClicked();
            gamePanel.repaintAll();
        });
        techBtn.addActionListener(e -> showTechDialog());
        recruitBtn.addActionListener(e -> showRecruitDialog());
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
        endTurnBtn.setBounds(getWidth() - 130, 13, 115, 40);
        techBtn.setBounds(getWidth() - 255, 13, 115, 40);
        recruitBtn.setBounds(getWidth() - 380, 13, 115, 40);
    }

    public void update() { repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (controller.getGameState() == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Player p = controller.getGameState().getPlayer();
        ResourceStorage rs = p.getResources();

        GradientPaint bgGrad = new GradientPaint(0, 0, new Color(18, 20, 40), getWidth(), 0, BG);
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, getWidth(), getHeight());

        int x = 14;
        x = drawResource(g2, x, "FOOD", rs.get(Constants.ResourceType.FOOD), rs.getCap(Constants.ResourceType.FOOD),
                new Color(110, 220, 90));
        x = drawResource(g2, x, "WOOD", rs.get(Constants.ResourceType.WOOD), rs.getCap(Constants.ResourceType.WOOD),
                new Color(190, 140, 70));
        x = drawResource(g2, x, "STONE", rs.get(Constants.ResourceType.STONE), rs.getCap(Constants.ResourceType.STONE),
                new Color(185, 185, 190));
        x = drawResource(g2, x, "IRON", rs.get(Constants.ResourceType.IRON), rs.getCap(Constants.ResourceType.IRON),
                new Color(220, 110, 110));

        // Turn + score block, centred in the free area between resources and buttons.
        int turn = controller.getGameState().getCurrentTurn();
        int maxTurn = controller.getGameState().getMaxTurns();
        String turnText = "TURN  " + turn + " / " + maxTurn;
        g2.setFont(new Font("Georgia", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(turnText);

        int areaStart = x + 10;
        int areaEnd = getWidth() - 395;
        int centerX = areaStart + Math.max(0, (areaEnd - areaStart - tw) / 2);

        g2.setColor(new Color(80, 70, 30, 110));
        g2.fillRoundRect(centerX - 12, 10, tw + 24, 30, 8, 8);
        g2.setColor(GOLD);
        g2.drawString(turnText, centerX, 33);

        int score = controller.getGameState().getCurrentScore();
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(new Color(170, 155, 110));
        String scoreStr = "Score: " + score;
        g2.drawString(scoreStr, centerX + (tw - g2.getFontMetrics().stringWidth(scoreStr)) / 2, 54);

        // Status message (left-aligned under resources area is tight; show near center bottom).
        String status = controller.getStatusMessage();
        if (status != null && !status.isEmpty()) {
            g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
            g2.setColor(new Color(210, 190, 130));
            g2.drawString(status, areaStart, 18);
        }

        g2.dispose();
    }

    private int drawResource(Graphics2D g2, int x, String name, int val, int cap, Color bright) {
        int boxW = 108, boxH = 46, y = 10;
        g2.setColor(new Color(30, 35, 60));
        g2.fillRoundRect(x, y, boxW, boxH, 6, 6);
        g2.setColor(bright.darker().darker());
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, boxW, boxH, 6, 6);

        // Colour swatch.
        g2.setColor(bright);
        g2.fillOval(x + 8, y + 6, 10, 10);

        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.setColor(bright.brighter());
        g2.drawString(name, x + 24, y + 15);

        String valStr = val + " / " + cap;
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        Color valColor = val < cap * 0.15 ? RED : (val < cap * 0.4 ? YELLOW : GREEN);
        g2.setColor(valColor);
        g2.drawString(valStr, x + 8, y + 33);

        g2.setColor(new Color(40, 40, 60));
        g2.fillRect(x + 8, y + 38, boxW - 16, 5);
        float pct = cap > 0 ? Math.min(1f, (float) val / cap) : 0f;
        g2.setColor(bright);
        g2.fillRect(x + 8, y + 38, (int) ((boxW - 16) * pct), 5);

        return x + boxW + 8;
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
