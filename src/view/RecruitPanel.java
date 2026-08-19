package view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
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
import model.ResourceAmount;
import model.military.MilitaryUnitType;

/** Modal dialog for recruiting new units near the Town Hall. */
public class RecruitPanel extends JDialog {
    private final GameController controller;
    private static final Color BG = new Color(10, 12, 28);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color TEXT = new Color(190, 175, 120);

    public RecruitPanel(GameController controller, JFrame parent) {
        super(parent, "Recruit Unit", true);
        this.controller = controller;
        setSize(760, 560);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("RECRUIT A UNIT", SwingConstants.CENTER);
        title.setForeground(GOLD);
        title.setFont(new Font("Georgia", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(14, 0, 6, 0));
        add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 12));
        grid.setBackground(BG);
        grid.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        for (Constants.UnitType type : Constants.UnitType.values()) {
            if (Constants.UNIT_COST.containsKey(type)) grid.add(buildUnitCard(type));
        }
        for (MilitaryUnitType type : MilitaryUnitType.values()) {
            grid.add(buildMilitaryCard(type));
        }
        add(grid, BorderLayout.CENTER);

        JButton close = styledButton("CLOSE", new Color(70, 35, 35));
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.setBackground(BG);
        south.add(close);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel buildMilitaryCard(MilitaryUnitType type) {
        boolean unlocked = controller.getGameState().canRecruitMilitaryUnit(
                type, controller.getGameState().getTownHallPos(), controller.getGameState().getTownHall().getLevel());
        boolean affordable = controller.getGameState().getPlayer().canAfford(type.getCost());
        boolean slotFree = !controller.getGameState().getTownHall().getCommandSlot().isBusy();
        boolean enabled = unlocked && affordable && slotFree;
        JPanel card = new JPanel(); card.setBackground(enabled ? new Color(28, 34, 58) : new Color(20, 20, 30));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(enabled ? GOLD : new Color(65, 65, 80), 1),
                BorderFactory.createEmptyBorder(10, 8, 10, 8)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(type.name()); name.setForeground(enabled ? Color.WHITE : new Color(110, 110, 130));
        name.setFont(new Font("Georgia", Font.BOLD, 13)); name.setAlignmentX(Component.CENTER_ALIGNMENT); card.add(name);
        model.military.MilitaryUnit sample = new model.military.UnitFactory().createUnit(type,
                controller.getGameState().getTownHallPos());
        JLabel stats = new JLabel("HP " + sample.getMaxHp() + " • AP " + sample.getMaxAP()
                + " • RNG " + sample.getRange());
        stats.setForeground(TEXT); stats.setFont(new Font("SansSerif", Font.PLAIN, 10));
        stats.setAlignmentX(Component.CENTER_ALIGNMENT); card.add(Box.createVerticalStrut(5)); card.add(stats);
        JLabel cost = new JLabel("Cost: " + costText(type.getCost()) + " • " + type.getTrainingTurns() + " turns");
        cost.setForeground(TEXT); cost.setFont(new Font("SansSerif", Font.PLAIN, 10)); cost.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(Box.createVerticalStrut(5)); card.add(cost);
        JButton train = styledButton("TRAIN", new Color(65, 72, 112)); train.setEnabled(enabled);
        train.setToolTipText(enabled ? "Use the Town Hall command slot" : !unlocked ? "Level, Stable, cap, or stack requirement not met"
                : !affordable ? "Insufficient resources" : "Town Hall command slot occupied");
        train.setAlignmentX(Component.CENTER_ALIGNMENT); train.addActionListener(e -> { controller.onRecruitMilitary(type); dispose(); });
        card.add(Box.createVerticalStrut(8)); card.add(train); return card;
    }

    private String costText(ResourceAmount cost) {
        return cost.get(Constants.ResourceType.FOOD) + "F " + cost.get(Constants.ResourceType.WOOD) + "W "
                + cost.get(Constants.ResourceType.STONE) + "S " + cost.get(Constants.ResourceType.IRON) + "I";
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
            JButton btn = styledButton("RECRUIT", new Color(40, 90, 60));
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

    private JButton styledButton(String text, final Color bg) {
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
                FontMetrics fm = g2.getFontMetrics();
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
        btn.setPreferredSize(new java.awt.Dimension(110, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
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
