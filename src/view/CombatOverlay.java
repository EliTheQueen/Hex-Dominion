package view;

import model.combat.CombatReport;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/** Pair-by-pair dice reveal followed by the full authoritative battle result. */
public final class CombatOverlay extends JDialog {
    private static final Color GOLD = new Color(212, 175, 55);
    private final CombatPresentationModel presentation;
    private final DiceBoard diceBoard;
    private final JLabel comparison = new JLabel("Preparing combat dice…", SwingConstants.CENTER);
    private final JTextArea results = new JTextArea();
    private final Timer timer;
    private int revealedPairs;
    private int revealedResults;

    public CombatOverlay(JFrame owner, CombatReport report) {
        super(owner, "Battle Report", false);
        presentation = new CombatPresentationModel(report);
        diceBoard = new DiceBoard(presentation);
        setName("combat-overlay");
        setSize(620, 430);
        setMinimumSize(new Dimension(560, 390));
        setLocationRelativeTo(owner);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(12, 10)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(30, 34, 58),
                        getWidth(), getHeight(), new Color(11, 14, 28)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(GOLD);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
                g2.dispose();
            }
        };
        content.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.X_AXIS));
        heading.add(sideLabel("ATTACKER", presentation.getAttackerLabel(),
                new Color(224, 104, 83)));
        JLabel versus = new JLabel("  VS  ", SwingConstants.CENTER);
        versus.setForeground(GOLD);
        versus.setFont(new Font("Georgia", Font.BOLD, 20));
        heading.add(versus);
        heading.add(sideLabel("DEFENDER", presentation.getDefenderLabel(),
                new Color(100, 170, 226)));
        content.add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        diceBoard.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(diceBoard);
        comparison.setForeground(new Color(237, 220, 166));
        comparison.setFont(new Font("SansSerif", Font.BOLD, 13));
        comparison.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(Box.createVerticalStrut(4));
        center.add(comparison);

        JLabel wall = new JLabel(presentation.hasWallModifier()
                ? "WALL ACTIVE  •  defender dice include +2, capped at 6" : " ",
                SwingConstants.CENTER);
        wall.setForeground(new Color(244, 193, 74));
        wall.setFont(new Font("SansSerif", Font.BOLD, 11));
        wall.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(Box.createVerticalStrut(6));
        center.add(wall);
        content.add(center, BorderLayout.CENTER);

        results.setName("combat-result-sequence");
        results.setEditable(false);
        results.setOpaque(false);
        results.setForeground(Color.WHITE);
        results.setFont(new Font("SansSerif", Font.BOLD, 12));
        results.setLineWrap(true);
        results.setWrapStyleWord(true);
        JScrollPane resultScroll = new JScrollPane(results);
        resultScroll.setOpaque(false);
        resultScroll.getViewport().setOpaque(false);
        resultScroll.setBorder(BorderFactory.createLineBorder(new Color(89, 97, 133), 1, true));
        resultScroll.setPreferredSize(new Dimension(0, 108));

        JButton close = new JButton("CLOSE");
        close.setName("combat-close");
        close.addActionListener(event -> dispose());
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setOpaque(false);
        bottom.add(resultScroll, BorderLayout.CENTER);
        bottom.add(close, BorderLayout.EAST);
        content.add(bottom, BorderLayout.SOUTH);
        setContentPane(content);

        timer = new Timer(560, event -> advance());
        timer.setInitialDelay(500);
        timer.start();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { timer.stop(); }
        });
    }

    private JPanel sideLabel(String role, String name, Color color) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel roleLabel = new JLabel(role, SwingConstants.CENTER);
        roleLabel.setForeground(color);
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setForeground(new Color(239, 232, 210));
        nameLabel.setFont(new Font("Georgia", Font.BOLD, 16));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(roleLabel);
        panel.add(nameLabel);
        return panel;
    }

    private void advance() {
        List<String> pairs = presentation.getPairComparisons();
        if (revealedPairs < pairs.size()) {
            comparison.setText(pairs.get(revealedPairs));
            revealedPairs++;
            diceBoard.setRevealedPairs(revealedPairs);
            return;
        }
        List<String> sequence = presentation.getResultSequence();
        if (revealedResults < sequence.size()) {
            if (revealedResults > 0) results.append("\n");
            results.append("• " + sequence.get(revealedResults));
            revealedResults++;
            return;
        }
        comparison.setText("Battle resolution complete");
        timer.stop();
    }

    public void revealAllForTest() {
        while (timer.isRunning()) advance();
    }

    public CombatPresentationModel getPresentationModel() { return presentation; }
    public String getComparisonText() { return comparison.getText(); }
    public String getResultText() { return results.getText(); }

    private static final class DiceBoard extends JPanel {
        private final CombatPresentationModel presentation;
        private int revealedPairs;

        private DiceBoard(CombatPresentationModel presentation) {
            this.presentation = presentation;
            setOpaque(false);
            setPreferredSize(new Dimension(540, 118));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));
        }

        private void setRevealedPairs(int revealedPairs) {
            this.revealedPairs = revealedPairs;
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawDiceRow(g2, presentation.getAttackerDice(), 22, new Color(195, 70, 57), true);
            drawDiceRow(g2, presentation.getDefenderDice(), 72, new Color(64, 123, 181), false);
            g2.dispose();
        }

        private void drawDiceRow(Graphics2D g2, List<Integer> dice, int y, Color color,
                                 boolean attacker) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(color.brighter());
            g2.drawString(attacker ? "ATTACK" : "DEFEND", 6, y + 24);
            for (int i = 0; i < dice.size(); i++) {
                int x = 78 + i * 58;
                boolean pairedAndRevealed = i < revealedPairs
                        && i < Math.min(presentation.getAttackerDice().size(),
                        presentation.getDefenderDice().size());
                g2.setPaint(new GradientPaint(x, y, pairedAndRevealed ? color.brighter()
                        : new Color(89, 94, 116), x + 40, y + 40,
                        pairedAndRevealed ? color.darker() : new Color(48, 52, 72)));
                g2.fillRoundRect(x, y, 40, 40, 9, 9);
                g2.setColor(pairedAndRevealed ? GOLD : new Color(157, 163, 186));
                g2.setStroke(new BasicStroke(pairedAndRevealed ? 2f : 1f));
                g2.drawRoundRect(x, y, 40, 40, 9, 9);
                g2.setFont(new Font("Monospaced", Font.BOLD, 20));
                g2.setColor(Color.WHITE);
                String value = String.valueOf(dice.get(i));
                g2.drawString(value, x + 20 - g2.getFontMetrics().stringWidth(value) / 2, y + 27);
            }
        }
    }
}
