package view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import controller.GameController;
import model.BorderExpander;
import model.Builder;
import model.Building;
import model.Constants;
import model.Explorer;
import model.Unit;
import model.Worker;
import model.military.MilitaryUnit;

/** Right-hand panel: shows the selected unit and contextual action buttons. */
public class SidePanel extends JPanel {
    private final GameController controller;
    private final GamePanel gamePanel;
    private final JPanel unitInfoPanel;
    private final JPanel actionPanel;
    private final JPanel statsPanel;
    private final JPanel contentPanel;

    private static final Color BG = new Color(18, 20, 42);
    private static final Color PANEL_BG = new Color(25, 28, 55);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color TEXT_COLOR = new Color(200, 190, 150);

    public SidePanel(GameController controller, GamePanel gamePanel) {
        this.controller = controller;
        this.gamePanel = gamePanel;
        setPreferredSize(new Dimension(232, 0));
        setBackground(BG);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(60, 55, 25)));

        unitInfoPanel = section();
        actionPanel = section();
        statsPanel = section();

        contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        contentPanel.add(unitInfoPanel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(actionPanel);

        JScrollPane scrollPane = new JScrollPane(contentPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        add(scrollPane, BorderLayout.CENTER);

        JPanel statsWrap = new JPanel(new BorderLayout());
        statsWrap.setOpaque(false);
        statsWrap.setBorder(BorderFactory.createEmptyBorder(0, 8, 10, 8));
        statsWrap.add(statsPanel, BorderLayout.CENTER);
        add(statsWrap, BorderLayout.SOUTH);

        update();
    }

    private JPanel section() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 55));
                g2.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 3, 12, 12);
                g2.setPaint(new GradientPaint(0, 0, PANEL_BG.brighter(), 0, getHeight(), PANEL_BG.darker()));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 12, 12);
                g2.setColor(new Color(72, 75, 108));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(13, 12, 13, 12));
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(214, Integer.MAX_VALUE));
        return p;
    }

    public void update() {
        unitInfoPanel.removeAll();
        actionPanel.removeAll();
        statsPanel.removeAll();

        Unit sel = controller.getSelectedUnit();
        actionPanel.setVisible(sel != null);
        if (sel != null) {
            buildUnitInfo(sel);
            buildActionButtons(sel);
        } else {
            Building selectedBuilding = controller.getGameState() == null || controller.getSelectedHex() == null
                    ? null : controller.getGameState().getPlayer().getBuildingAt(controller.getSelectedHex());
            if (selectedBuilding != null) buildBuildingInfo(selectedBuilding);
            else {
                JLabel lbl = makeLabel("Click a unit or building", TEXT_COLOR, Font.ITALIC, 12);
                lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
                unitInfoPanel.add(lbl);
            }
        }
        buildStats();

        fitSection(unitInfoPanel);
        fitSection(actionPanel);
        fitSection(statsPanel);

        revalidate();
        repaint();
    }

    private void buildStats() {
        if (controller.getGameState() == null) return;
        model.Player p = controller.getGameState().getPlayer();
        statsPanel.add(makeCentered(makeLabel("EMPIRE", GOLD, Font.BOLD, 13)));
        statsPanel.add(Box.createVerticalStrut(7));
        statsPanel.add(makeCentered(makeLabel("Territory: " + p.getTerritorySize(), TEXT_COLOR, Font.PLAIN, 11)));
        statsPanel.add(makeCentered(makeLabel("Buildings: " + p.getBuildingCount(), TEXT_COLOR, Font.PLAIN, 11)));
        statsPanel.add(makeCentered(makeLabel("Units: " + p.getUnitCount() + "/" + p.getUnitCap(), TEXT_COLOR, Font.PLAIN, 11)));
        statsPanel.add(makeCentered(makeLabel("Tech: " + p.getResearched().size() + "/6", TEXT_COLOR, Font.PLAIN, 11)));

        model.ProductionQueue queue = p.getProductionQueue();
        if (!queue.isEmpty()) {
            statsPanel.add(Box.createVerticalStrut(6));
            statsPanel.add(makeCentered(makeLabel("PRODUCTION QUEUE", new Color(150, 200, 240), Font.BOLD, 11)));
            int shown = 0;
            for (model.ProductionTask t : queue.getTasks()) {
                if (shown++ >= 4) break;
                statsPanel.add(makeCentered(makeLabel(t.getLabel(), TEXT_COLOR, Font.PLAIN, 10)));
            }
        }

        statsPanel.add(Box.createVerticalStrut(7));
        statsPanel.add(makeCentered(makeLabel("Score: " + controller.getGameState().getCurrentScore(), GOLD, Font.BOLD, 13)));
    }

    private void buildUnitInfo(Unit u) {
        JLabel titleLbl = makeLabel(u.getUnitType().name().replace("_", " "), GOLD, Font.BOLD, 16);
        unitInfoPanel.add(makeCentered(titleLbl));
        unitInfoPanel.add(Box.createVerticalStrut(6));

        unitInfoPanel.add(makeCentered(makeLabel(
                "Pos: (" + u.getPosition().getQ() + ", " + u.getPosition().getR() + ")",
                TEXT_COLOR, Font.PLAIN, 11)));

        unitInfoPanel.add(Box.createVerticalStrut(9));
        unitInfoPanel.add(makeCentered(makeLabel(
                "HP  " + u.getCurrentHp() + " / " + u.getMaxHp(), TEXT_COLOR, Font.PLAIN, 11)));
        unitInfoPanel.add(makeProgressBar(u.getCurrentHp(), u.getMaxHp(),
                new Color(83, 196, 101), new Color(226, 77, 69)));

        unitInfoPanel.add(Box.createVerticalStrut(7));
        unitInfoPanel.add(makeCentered(makeLabel(
                "AP  " + u.getCurrentAP() + " / " + u.getMaxAP(), TEXT_COLOR, Font.PLAIN, 11)));
        unitInfoPanel.add(makeProgressBar(u.getCurrentAP(), u.getMaxAP(),
                new Color(89, 177, 226), new Color(222, 75, 69)));

        unitInfoPanel.add(Box.createVerticalStrut(6));
        unitInfoPanel.add(makeCentered(makeLabel("State: " + u.getState().name(), TEXT_COLOR, Font.PLAIN, 11)));

        if (u instanceof MilitaryUnit) {
            MilitaryUnit military = (MilitaryUnit) u;
            unitInfoPanel.add(Box.createVerticalStrut(5));
            unitInfoPanel.add(makeCentered(makeLabel("Type: " + military.getMilitaryUnitType().name(), GOLD,
                    Font.BOLD, 12)));
            unitInfoPanel.add(makeCentered(makeLabel("Range: " + military.getRange(), TEXT_COLOR, Font.PLAIN, 11)));
        }

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
            unitInfoPanel.add(Box.createVerticalStrut(4));
            if (w.isStationed()) {
                model.Building b = w.getStationedAt();
                unitInfoPanel.add(makeCentered(makeLabel("Working: " + b.getType().name().replace("_", " "),
                        new Color(120, 210, 120), Font.BOLD, 11)));
                unitInfoPanel.add(makeCentered(makeLabel("Crew: " + b.getWorkerCount() + "/" + b.getWorkerCap(),
                        TEXT_COLOR, Font.PLAIN, 11)));
            } else {
                model.Building here = controller.getGameState().getPlayer().getBuildingAt(w.getPosition());
                if (here != null && here.getWorkerCap() > 0) {
                    unitInfoPanel.add(makeCentered(makeLabel("On " + here.getType().name().replace("_", " ")
                            + " (" + here.getWorkerCount() + "/" + here.getWorkerCap() + ")", TEXT_COLOR, Font.PLAIN, 11)));
                } else {
                    unitInfoPanel.add(makeCentered(makeLabel("Idle — stand on a building", TEXT_COLOR, Font.ITALIC, 11)));
                }
            }
        }
    }

    private void buildBuildingInfo(Building b) {
        unitInfoPanel.add(makeCentered(makeLabel(b.getType().name().replace('_', ' '), GOLD, Font.BOLD, 16)));
        unitInfoPanel.add(Box.createVerticalStrut(7));
        unitInfoPanel.add(makeCentered(makeLabel("HP: " + b.getCurrentHp() + " / " + b.getMaxHp(),
                b.isRuined() ? new Color(230, 85, 75) : TEXT_COLOR, Font.BOLD, 12)));
        unitInfoPanel.add(makeCentered(makeLabel(b.isRuined() ? "RUINED" : "ACTIVE",
                b.isRuined() ? new Color(230, 85, 75) : new Color(90, 205, 105), Font.BOLD, 11)));
        if (b.getWorkerCap() > 0)
            unitInfoPanel.add(makeCentered(makeLabel("Workers: " + b.getWorkerCount() + "/" + b.getWorkerCap(),
                    TEXT_COLOR, Font.PLAIN, 11)));
        if (b.isProductionBlocked())
            unitInfoPanel.add(makeCentered(makeLabel("Production blocked", new Color(235, 175, 65), Font.BOLD, 11)));
    }

    private void buildActionButtons(Unit u) {
        actionPanel.add(makeCentered(makeLabel("ACTIONS", GOLD, Font.BOLD, 13)));
        actionPanel.add(Box.createVerticalStrut(8));

        if (u instanceof Explorer) {
            Explorer e = (Explorer) u;
            addActionBtn((e.isAutoExploreMode() ? "Stop Auto-Explore" : "Start Auto-Explore"), true,
                    ev -> { controller.onAutoExploreToggled(); gamePanel.repaintAll(); });
        } else if (u instanceof Builder) {
            addBuildBtn("Lumber Mill", Constants.BuildingType.LUMBER_MILL);
            addBuildBtn("Farm", Constants.BuildingType.FARM);
            addBuildBtn("Stable", Constants.BuildingType.STABLE);
            addBuildBtn("Stone Mine", Constants.BuildingType.STONE_MINE);
            addBuildBtn("Iron Mine", Constants.BuildingType.IRON_MINE);
            addBuildBtn("Township", Constants.BuildingType.TOWNSHIP);
            addBuildBtn("Dock", Constants.BuildingType.DOCK);
            addBuildBtn("Monument", Constants.BuildingType.MONUMENT);
            addBuildBtn("Bazaar", Constants.BuildingType.BAZAAR);
            Builder builder = (Builder) u;
            boolean roadHere = controller.getGameState().getMap().hasRoad(builder.getPosition());
            addActionBtn(roadHere ? "Demolish Road" : "Build Road", true,
                    ev -> {
                        if (roadHere) {
                            if (confirmDemolition("road")) controller.onDemolishRoad();
                        } else controller.onBuildRoad();
                        gamePanel.repaintAll();
                    });
            java.util.List<model.HexCoordinate> demolitionSites = new java.util.ArrayList<>();
            demolitionSites.add(builder.getPosition());
            demolitionSites.addAll(builder.getPosition().findNeighbours());
            for (model.HexCoordinate site : demolitionSites) {
                Building building = controller.getGameState().getPlayer().getBuildingAt(site);
                if (building == null || building.getType() == Constants.BuildingType.TOWN_HALL) continue;
                boolean enabled = controller.getGameState().getInfrastructureService()
                        .canDemolishBuilding(builder, site);
                String label = "Demolish " + building.getType().name().replace('_', ' ')
                        + " " + site.getQ() + "," + site.getR();
                addActionBtn(label, enabled, ev -> {
                    if (confirmDemolition(building.getType().name().replace('_', ' '))) {
                        controller.onDemolishBuilding(site);
                    }
                    gamePanel.repaintAll();
                });
            }
            for (model.HexCoordinate neighbour : builder.getPosition().findNeighbours()) {
                if (!controller.getGameState().getMap().containsCoordinate(neighbour)) continue;
                if (controller.getGameState().getMap().hasRiverBetween(builder.getPosition(), neighbour)) {
                    boolean bridgeExists = controller.getGameState().getMap()
                            .hasBridgeBetween(builder.getPosition(), neighbour);
                    addActionBtn((bridgeExists ? "Demolish bridge " : "Build bridge ")
                                    + neighbour.getQ() + "," + neighbour.getR(), true,
                            ev -> {
                                if (bridgeExists) {
                                    if (confirmDemolition("bridge")) controller.onDemolishBridge(neighbour);
                                } else controller.onBuildBridge(neighbour);
                                gamePanel.repaintAll();
                            });
                }
                boolean wallExists = controller.getGameState().getMap()
                        .hasWallBetween(builder.getPosition(), neighbour);
                boolean wallEnabled = wallExists
                        ? controller.getGameState().getInfrastructureService()
                        .canDemolishWall(builder, builder.getPosition(), neighbour)
                        : controller.getGameState().getInfrastructureService()
                        .canBuildWall(builder, builder.getPosition(), neighbour);
                String wallLabel = wallExists ? "Demolish wall " : "Build wall (10W 10S) ";
                addActionBtn(wallLabel + neighbour.getQ() + "," + neighbour.getR(), wallEnabled,
                        ev -> {
                            if (wallExists) {
                                if (confirmDemolition("wall")) controller.onDemolishWall(neighbour);
                            } else controller.onBuildWall(neighbour);
                            gamePanel.repaintAll();
                        });
                }
        } else if (u instanceof Worker) {
            Worker w = (Worker) u;
            boolean canStation = controller.canStationHere();
            addActionBtn(w.isStationed() ? "Re-station Here" : "Station Here", canStation,
                    ev -> { controller.onStationWorker(); gamePanel.repaintAll(); });
            addActionBtn("Leave Building", w.isStationed(),
                    ev -> { controller.onUnstationWorker(); gamePanel.repaintAll(); });
        } else if (u instanceof BorderExpander) {
            BorderExpander be = (BorderExpander) u;
            addActionBtn("Expand Border", be.canExpand(),
                    ev -> { controller.onExpandBorder(); gamePanel.repaintAll(); });
        }

        actionPanel.add(Box.createVerticalStrut(6));
        addActionBtn("Deselect (ESC)", true, ev -> { controller.deselectUnit(); gamePanel.repaintAll(); });
    }

    private boolean confirmDemolition(String target) {
        return JOptionPane.showConfirmDialog(this,
                "Demolish this " + target + "? No resources will be refunded.",
                "Confirm demolition", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    private void addBuildBtn(String label, Constants.BuildingType type) {
        boolean enabled = controller.canBuildType(type);
        addActionBtn("Build " + label, enabled,
                ev -> { controller.onBuildChosen(type); gamePanel.repaintAll(); });
    }

    private void addActionBtn(String text, boolean enabled, ActionListener al) {
        final boolean en = enabled;
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
                if (!en) {
                    g2.setColor(new Color(28, 30, 45));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.setColor(new Color(55, 55, 70));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                    g2.setColor(new Color(95, 95, 110));
                } else {
                    g2.setColor(hovered ? new Color(50, 60, 100) : new Color(35, 40, 75));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.setColor(hovered ? GOLD : new Color(100, 90, 50));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                    g2.setColor(TEXT_COLOR);
                }
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
        btn.setEnabled(en);
        btn.setMaximumSize(new Dimension(190, 32));
        btn.setPreferredSize(new Dimension(190, 32));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(en ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        if (en) btn.addActionListener(al);
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
        lbl.setFont(new Font(style == Font.BOLD && size >= 13 ? "Georgia" : "SansSerif", style, size));
        return lbl;
    }

    private JPanel makeProgressBar(int value, int max, Color healthy, Color critical) {
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float pct = max > 0 ? Math.max(0f, Math.min(1f, value / (float) max)) : 0f;
                g2.setColor(new Color(10, 12, 27, 220));
                g2.fillRoundRect(0, 1, getWidth(), getHeight() - 2, 7, 7);
                Color fill = pct > .5f ? healthy : pct > .22f ? new Color(225, 174, 54) : critical;
                g2.setPaint(new GradientPaint(0, 0, fill.brighter(), getWidth(), 0, fill.darker()));
                g2.fillRoundRect(0, 1, (int) (getWidth() * pct), getHeight() - 2, 7, 7);
                g2.setColor(new Color(220, 225, 240, 80));
                g2.drawRoundRect(0, 1, getWidth() - 1, getHeight() - 3, 7, 7);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(185, 9));
        bar.setMaximumSize(new Dimension(185, 9));
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);
        return bar;
    }

    private void fitSection(JPanel panel) {
        if (!panel.isVisible()) return;
        panel.setPreferredSize(null);
        panel.setMaximumSize(new Dimension(214, Integer.MAX_VALUE));
        panel.invalidate();
        int height = panel.getLayout().preferredLayoutSize(panel).height;
        panel.setMaximumSize(new Dimension(214, height));
    }
}
