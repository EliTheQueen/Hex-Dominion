package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/** Settings dialog: music volume slider (scroll bar) and a mute toggle. */
public class SettingsPanel extends JDialog {

    private static final Color BG = new Color(18, 20, 40);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color TEXT = new Color(225, 220, 200);

    public SettingsPanel(java.awt.Window owner) {
        super(owner, "Settings", ModalityType.APPLICATION_MODAL);

        SoundManager sound = SoundManager.getInstance();

        JPanel content = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, BG, getWidth(), getHeight(), new Color(28, 30, 60)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        content.setLayout(new BorderLayout());
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("SETTINGS", SwingConstants.CENTER);
        title.setFont(new Font("Georgia", Font.BOLD, 26));
        title.setForeground(GOLD);
        title.setBorder(new EmptyBorder(0, 0, 16, 0));
        content.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel musicLabel = new JLabel("Music volume");
        musicLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        musicLabel.setForeground(TEXT);
        musicLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        center.add(musicLabel);
        center.add(Box.createVerticalStrut(6));

        JLabel valueLabel = new JLabel(sound.getVolume() + "%");
        valueLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        valueLabel.setForeground(GOLD);

        JSlider volumeSlider = new JSlider(0, 100, sound.getVolume());
        volumeSlider.setOpaque(false);
        volumeSlider.setForeground(GOLD);
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setMinorTickSpacing(5);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setMaximumSize(new Dimension(360, 50));
        volumeSlider.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        volumeSlider.addChangeListener(e -> {
            int v = volumeSlider.getValue();
            sound.setVolume(v);
            valueLabel.setText(v + "%");
        });
        center.add(volumeSlider);

        valueLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        center.add(valueLabel);
        center.add(Box.createVerticalStrut(14));

        JCheckBox muteBox = new JCheckBox("Mute music", sound.isMuted());
        muteBox.setOpaque(false);
        muteBox.setForeground(TEXT);
        muteBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        muteBox.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        muteBox.addActionListener(e -> sound.setMuted(muteBox.isSelected()));
        center.add(muteBox);

        content.add(center, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        closeBtn.setBackground(new Color(60, 90, 60));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(BorderFactory.createLineBorder(GOLD, 1));
        closeBtn.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.add(closeBtn);
        content.add(south, BorderLayout.SOUTH);

        setContentPane(content);
        setSize(440, 320);
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}
