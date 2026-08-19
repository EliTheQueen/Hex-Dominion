package view;

import controller.GameController;
import model.Constants;
import model.tribe.*;
import model.tribe.mission.TribeMission;

import javax.swing.*;
import java.awt.*;

/** Diplomacy, tribe trade, and mission lifecycle in one presentation-friendly view. */
public final class TribePanel extends JDialog {
    private final GameController controller;
    private final JPanel list = new JPanel();
    public TribePanel(GameController controller, JFrame parent) {
        super(parent, "Tribes & Missions", true); this.controller = controller;
        setSize(900, 650); setLocationRelativeTo(parent); setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(11, 14, 29));
        JLabel title = new JLabel("TRIBAL COUNCIL", SwingConstants.CENTER);
        title.setForeground(new Color(212, 175, 55)); title.setFont(new Font("Georgia", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(15, 8, 10, 8)); add(title, BorderLayout.NORTH);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS)); list.setBackground(new Color(11, 14, 29));
        JScrollPane scroll = new JScrollPane(list); scroll.setBorder(null); scroll.getViewport().setBackground(list.getBackground());
        add(scroll, BorderLayout.CENTER); rebuild();
    }

    private void rebuild() {
        list.removeAll();
        for (Tribe tribe : controller.getGameState().getTribes()) list.add(card(tribe));
        list.revalidate(); list.repaint();
    }

    private JPanel card(Tribe tribe) {
        JPanel card = new JPanel(new BorderLayout(10, 7)); card.setMaximumSize(new Dimension(850, 150));
        card.setBackground(new Color(24, 30, 52));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color(tribe), 2, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        String identity = tribe.isDiscovered() ? tribe.getName() + "  •  " + tribe.getType().name()
                : "UNDISCOVERED TRIBE";
        JLabel head = new JLabel(identity); head.setForeground(color(tribe)); head.setFont(new Font("Georgia", Font.BOLD, 15));
        card.add(head, BorderLayout.NORTH);
        if (!tribe.isDiscovered()) {
            JLabel fog = new JLabel("Explore the map to reveal this camp and its benefits."); fog.setForeground(Color.GRAY);
            card.add(fog, BorderLayout.CENTER); return card;
        }
        TribeMission mission = controller.getGameState().getMission(tribe);
        String info = "<html>Relation: <b>" + tribe.getRelation().getScore() + "  " + tribe.getRelation().getStatus()
                + "</b> • Camp HP " + tribe.getCurrentHp() + "/" + tribe.getMaxHp() + " • Guards " + tribe.getGuardCount()
                + "<br>Benefit: " + benefit(tribe.getType())
                + (mission == null ? "<br>Mission: none" : "<br>Mission: <b>" + mission.getTitle() + "</b> — "
                + mission.getDescription() + " • " + mission.getStatus() + " • " + mission.getRemainingTurns() + " turns")
                + "</html>";
        JLabel details = new JLabel(info); details.setForeground(new Color(215, 216, 220)); card.add(details, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2)); actions.setOpaque(false);
        add(actions, "GIFT 10 FOOD", !tribe.getRelation().isEnemy(), "Enemies reject gifts",
                () -> controller.giftTribe(tribe, Constants.ResourceType.FOOD, 10));
        add(actions, "TRADE 10", tribe.canTradeAt(controller.getGameState().getCurrentTurn())
                        && tribe.getType() != TribeType.WARRIOR, "Requires Friendly/Allied and one trade per turn",
                () -> controller.tradeWithTribe(tribe, Constants.ResourceType.WOOD, tradeOutput(tribe), 10));
        add(actions, "MISSION", mission == null && tribe.getRelation().getScore() >= 20, "Requires Friendly relation",
                () -> controller.requestMission(tribe));
        add(actions, "TURN IN", mission != null && mission.getStatus().name().equals("READY_TO_TURN_IN"), "Objective not complete",
                () -> controller.turnInMission(tribe));
        add(actions, "CANCEL", mission != null, "No active mission",
                () -> controller.cancelMission(tribe));
        add(actions, "ALLIANCE", tribe.getRelation().getScore() >= 70 && !controller.getGameState().isAllied(tribe),
                "Requires 70 relation and compatible alliances", () -> controller.requestAlliance(tribe));
        add(actions, tribe.getRelation().isEnemy() ? "PEACE" : "DECLARE WAR", true, "",
                () -> { if (tribe.getRelation().isEnemy()) controller.requestPeace(tribe); else controller.declareWar(tribe); });
        card.add(actions, BorderLayout.SOUTH); return card;
    }

    private void add(JPanel panel, String text, boolean enabled, String reason, Runnable action) {
        JButton button = new JButton(text); button.setEnabled(enabled); button.setFont(new Font("SansSerif", Font.BOLD, 9));
        button.setToolTipText(enabled ? text : reason); button.addActionListener(e -> { action.run(); rebuild(); }); panel.add(button);
    }

    private Constants.ResourceType tradeOutput(Tribe tribe) {
        return tribe.getType() == TribeType.FARMER || tribe.getType() == TribeType.COASTAL ? Constants.ResourceType.FOOD
                : tribe.getType() == TribeType.MOUNTAIN ? Constants.ResourceType.STONE : Constants.ResourceType.IRON;
    }
    private Color color(Tribe tribe) {
        if (!tribe.isDiscovered()) return new Color(75, 78, 90);
        switch (tribe.getType()) {
            case FARMER: return new Color(116, 185, 92); case WARRIOR: return new Color(205, 87, 72);
            case MERCHANT: return new Color(216, 169, 69); case MOUNTAIN: return new Color(155, 163, 177);
            case COASTAL: return new Color(75, 165, 195); default: return Color.GRAY;
        }
    }
    private String benefit(TribeType type) {
        switch (type) {
            case FARMER: return "Food trade at 75%"; case WARRIOR: return "Military counsel";
            case MERCHANT: return "Any resource trade at 80%"; case MOUNTAIN: return "Stone/Iron trade at 75%";
            case COASTAL: return "Food trade at 75%"; default: return "";
        }
    }
}
