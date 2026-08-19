package view;

import controller.GameController;
import model.Constants;
import model.ResourceAmount;
import model.military.MilitaryUnit;
import model.tribe.*;
import model.tribe.mission.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Complete tribe interaction surface with visibility masking and state-aware reasons. */
public final class TribePanel extends JDialog {
    private final GameController controller;
    private final JPanel list = new JPanel();
    private final Tribe focusedTribe;
    private int displayedTribeCount;

    public TribePanel(GameController controller, JFrame parent) {
        this(controller, parent, null);
    }

    public TribePanel(GameController controller, JFrame parent, Tribe focusedTribe) {
        super(parent, "Tribes & Missions", true);
        this.controller = controller;
        this.focusedTribe = focusedTribe;
        setSize(980, 700);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(11, 14, 29));
        JLabel title = new JLabel("TRIBAL COUNCIL", SwingConstants.CENTER);
        title.setForeground(new Color(212, 175, 55));
        title.setFont(new Font("Georgia", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(15, 8, 10, 8));
        add(title, BorderLayout.NORTH);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(new Color(11, 14, 29));
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(list.getBackground());
        add(scroll, BorderLayout.CENTER);
        rebuild();
    }

    private void rebuild() {
        list.removeAll();
        List<Tribe> discovered = new ArrayList<>(controller.getGameState().getDiscoveredTribes());
        if (focusedTribe != null && discovered.remove(focusedTribe)) discovered.add(0, focusedTribe);
        displayedTribeCount = discovered.size();
        if (discovered.isEmpty()) {
            JLabel empty = new JLabel("No tribes discovered. Explore the map to reveal camps.", SwingConstants.CENTER);
            empty.setForeground(new Color(150, 154, 170));
            empty.setBorder(BorderFactory.createEmptyBorder(80, 20, 20, 20));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            list.add(empty);
        } else {
            for (Tribe tribe : discovered) list.add(card(tribe));
        }
        list.revalidate();
        list.repaint();
    }

