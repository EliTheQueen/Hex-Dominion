package view;

import controller.GameController;
import model.ActionAvailability;
import model.Constants;
import model.HexCoordinate;
import model.trade.BazaarTradeLevel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** One panel for Bazaar fixed tiers and neutral Trading Post exchanges. */
public final class TradePanel extends JDialog {
    private final GameController controller;
    private final JComboBox<Constants.ResourceType> sell = new JComboBox<>(Constants.ResourceType.values());
    private final JComboBox<Constants.ResourceType> buy = new JComboBox<>(Constants.ResourceType.values());
    private final JComboBox<String> source = new JComboBox<>();
    private final JComboBox<Integer> bazaarTier = new JComboBox<>(new Integer[] {10, 100, 500});
    private final JSpinner postAmount = new JSpinner(new SpinnerNumberModel(10, 1, 10000, 1));
    private final JLabel preview = new JLabel(" ", SwingConstants.CENTER);
    private final JButton confirm = new JButton("CONFIRM TRADE");
    private final List<HexCoordinate> posts = new ArrayList<>();

    public TradePanel(GameController controller, JFrame parent) {
        super(parent, "Trade", true); this.controller = controller;
        setSize(480, 390); setLocationRelativeTo(parent);
        getContentPane().setBackground(new Color(12, 15, 30)); setLayout(new BorderLayout(10, 10));
        JLabel title = new JLabel("DOMINION EXCHANGE", SwingConstants.CENTER);
        title.setForeground(new Color(212, 175, 55)); title.setFont(new Font("Georgia", Font.BOLD, 21));
        title.setBorder(BorderFactory.createEmptyBorder(16, 8, 5, 8)); add(title, BorderLayout.NORTH);
        JPanel form = new JPanel(new GridLayout(5, 2, 9, 9)); form.setBackground(new Color(25, 30, 51));
        form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(75, 83, 110)),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        if (controller.getGameState().getActiveBazaar() != null) source.addItem("Bazaar");
        for (HexCoordinate post : controller.getGameState().getEligibleTradingPosts()) {
            posts.add(post); source.addItem("Trading Post " + post.getQ() + "," + post.getR());
        }
        form.add(fieldLabel("Trader")); form.add(source);
        form.add(fieldLabel("Sell")); form.add(sell);
        form.add(fieldLabel("Buy")); form.add(buy);
        form.add(fieldLabel("Bazaar tier")); form.add(bazaarTier);
        form.add(fieldLabel("Post amount")); form.add(postAmount);
        add(form, BorderLayout.CENTER);
        JPanel south = new JPanel(); south.setBackground(new Color(12, 15, 30));
        preview.setForeground(new Color(180, 205, 215)); preview.setPreferredSize(new Dimension(220, 28));
        confirm.setBackground(new Color(42, 90, 68)); confirm.setForeground(Color.WHITE); confirm.setFocusPainted(false);
        confirm.setName("trade-confirm");
        JButton close = new JButton("CLOSE"); close.addActionListener(e -> dispose());
        south.add(preview); south.add(confirm); south.add(close); add(south, BorderLayout.SOUTH);
        source.addActionListener(e -> refresh()); sell.addActionListener(e -> refresh()); buy.addActionListener(e -> refresh());
        bazaarTier.addActionListener(e -> refresh()); postAmount.addChangeListener(e -> refresh());
        confirm.addActionListener(e -> execute()); refresh();
    }

    private JLabel fieldLabel(String value) {
        JLabel label = new JLabel(value); label.setForeground(new Color(205, 195, 160));
        label.setFont(new Font("SansSerif", Font.BOLD, 12)); return label;
    }

    private boolean bazaarSelected() { return source.getSelectedIndex() == 0
            && controller.getGameState().getActiveBazaar() != null; }

    private void refresh() {
        boolean hasSource = source.getItemCount() > 0;
        int amount = bazaarSelected() ? (Integer) bazaarTier.getSelectedItem() : (Integer) postAmount.getValue();
        int received;
        if (bazaarSelected()) {
            BazaarTradeLevel tier = BazaarTradeLevel.forQuantity(amount);
            received = tier == null ? 0 : (int) Math.floor(amount * tier.getRate());
        } else received = (int) Math.floor(amount * .8);
        bazaarTier.setEnabled(bazaarSelected()); postAmount.setEnabled(!bazaarSelected());
        ActionAvailability availability;
        Constants.ResourceType sold = (Constants.ResourceType) sell.getSelectedItem();
        Constants.ResourceType bought = (Constants.ResourceType) buy.getSelectedItem();
        if (!hasSource) {
            availability = ActionAvailability.disabled("Build a Bazaar or claim a Trading Post");
        } else if (bazaarSelected()) {
            availability = controller.getBazaarTradeAvailability(sold, bought, amount);
        } else {
            int index = source.getSelectedIndex()
                    - (controller.getGameState().getActiveBazaar() != null ? 1 : 0);
            availability = index >= 0 && index < posts.size()
                    ? controller.getTradingPostTradeAvailability(posts.get(index), sold, bought, amount)
                    : ActionAvailability.disabled("Choose a valid Trading Post");
        }
        preview.setText(availability.isEnabled()
                ? "Sell " + amount + "  →  receive " + received + " (floor)"
                : availability.getReason());
        confirm.setEnabled(availability.isEnabled());
        confirm.setToolTipText(availability.getReason());
    }

    private void execute() {
        Constants.ResourceType s = (Constants.ResourceType) sell.getSelectedItem();
        Constants.ResourceType b = (Constants.ResourceType) buy.getSelectedItem();
        boolean ok;
        if (bazaarSelected()) ok = controller.tradeAtBazaar(s, b, (Integer) bazaarTier.getSelectedItem());
        else {
            int index = source.getSelectedIndex() - (controller.getGameState().getActiveBazaar() != null ? 1 : 0);
            ok = index >= 0 && index < posts.size() && controller.tradeAtPost(posts.get(index), s, b,
                    (Integer) postAmount.getValue());
        }
        if (ok) dispose(); else refresh();
    }
}
