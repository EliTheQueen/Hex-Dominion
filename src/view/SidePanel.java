package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import controller.GameController;
import model.BorderExpander;
import model.Builder;
import model.Constants;
import model.Explorer;
import model.Unit;
import model.Worker;

/** Right-hand panel: shows the selected unit and contextual action buttons. */
public class SidePanel extends JPanel {
    private final GameController controller;
    private final GamePanel gamePanel;
    private final JPanel unitInfoPanel;
    private final JPanel actionPanel;
    private final JPanel statsPanel;

    private static final Color BG = new Color(18, 20, 42);
    private static final Color PANEL_BG = new Color(25, 28, 55);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color TEXT_COLOR = new Color(200, 190, 150);

    public SidePanel(GameController controller, GamePanel gamePanel) {
        this.controller = controller;
        this.gamePanel = gamePanel;
        setPreferredSize(new Dimension(224, 0));
        setBackground(BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(60, 55, 25)));

        unitInfoPanel = section();
        actionPanel = section();
        statsPanel = section();

        add(Box.createVerticalStrut(10));
        add(unitInfoPanel);
        add(Box.createVerticalStrut(10));
        add(actionPanel);
        add(Box.createVerticalGlue());
        add(statsPanel);
        add(Box.createVerticalStrut(10));

