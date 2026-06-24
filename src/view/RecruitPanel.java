package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;

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
import model.ResourceAmount;

/** Modal dialog for recruiting new units near the Town Hall. */
public class RecruitPanel extends JDialog {
    private final GameController controller;
    private static final Color BG = new Color(10, 12, 28);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color TEXT = new Color(190, 175, 120);

    public RecruitPanel(GameController controller, JFrame parent) {
        super(parent, "Recruit Unit", true);
        this.controller = controller;
        setSize(520, 380);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("RECRUIT A UNIT", SwingConstants.CENTER);
        title.setForeground(GOLD);
        title.setFont(new Font("Georgia", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(14, 0, 6, 0));
        add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setBackground(BG);
        grid.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        for (Constants.UnitType type : Constants.UnitType.values()) {
            grid.add(buildUnitCard(type));
        }
        add(grid, BorderLayout.CENTER);

        JButton close = new JButton("CLOSE");
        close.setBackground(new Color(70, 35, 35));
        close.setForeground(Color.WHITE);
        close.setFocusPainted(false);
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.setBackground(BG);
        south.add(close);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel buildUnitCard(Constants.UnitType type) {
        ResourceAmount cost = Constants.UNIT_COST.get(type);
        boolean canAfford = controller.canRecruit(type);

        JPanel card = new JPanel();
        card.setBackground(canAfford ? new Color(25, 30, 55) : new Color(20, 20, 30));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(canAfford ? GOLD : new Color(60, 60, 80), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel(type.name().replace("_", " "));
        nameLbl.setForeground(canAfford ? Color.WHITE : new Color(110, 110, 130));
        nameLbl.setFont(new Font("Georgia", Font.BOLD, 14));
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(nameLbl);

        JLabel descLbl = new JLabel(describe(type));
        descLbl.setForeground(TEXT);
        descLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        descLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(Box.createVerticalStrut(3));
        card.add(descLbl);

        if (cost != null) {
            String costStr = cost.get(Constants.ResourceType.FOOD) + "F  "
                    + cost.get(Constants.ResourceType.WOOD) + "W"
                    + (cost.get(Constants.ResourceType.STONE) > 0 ? "  " + cost.get(Constants.ResourceType.STONE) + "S" : "")
                    + (cost.get(Constants.ResourceType.IRON) > 0 ? "  " + cost.get(Constants.ResourceType.IRON) + "I" : "");
            JLabel costLbl = new JLabel("Cost: " + costStr);
            costLbl.setForeground(new Color(180, 160, 100));
            costLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
            costLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(Box.createVerticalStrut(5));
            card.add(costLbl);
        }

        if (canAfford) {
            JButton btn = new JButton("RECRUIT");
            btn.setFont(new Font("SansSerif", Font.BOLD, 11));
            btn.setBackground(new Color(40, 90, 60));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.addActionListener(e -> {
                controller.onRecruitUnit(type);
                dispose();
            });
            card.add(Box.createVerticalStrut(6));
            card.add(btn);
        }
        return card;
    }

    private String describe(Constants.UnitType type) {
        switch (type) {
            case EXPLORER: return "Vision 3, fast scout";
            case BUILDER: return "Builds, 3 charges";
            case WORKER: return "Staffs buildings";
            case BORDER_EXPANDER: return "Claims territory";
            default: return "";
        }
    }
}