    private JPanel card(Tribe tribe) {
        boolean visible = controller.getGameState().isTribeCurrentlyVisible(tribe);
        JPanel card = new JPanel(new BorderLayout(10, 7));
        card.setMaximumSize(new Dimension(930, 255));
        card.setBackground(new Color(24, 30, 52));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color(tribe), 2, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        String identity = tribe.getName() + "  •  " + tribe.getType().name()
                + "  •  Camp " + coordinate(tribe);
        JLabel head = new JLabel(identity);
        head.setForeground(color(tribe));
        head.setFont(new Font("Georgia", Font.BOLD, 15));
        card.add(head, BorderLayout.NORTH);

        TribeMission mission = controller.getGameState().getMission(tribe);
        StringBuilder info = new StringBuilder("<html>");
        info.append("Relation: <b>").append(tribe.getRelation().getScore()).append("  ")
                .append(tribe.getRelation().getStatus()).append("</b>");
        if (tribe.isOutpost()) {
            info.append(" • <b>OUTPOST — camp defeated</b>");
        } else if (visible) {
            info.append(" • Camp HP ").append(tribe.getCurrentHp()).append("/").append(tribe.getMaxHp())
                    .append(" • Defenders ").append(tribe.getGuardCount());
            if (controller.getGameState().hasTradeOffer(tribe)) info.append(" • <b>Trade offer available</b>");
        } else {
            info.append("<br><i>Camp outside current sight — HP, defenders, and activity are hidden.</i>");
        }
        info.append("<br>Trade terms: ").append(tradeBenefit(tribe.getType()));
        info.append("<br>Alliance benefit: ")
                .append(controller.getGameState().getAllianceBenefitDescription(tribe));
        if (controller.getGameState().isPeacePending(tribe)) {
            info.append("<br>Peace: <b>pending ")
                    .append(controller.getGameState().getPeaceProgress(tribe)).append("/")
                    .append(controller.getGameState().getRequiredPeaceTurns())
                    .append(" consecutive peaceful turns</b>");
        }
        info.append(missionDetails(mission)).append("</html>");
        JLabel details = new JLabel(info.toString());
        details.setForeground(new Color(215, 216, 220));
        card.add(details, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        actions.setOpaque(false);
        addAction(actions, "GIFT…", availability(tribe, TribeAction.GIFT), () -> showGiftDialog(tribe));
        addAction(actions, "TRADE…", availability(tribe, TribeAction.TRADE), () -> showTradeDialog(tribe));
        addAction(actions, "MISSION", availability(tribe, TribeAction.REQUEST_MISSION),
                () -> controller.requestMission(tribe));
        addAction(actions, "TURN IN", availability(tribe, TribeAction.TURN_IN_MISSION),
                () -> controller.turnInMission(tribe));
        addAction(actions, "CANCEL", availability(tribe, TribeAction.CANCEL_MISSION),
                () -> controller.cancelMission(tribe));
        addAction(actions, "ALLIANCE", availability(tribe, TribeAction.REQUEST_ALLIANCE),
                () -> controller.requestAlliance(tribe));
        addAction(actions, "BENEFIT DETAILS", availability(tribe, TribeAction.VIEW_ALLIANCE_BENEFIT),
                () -> showBenefit(tribe));
        if (tribe.getRelation().isEnemy()) {
            addAction(actions, "PEACE", availability(tribe, TribeAction.REQUEST_PEACE),
                    () -> controller.requestPeace(tribe));
        } else {
            addAction(actions, "DECLARE WAR", availability(tribe, TribeAction.DECLARE_WAR),
                    () -> confirmWar(tribe));
        }
        TribeActionAvailability attack = attackAvailability(tribe);
        addAction(actions, "ATTACK CAMP", attack, () -> controller.attackSelectedTribeCamp(tribe));
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private TribeActionAvailability availability(Tribe tribe, TribeAction action) {
        return controller.getTribeActionAvailability(tribe, action);
    }

    private TribeActionAvailability attackAvailability(Tribe tribe) {
        if (!controller.getGameState().isTribeCurrentlyVisible(tribe)) {
            return TribeActionAvailability.unavailable("Camp is outside current vision");
        }
        if (tribe.isDefeated()) return TribeActionAvailability.unavailable("Camp is already an Outpost");
        if (!(controller.getSelectedUnit() instanceof MilitaryUnit)) {
            return TribeActionAvailability.unavailable("Select a military unit first");
        }
        MilitaryUnit selected = (MilitaryUnit) controller.getSelectedUnit();
        if (!selected.canAttack()) return TribeActionAvailability.unavailable("Selected military unit has no attack AP");
        if (selected.getPosition().distanceTo(tribe.getCampCoordinate()) > selected.getRange()) {
            return TribeActionAvailability.unavailable("Selected military unit is out of range");
        }
        return TribeActionAvailability.available("Attack through the combat engine");
    }

    private void addAction(JPanel panel, String text, TribeActionAvailability availability, Runnable action) {
        JButton button = new JButton(text);
        button.setName("tribe-action-" + text.toLowerCase().replace(' ', '-').replace("…", ""));
        button.setEnabled(availability.isAvailable());
        button.setFont(new Font("SansSerif", Font.BOLD, 9));
        button.setToolTipText(availability.getReason());
        button.addActionListener(e -> {
            action.run();
            rebuild();
        });
        panel.add(button);
    }

    private void showGiftDialog(Tribe tribe) {
        JDialog dialog = formDialog("Gift to " + tribe.getName());
        JComboBox<Constants.ResourceType> resource = new JComboBox<>(Constants.ResourceType.values());
        JSpinner amount = new JSpinner(new SpinnerNumberModel(10, 1, 500, 1));
        JLabel reason = reasonLabel();
        JButton send = new JButton("SEND GIFT");
        JPanel form = formGrid();
        form.add(new JLabel("Resource")); form.add(resource);
        form.add(new JLabel("Amount")); form.add(amount);
        form.add(reason); form.add(send);
        Runnable refresh = () -> {
            TribeActionAvailability availability = controller.getGiftAvailability(tribe,
                    (Constants.ResourceType) resource.getSelectedItem(), (Integer) amount.getValue());
            updateFormAction(send, reason, availability);
        };
        resource.addActionListener(e -> refresh.run());
        amount.addChangeListener(e -> refresh.run());
        send.addActionListener(e -> {
            if (controller.giftTribe(tribe, (Constants.ResourceType) resource.getSelectedItem(),
                    (Integer) amount.getValue())) dialog.dispose();
            refresh.run();
            rebuild();
        });
        refresh.run();
        dialog.add(form);
        dialog.setVisible(true);
    }

    private void showTradeDialog(Tribe tribe) {
        JDialog dialog = formDialog("Trade with " + tribe.getName());
        JComboBox<Constants.ResourceType> sell = new JComboBox<>(Constants.ResourceType.values());
        JComboBox<Constants.ResourceType> buy = new JComboBox<>(Constants.ResourceType.values());
        buy.setSelectedIndex(1);
        JSpinner amount = new JSpinner(new SpinnerNumberModel(10, 1, 500, 1));
        JLabel reason = reasonLabel();
        JButton exchange = new JButton("EXCHANGE");
        JPanel form = formGrid();
        form.add(new JLabel("Sell")); form.add(sell);
        form.add(new JLabel("Buy")); form.add(buy);
        form.add(new JLabel("Amount")); form.add(amount);
        form.add(reason); form.add(exchange);
        Runnable refresh = () -> {
            TribeActionAvailability availability = controller.getTradeAvailability(tribe,
                    (Constants.ResourceType) sell.getSelectedItem(),
                    (Constants.ResourceType) buy.getSelectedItem(), (Integer) amount.getValue());
            updateFormAction(exchange, reason, availability);
        };
        sell.addActionListener(e -> refresh.run());
        buy.addActionListener(e -> refresh.run());
        amount.addChangeListener(e -> refresh.run());
        exchange.addActionListener(e -> {
            if (controller.tradeWithTribe(tribe, (Constants.ResourceType) sell.getSelectedItem(),
                    (Constants.ResourceType) buy.getSelectedItem(), (Integer) amount.getValue())) dialog.dispose();
            refresh.run();
            rebuild();
        });
        refresh.run();
        dialog.add(form);
        dialog.setVisible(true);
    }

    private JDialog formDialog(String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(430, 260);
        dialog.setLocationRelativeTo(this);
        return dialog;
    }

    private JPanel formGrid() {
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        return form;
    }

    private JLabel reasonLabel() {
        JLabel label = new JLabel();
        label.setForeground(new Color(120, 70, 30));
        return label;
    }

    private void updateFormAction(JButton button, JLabel label, TribeActionAvailability availability) {
        button.setEnabled(availability.isAvailable());
        button.setToolTipText(availability.getReason());
        label.setText("<html>" + availability.getReason() + "</html>");
    }

    private void confirmWar(Tribe tribe) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Declare war on " + tribe.getName() + "? This sets relation to -100 and cancels its alliance/mission.",
                "Confirm war", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) controller.declareWar(tribe);
    }

