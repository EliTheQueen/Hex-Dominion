package view;

import controller.GameController;
import model.Constants;
import model.ResourceAmount;
import model.tribe.Tribe;
import model.tribe.mission.KillCountObjective;
import model.tribe.mission.TribeMission;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Persistent in-game list for accepted missions and newly offered missions. */
public final class MissionHudPanel extends JPanel {
    private final GameController controller;
    private final JPanel content = new JPanel();

    public MissionHudPanel(GameController controller) {
        this.controller = controller;
        setPreferredSize(new Dimension(235, 0));
        setLayout(new BorderLayout());
        setBackground(new Color(14, 17, 33));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(70, 66, 92)));
        JLabel title = new JLabel("MISSIONS", SwingConstants.CENTER);
        title.setForeground(new Color(212, 175, 55));
        title.setFont(new Font("Georgia", Font.BOLD, 15));
        title.setBorder(BorderFactory.createEmptyBorder(12, 6, 9, 6));
        add(title, BorderLayout.NORTH);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(getBackground());
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(getBackground());
        add(scroll, BorderLayout.CENTER);
        update();
    }

    public void update() {
        content.removeAll();
        int count = 0;
        if (controller.getGameState() != null) {
            for (Tribe tribe : controller.getGameState().getDiscoveredTribes()) {
                TribeMission active = controller.getGameState().getMission(tribe);
                if (active != null) { content.add(missionCard(tribe, active, false)); count++; }
                TribeMission offered = controller.getGameState().getOfferedMission(tribe);
                if (offered != null) { content.add(missionCard(tribe, offered, true)); count++; }
            }
        }
        if (count == 0) {
            JLabel empty = new JLabel("<html><div style='width:180px;text-align:center'>No active missions.<br>"
                    + "Friendly tribes offer one every 5 turns.</div></html>");
            empty.setForeground(new Color(135, 140, 160));
            empty.setBorder(BorderFactory.createEmptyBorder(22, 12, 12, 12));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(empty);
        }
        content.revalidate();
        content.repaint();
    }

    private JPanel missionCard(Tribe tribe, TribeMission mission, boolean offered) {
        JPanel card = new JPanel(new BorderLayout());
        card.setMaximumSize(new Dimension(215, 165));
        card.setBackground(new Color(26, 31, 53));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(offered ? new Color(92, 168, 204) : new Color(116, 104, 66)),
                BorderFactory.createEmptyBorder(8, 9, 8, 9)));
        String progress = mission.getObjective() instanceof KillCountObjective kills
                ? kills.getCurrentKills() + "/" + kills.getRequiredKills() + " kills"
                : mission.getObjective().isCompleted() ? "complete" : "in progress";
        String text = "<html><b>" + tribe.getName() + "</b> • " + (offered ? "OFFERED" : mission.getStatus())
                + "<br>" + mission.getTitle()
                + "<br>" + mission.getObjective().getDescription()
                + "<br>Progress: " + progress
                + "<br>Deadline: " + mission.getRemainingTurns() + "/" + mission.getTotalTurns()
                + "<br>Reward: " + reward(mission) + "</html>";
        JLabel label = new JLabel(text);
        label.setForeground(new Color(210, 213, 224));
        label.setFont(new Font("SansSerif", Font.PLAIN, 10));
        card.add(label);
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        return card;
    }

    private String reward(TribeMission mission) {
        ResourceAmount amount = mission.getReward().getResources();
        List<String> parts = new ArrayList<>();
        for (Constants.ResourceType type : Constants.ResourceType.values()) {
            if (amount.get(type) > 0) parts.add(amount.get(type) + " " + type.name());
        }
        if (mission.getReward().getRelationReward() > 0) {
            parts.add("+" + mission.getReward().getRelationReward() + " relation");
        }
        if (!mission.getReward().getEffects().isEmpty()) {
            parts.add(mission.getReward().getEffects().toString().replace('_', ' '));
        }
        return parts.isEmpty() ? "none" : String.join(", ", parts);
    }
}
