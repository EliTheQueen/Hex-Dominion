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
import model.Constants;
import model.Player;
import model.ResourceAmount;

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
        setSize(620, 500);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("TECHNOLOGY TREE", SwingConstants.CENTER);
        title.setForeground(GOLD);
        title.setFont(new Font("Georgia", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(15, 0, 8, 0));
        add(title, BorderLayout.NORTH);

        JPanel techGrid = new JPanel(new GridLayout(3, 2, 14, 14));
        techGrid.setBackground(BG);
        techGrid.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        Player player = controller.getGameState().getPlayer();
        for (Constants.TechnologyType tech : Constants.TechnologyType.values()) {
            techGrid.add(buildTechCard(tech, player));
        }
        add(techGrid, BorderLayout.CENTER);

        JButton closeBtn = makeButton("CLOSE", new Color(70, 35, 35));
        closeBtn.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.setBackground(BG);
        south.add(closeBtn);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel buildTechCard(Constants.TechnologyType tech, Player player) {
        boolean researched = player.hasResearched(tech);
        boolean available = player.canResearch(tech);

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

            if (available) {
                JButton resBtn = makeButton("RESEARCH", new Color(30, 80, 140));
                resBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
                resBtn.addActionListener(e -> {
                    controller.onResearchChosen(tech);
                    dispose();
                });
                card.add(Box.createVerticalStrut(6));
                card.add(resBtn);
            }
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

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
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