    private void showBenefit(Tribe tribe) {
        JOptionPane.showMessageDialog(this,
                controller.getGameState().getAllianceBenefitDescription(tribe),
                tribe.getType().name() + " alliance benefit", JOptionPane.INFORMATION_MESSAGE);
    }

    private String missionDetails(TribeMission mission) {
        if (mission == null) return "<br>Mission: none";
        return "<br>Mission: <b>" + mission.getTitle() + "</b> — " + mission.getObjective().getDescription()
                + "<br>Progress: " + missionProgress(mission) + " • Status " + mission.getStatus()
                + " • Deadline " + mission.getRemainingTurns() + "/" + mission.getTotalTurns() + " turns"
                + "<br>Reward: " + rewardText(mission);
    }

    private String missionProgress(TribeMission mission) {
        TribeMissionObjective objective = mission.getObjective();
        if (objective instanceof KillCountObjective kills) {
            return kills.getCurrentKills() + "/" + kills.getRequiredKills() + " kills";
        }
        if (objective instanceof ResourcePaymentObjective payment) {
            return "provide " + resources(payment.getRequiredResources());
        }
        return objective.isCompleted() ? "complete" : "in progress";
    }

    private String rewardText(TribeMission mission) {
        String text = resources(mission.getReward().getResources());
        if (mission.getReward().getRelationReward() > 0) {
            text += (text.equals("none") ? "" : ", ") + "+" + mission.getReward().getRelationReward() + " relation";
        }
        if (!mission.getReward().getEffects().isEmpty()) {
            text += (text.equals("none") ? "" : ", ")
                    + mission.getReward().getEffects().toString().replace('_', ' ');
        }
        return text;
    }

    private String resources(ResourceAmount amount) {
        List<String> parts = new ArrayList<>();
        for (Constants.ResourceType type : Constants.ResourceType.values()) {
            if (amount.get(type) > 0) parts.add(amount.get(type) + " " + type.name());
        }
        return parts.isEmpty() ? "none" : String.join(", ", parts);
    }

    private String coordinate(Tribe tribe) {
        return "(" + tribe.getCampCoordinate().getQ() + ", " + tribe.getCampCoordinate().getR() + ")";
    }

    private Color color(Tribe tribe) {
        return switch (tribe.getType()) {
            case FARMER -> new Color(116, 185, 92);
            case WARRIOR -> new Color(205, 87, 72);
            case MERCHANT -> new Color(216, 169, 69);
            case MOUNTAIN -> new Color(155, 163, 177);
            case COASTAL -> new Color(75, 165, 195);
        };
    }

    private String tradeBenefit(TribeType type) {
        return switch (type) {
            case FARMER, COASTAL -> "Food at 75%";
            case WARRIOR -> "No resource trade";
            case MERCHANT -> "Any different resource at 80%";
            case MOUNTAIN -> "Stone or Iron at 75%";
        };
    }

    public int getDisplayedTribeCount() { return displayedTribeCount; }
    public Tribe getFocusedTribe() { return focusedTribe; }
}
