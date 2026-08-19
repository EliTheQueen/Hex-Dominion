package view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import controller.GameController;
import model.ActionAvailability;
import model.Constants;
import model.Player;
import model.ResourceAmount;
import model.townhall.ProductionCommand;
import model.technology.TechnologyType;

/** Modal technology tree. Lets the player spend resources to research upgrades. */
public class TechPanel extends JDialog {
    private final GameController controller;

    private static final Color BG = new Color(10, 12, 28);
    private static final Color PANEL_BG = new Color(18, 22, 45);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color TEXT = new Color(200, 190, 150);
    private static final Color RESEARCHED = new Color(60, 190, 60);
    private static final Color AVAILABLE = new Color(100, 160, 220);
    private static final Color LOCKED = new Color(110, 110, 130);

    public TechPanel(GameController controller, JFrame parent) {
        super(parent, "Technology Research", true);
        this.controller = controller;
        setSize(780, 680);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("TECHNOLOGY TREE", SwingConstants.CENTER);
        title.setForeground(GOLD);
        title.setFont(new Font("Georgia", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(15, 0, 8, 0));
        add(title, BorderLayout.NORTH);

        JPanel techGrid = new JPanel(new GridLayout(3, 3, 14, 14));
        techGrid.setBackground(BG);
        techGrid.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        Player player = controller.getGameState().getPlayer();
        for (Constants.TechnologyType tech : Constants.TechnologyType.values()) {
            techGrid.add(buildTechCard(tech, player));
        }
        for (TechnologyType tech : TechnologyType.values()) {
            techGrid.add(buildPhaseTwoTechCard(tech));
        }
        add(techGrid, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setBackground(BG);
        ProductionCommand active = controller.getGameState().getTownHall().getCommandSlot().getActiveCommand();
        JLabel hall = new JLabel("Town Hall: " + controller.getGameState().getTownHall().getLevel().name().replace('_', ' ')
                + (active == null ? "  •  command slot ready" : "  •  " + active.getClass().getSimpleName()
                + " (" + active.getRemainingTurns() + " turns)"));
        hall.setForeground(TEXT);
        south.add(hall);
        JButton upgradeBtn = makeButton("UPGRADE TOWN HALL", new Color(48, 78, 100));
        ActionAvailability upgradeAvailability = controller.getTownHallUpgradeAvailability();
        upgradeBtn.setName("tech-upgrade-town-hall");
        upgradeBtn.setEnabled(upgradeAvailability.isEnabled());
        upgradeBtn.setToolTipText(upgradeAvailability.getReason());
        upgradeBtn.addActionListener(e -> { controller.onTownHallUpgrade(); dispose(); });
        south.add(upgradeBtn);
        if (active != null) {
            JButton cancelBtn = makeButton("CANCEL (NO REFUND)", new Color(110, 48, 42));
            cancelBtn.addActionListener(e -> { controller.onCancelTownHallCommand(); dispose(); });
            south.add(cancelBtn);
        }
        JButton closeBtn = makeButton("CLOSE", new Color(70, 35, 35));
        closeBtn.addActionListener(e -> dispose());
        south.add(closeBtn);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel buildPhaseTwoTechCard(TechnologyType tech) {
        boolean researched = controller.getGameState().getPhaseTwoTechnologies().has(tech);
        boolean queued = controller.getGameState().getPhaseTwoTechnologies().isQueued(tech);
        boolean levelOk = tech.isUnlockedAt(controller.getGameState().getTownHall().getLevel());
        JPanel card = new JPanel();
        card.setBackground(PANEL_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(researched ? RESEARCHED : levelOk ? AVAILABLE : LOCKED, 2, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JLabel name = new JLabel(tech.name().replace('_', ' '));
        name.setForeground(researched ? RESEARCHED : Color.WHITE);
        name.setFont(new Font("Georgia", Font.BOLD, 13));
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(name);
        JLabel detail = new JLabel("<html><center>" + phaseTwoDescription(tech) + "<br>Cost: "
                + buildCostStr(tech.getCost()) + " • " + tech.getResearchTurns() + " turns</center></html>");
        detail.setForeground(TEXT);
        detail.setFont(new Font("SansSerif", Font.PLAIN, 10));
        detail.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(Box.createVerticalStrut(6));
        card.add(detail);
        if (!researched && !queued) {
            JButton button = makeButton("RESEARCH", new Color(30, 80, 140));
            ActionAvailability availability = controller.getPhaseTwoResearchAvailability(tech);
            button.setName("tech-phase2-" + tech.name().toLowerCase());
            button.setEnabled(availability.isEnabled());
            button.setToolTipText(availability.getReason());
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.addActionListener(e -> { controller.onPhaseTwoResearch(tech); dispose(); });
            card.add(Box.createVerticalStrut(8));
            card.add(button);
        } else {
            JLabel status = new JLabel(researched ? "RESEARCHED" : "IN PROGRESS");
            status.setForeground(researched ? RESEARCHED : AVAILABLE);
            status.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(status);
        }
        return card;
    }

    private String phaseTwoDescription(TechnologyType tech) {
        switch (tech) {
            case SAILING: return "Land units may enter sea";
            case STEEL_TOOLS: return "+50% stone and iron production";
            case DEFENSIVE_ARCHITECTURE: return "Fortify Town Hall and add walls";
            default: return "";
        }
    }

    private JPanel buildTechCard(Constants.TechnologyType tech, Player player) {
        boolean researched = player.hasResearched(tech);
        ActionAvailability availabilityResult = controller.getLegacyResearchAvailability(tech);
        boolean available = availabilityResult.isEnabled();

        final Color borderColor = researched ? RESEARCHED : (available ? AVAILABLE : LOCKED);
        final Color bgColor = researched ? new Color(20, 50, 20)
                : (available ? PANEL_BG : new Color(20, 20, 30));

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel nameLbl = new JLabel(tech.name().replace("_", " "));
        nameLbl.setForeground(researched ? RESEARCHED : (available ? Color.WHITE : LOCKED));
        nameLbl.setFont(new Font("Georgia", Font.BOLD, 14));
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(nameLbl);
        card.add(Box.createVerticalStrut(4));

        JLabel descLbl = new JLabel("<html><center>" + describe(tech) + "</center></html>");
        descLbl.setForeground(TEXT);
        descLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        descLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(descLbl);
        card.add(Box.createVerticalStrut(5));

        String status = researched ? "Researched" : (available ? "Available" : "Locked");
        JLabel statusLbl = new JLabel(status);
        statusLbl.setForeground(borderColor);
        statusLbl.setFont(new Font("SansSerif", Font.ITALIC, 11));
        statusLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(statusLbl);

        if (!researched) {
            ResourceAmount cost = player.getTechCost(tech);
            JLabel costLbl = new JLabel("Cost: " + buildCostStr(cost));
            costLbl.setForeground(TEXT);
            costLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
            costLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(Box.createVerticalStrut(4));
            card.add(costLbl);

            JButton resBtn = makeButton("RESEARCH", new Color(30, 80, 140));
            resBtn.setName("tech-legacy-" + tech.name().toLowerCase());
            resBtn.setEnabled(availabilityResult.isEnabled());
            resBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            resBtn.setToolTipText(availabilityResult.getReason());
            resBtn.addActionListener(e -> {
                controller.onResearchChosen(tech);
                dispose();
            });
            card.add(Box.createVerticalStrut(6));
            card.add(resBtn);
        }
        return card;
    }

    private String describe(Constants.TechnologyType tech) {
        switch (tech) {
            case STORAGE_I: return "+50 storage capacity";
            case STORAGE_II: return "+100 more capacity";
            case STONE_MINING: return "Unlocks Stone Mine";
            case IRON_MINING: return "Unlocks Iron Mine";
            case PROFESSIONAL_TOOLS: return "+50% production";
            case TOWNSHIP: return "Unlocks Township";
            default: return "";
        }
    }

    private JButton makeButton(String text, final Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(GOLD);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private String buildCostStr(ResourceAmount cost) {
        StringBuilder sb = new StringBuilder();
        if (cost.get(Constants.ResourceType.FOOD) > 0) sb.append(cost.get(Constants.ResourceType.FOOD)).append("F ");
        if (cost.get(Constants.ResourceType.WOOD) > 0) sb.append(cost.get(Constants.ResourceType.WOOD)).append("W ");
        if (cost.get(Constants.ResourceType.STONE) > 0) sb.append(cost.get(Constants.ResourceType.STONE)).append("S ");
        if (cost.get(Constants.ResourceType.IRON) > 0) sb.append(cost.get(Constants.ResourceType.IRON)).append("I ");
        return sb.length() > 0 ? sb.toString().trim() : "Free";
    }
}
