package view;

import model.combat.CombatReport;
import javax.swing.*;
import java.awt.*;

/** Fast modeless battle sequence: focus, dice reveal, comparison, then casualty/damage. */
public final class CombatOverlay extends JDialog {
    private int phase;
    private final CombatReport report;
    private final JLabel dice = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel outcome = new JLabel(" ", SwingConstants.CENTER);
    public CombatOverlay(JFrame owner, CombatReport report) {
        super(owner, "Battle Report", false); this.report = report;
        setSize(470, 250); setLocationRelativeTo(owner); setAlwaysOnTop(true);
        JPanel content = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(30, 34, 58), getWidth(), getHeight(), new Color(11, 14, 28)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(212, 175, 55)); g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18); g2.dispose();
            }
        };
        content.setOpaque(false); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(report.getAttacker() + "  VS  " + report.getDefender(), SwingConstants.CENTER);
        title.setForeground(new Color(238, 213, 142)); title.setFont(new Font("Georgia", Font.BOLD, 19));
        title.setAlignmentX(Component.CENTER_ALIGNMENT); title.setBorder(BorderFactory.createEmptyBorder(20, 8, 12, 8));
        dice.setForeground(new Color(190, 211, 232)); dice.setFont(new Font("Monospaced", Font.BOLD, 18)); dice.setAlignmentX(Component.CENTER_ALIGNMENT);
        outcome.setForeground(Color.WHITE); outcome.setFont(new Font("SansSerif", Font.BOLD, 13)); outcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title); content.add(dice); content.add(Box.createVerticalStrut(15)); content.add(outcome); setContentPane(content);
        Timer timer = new Timer(520, e -> advance()); timer.start();
        new Timer(2800, e -> { ((Timer)e.getSource()).stop(); dispose(); }).start();
    }
    private void advance() {
        phase++;
        if (phase == 1) dice.setText("Rolling polished bone dice…");
        else if (phase == 2) dice.setText("⚔ " + report.getAttackerRolls() + "     ◈ " + report.getDefenderRolls());
        else if (phase == 3) outcome.setText(report.isStructureAttack()
                ? report.getStructureDamage() + " structure damage • 1 AP consumed"
                : "Comparisons: " + report.getAttackerWins() + " won / " + report.getDefenderWins()
                + " defended (ties defend)");
        else outcome.setText(report.getCasualty() + " • 1 AP consumed");
    }
}