        update();
    }

    private JPanel section() {
        JPanel p = new JPanel();
        p.setBackground(PANEL_BG);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(210, Integer.MAX_VALUE));
        return p;
    }

    public void update() {
        unitInfoPanel.removeAll();
        actionPanel.removeAll();
        statsPanel.removeAll();

        Unit sel = controller.getSelectedUnit();
        if (sel != null) {
            buildUnitInfo(sel);
            buildActionButtons(sel);
        } else {
            JLabel lbl = makeLabel("Click a unit to select it", TEXT_COLOR, Font.ITALIC, 12);
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            unitInfoPanel.add(lbl);
        }
        buildStats();

        revalidate();
        repaint();
    }

    private void buildStats() {
        if (controller.getGameState() == null) return;
        model.Player p = controller.getGameState().getPlayer();
        statsPanel.add(makeCentered(makeLabel("EMPIRE", GOLD, Font.BOLD, 13)));
        statsPanel.add(Box.createVerticalStrut(4));
        statsPanel.add(makeCentered(makeLabel("Territory: " + p.getTerritorySize(), TEXT_COLOR, Font.PLAIN, 11)));
        statsPanel.add(makeCentered(makeLabel("Buildings: " + p.getBuildingCount(), TEXT_COLOR, Font.PLAIN, 11)));
        statsPanel.add(makeCentered(makeLabel("Units: " + p.getUnitCount(), TEXT_COLOR, Font.PLAIN, 11)));
        statsPanel.add(makeCentered(makeLabel("Tech: " + p.getResearched().size() + "/6", TEXT_COLOR, Font.PLAIN, 11)));
        statsPanel.add(Box.createVerticalStrut(4));
        statsPanel.add(makeCentered(makeLabel("Score: " + controller.getGameState().getCurrentScore(), GOLD, Font.BOLD, 13)));
    }

    private void buildUnitInfo(Unit u) {
        JLabel titleLbl = makeLabel(u.getUnitType().name().replace("_", " "), GOLD, Font.BOLD, 16);
        unitInfoPanel.add(makeCentered(titleLbl));
        unitInfoPanel.add(Box.createVerticalStrut(6));

        unitInfoPanel.add(makeCentered(makeLabel(
                "Pos: (" + u.getPosition().getQ() + ", " + u.getPosition().getR() + ")",
                TEXT_COLOR, Font.PLAIN, 11)));

        unitInfoPanel.add(Box.createVerticalStrut(8));
        unitInfoPanel.add(makeCentered(makeLabel(
                "Action Points: " + u.getCurrentAP() + "/" + u.getMaxAP(), TEXT_COLOR, Font.PLAIN, 12)));

        JPanel apBar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(40, 40, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
                float pct = u.getMaxAP() > 0 ? (float) u.getCurrentAP() / u.getMaxAP() : 0;
                Color c = pct > 0.5f ? new Color(80, 200, 80)
                        : (pct > 0.2f ? new Color(230, 180, 30) : new Color(220, 60, 60));
                g2.setColor(c);
                g2.fillRoundRect(0, 0, (int) (getWidth() * pct), getHeight(), 5, 5);
            }
        };
        apBar.setPreferredSize(new Dimension(185, 9));
        apBar.setMaximumSize(new Dimension(185, 9));
        apBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        unitInfoPanel.add(Box.createVerticalStrut(3));
        unitInfoPanel.add(apBar);

        unitInfoPanel.add(Box.createVerticalStrut(6));
        unitInfoPanel.add(makeCentered(makeLabel("State: " + u.getState().name(), TEXT_COLOR, Font.PLAIN, 11)));

        if (u instanceof Builder) {
            Builder b = (Builder) u;
            unitInfoPanel.add(Box.createVerticalStrut(4));
            unitInfoPanel.add(makeCentered(makeLabel("Charges: " + b.getCharges() + "/" + Constants.BUILDER_CHARGES,
                    b.hasCharges() ? new Color(80, 200, 80) : new Color(220, 80, 80), Font.BOLD, 12)));
        } else if (u instanceof Explorer) {
            Explorer e = (Explorer) u;
            unitInfoPanel.add(Box.createVerticalStrut(4));
            unitInfoPanel.add(makeCentered(makeLabel("Auto-Explore: " + (e.isAutoExploreMode() ? "ON" : "OFF"),
                    e.isAutoExploreMode() ? new Color(80, 200, 80) : TEXT_COLOR, Font.PLAIN, 11)));
        } else if (u instanceof Worker) {
            Worker w = (Worker) u;
            String stationed = w.isStationed() ? w.getStationedAt().getType().name() : "No";
            unitInfoPanel.add(Box.createVerticalStrut(4));
            unitInfoPanel.add(makeCentered(makeLabel("Stationed: " + stationed, TEXT_COLOR, Font.PLAIN, 11)));
        }
    }

    private void buildActionButtons(Unit u) {
        actionPanel.add(makeCentered(makeLabel("ACTIONS", GOLD, Font.BOLD, 13)));
        actionPanel.add(Box.createVerticalStrut(8));

        if (u instanceof Explorer) {
            addActionBtn("Toggle Auto-Explore", e -> { controller.onAutoExploreToggled(); gamePanel.repaintAll(); });
        } else if (u instanceof Builder) {
            addActionBtn("Build Lumber Mill", e -> { controller.onBuildChosen(Constants.BuildingType.LUMBER_MILL); gamePanel.repaintAll(); });
            addActionBtn("Build Farm", e -> { controller.onBuildChosen(Constants.BuildingType.FARM); gamePanel.repaintAll(); });
            addActionBtn("Build Stable", e -> { controller.onBuildChosen(Constants.BuildingType.STABLE); gamePanel.repaintAll(); });
            addActionBtn("Build Stone Mine", e -> { controller.onBuildChosen(Constants.BuildingType.STONE_MINE); gamePanel.repaintAll(); });
            addActionBtn("Build Iron Mine", e -> { controller.onBuildChosen(Constants.BuildingType.IRON_MINE); gamePanel.repaintAll(); });
            addActionBtn("Build Township", e -> { controller.onBuildChosen(Constants.BuildingType.TOWNSHIP); gamePanel.repaintAll(); });
        } else if (u instanceof Worker) {
            addActionBtn("Station Here", e -> { controller.onStationWorker(); gamePanel.repaintAll(); });
            addActionBtn("Unstation", e -> { controller.onUnstationWorker(); gamePanel.repaintAll(); });
        } else if (u instanceof BorderExpander) {
            addActionBtn("Expand Border", e -> { controller.onExpandBorder(); gamePanel.repaintAll(); });
        }

        actionPanel.add(Box.createVerticalStrut(6));
        addActionBtn("Deselect (ESC)", e -> { controller.deselectUnit(); gamePanel.repaintAll(); });
    }

    private void addActionBtn(String text, ActionListener al) {
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
                g2.setColor(hovered ? new Color(50, 60, 100) : new Color(35, 40, 75));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(hovered ? GOLD : new Color(100, 90, 50));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.setColor(TEXT_COLOR);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
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
        btn.setMaximumSize(new Dimension(195, 32));
        btn.setPreferredSize(new Dimension(195, 32));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(al);
        actionPanel.add(btn);
        actionPanel.add(Box.createVerticalStrut(5));
    }

    private JPanel makeCentered(JLabel lbl) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(Box.createHorizontalGlue());
        p.add(lbl);
        p.add(Box.createHorizontalGlue());
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(200, 22));
        return p;
    }

    private JLabel makeLabel(String text, Color color, int style, int size) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(color);
        lbl.setFont(new Font("SansSerif", style, size));
        return lbl;
    }
}
