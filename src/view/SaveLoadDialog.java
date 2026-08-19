package view;

import controller.GameController;
import model.save.*;

import javax.swing.*;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Four slot cards shared by the main menu and in-game HUD. */
public final class SaveLoadDialog extends JDialog {
    private static final Color BG = new Color(11, 14, 29);
    private static final Color CARD = new Color(24, 30, 52);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color TEXT = new Color(218, 220, 225);
    private final MainWindow window;
    private final GameController controller;
    private final boolean canSave;
    private final JPanel slots = new JPanel(new GridLayout(2, 2, 14, 14));

    public SaveLoadDialog(MainWindow window, GameController controller, boolean canSave) {
        super(window, "Save & Load", true);
        this.window = window; this.controller = controller; this.canSave = canSave;
        setSize(760, 470);
        setLocationRelativeTo(window);
        setLayout(new BorderLayout(12, 12));
        getContentPane().setBackground(BG);
        JLabel title = new JLabel("SAVE CHRONICLE", SwingConstants.CENTER);
        title.setForeground(GOLD); title.setFont(new Font("Georgia", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(16, 8, 2, 8));
        add(title, BorderLayout.NORTH);
        slots.setOpaque(false);
        slots.setBorder(BorderFactory.createEmptyBorder(8, 18, 18, 18));
        add(slots, BorderLayout.CENTER);
        rebuild();
    }

    private void rebuild() {
        slots.removeAll();
        for (SaveSlot slot : SaveSlot.values()) slots.add(card(slot));
        slots.revalidate(); slots.repaint();
    }

    private JPanel card(SaveSlot slot) {
        SavePreview preview = controller.getSavePreview(slot);
        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(preview.isCorrupted() ? new Color(190, 70, 70) : new Color(80, 92, 125), 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel heading = label(slot.getDisplayName(), GOLD, Font.BOLD, 15);
        card.add(heading); card.add(Box.createVerticalStrut(7));
        String details;
        if (!preview.isPresent()) details = "Empty slot";
        else if (preview.isCorrupted()) details = "Corrupted or incompatible save";
        else {
            String time = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm")
                    .withZone(ZoneId.systemDefault()).format(preview.getSavedAt());
            details = "<html>" + preview.getName() + "<br>Turn " + preview.getTurn() + " • "
                    + preview.getSeason() + "<br>Town Hall level " + preview.getTownHallLevel()
                    + "<br><font color='#8f98ac'>" + time + "</font></html>";
        }
        card.add(label(details, preview.isCorrupted() ? new Color(240, 120, 120) : TEXT, Font.PLAIN, 12));
        card.add(Box.createVerticalGlue());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); actions.setOpaque(false);
        if (canSave && slot.isManual()) {
            SaveAvailability availability = controller.getSaveAvailability();
            JButton save = button(preview.isPresent() ? "OVERWRITE" : "SAVE", new Color(47, 91, 67));
            save.setName("save-slot-" + slot.name());
            save.setEnabled(availability.isEnabled());
            save.setToolTipText(availability.getReason());
            save.addActionListener(e -> save(slot, preview.isPresent())); actions.add(save);
            if (!availability.isEnabled()) {
                card.add(label("<html><font color='#d6ad63'>Save unavailable:</font> "
                        + availability.getReason() + "</html>", TEXT, Font.PLAIN, 10));
            }
        }
        JButton load = button("LOAD", new Color(43, 76, 118));
        load.setEnabled(preview.isPresent() && !preview.isCorrupted());
        load.setToolTipText(load.isEnabled() ? "Load after validation" : "No valid save in this slot");
        load.addActionListener(e -> load(slot)); actions.add(load);
        if (preview.isPresent() && slot.isManual()) {
            JButton delete = button("DELETE", new Color(100, 48, 48));
            delete.addActionListener(e -> { controller.deleteSave(slot); rebuild(); }); actions.add(delete);
        }
        card.add(actions);
        return card;
    }

    private void save(SaveSlot slot, boolean overwrite) {
        if (overwrite && JOptionPane.showConfirmDialog(this, "Overwrite " + slot.getDisplayName() + "?",
                "Confirm overwrite", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        String name = JOptionPane.showInputDialog(this, "Save name:", "My Dominion");
        if (name != null && controller.saveGame(slot, name)) rebuild();
    }

    private void load(SaveSlot slot) {
        if (canSave && controller.hasUnsavedProgress() && !resolveUnsavedProgress()) return;
        SaveLoadResult result = controller.loadGame(slot);
        if (result.isSuccessful()) { dispose(); window.showCurrentGame(); }
        else JOptionPane.showMessageDialog(this, result.getMessage(), "Load failed", JOptionPane.ERROR_MESSAGE);
    }

    private boolean resolveUnsavedProgress() {
        Object[] options = {"Save", "Continue", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "The current game has unsaved progress. Save before loading another chronicle?",
                "Unsaved progress", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
        if (choice == 1) return true;
        if (choice != 0) return false;
        return saveBeforeLoad();
    }

    private boolean saveBeforeLoad() {
        SaveSlot[] manualSlots = {SaveSlot.MANUAL_1, SaveSlot.MANUAL_2, SaveSlot.MANUAL_3};
        SaveSlot slot = (SaveSlot) JOptionPane.showInputDialog(this,
                "Choose a manual slot:", "Save before load", JOptionPane.QUESTION_MESSAGE,
                null, manualSlots, manualSlots[0]);
        if (slot == null) return false;
        SavePreview preview = controller.getSavePreview(slot);
        if (preview.isPresent() && JOptionPane.showConfirmDialog(this,
                "Overwrite " + slot.getDisplayName() + "?", "Confirm overwrite",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return false;
        String name = JOptionPane.showInputDialog(this, "Save name:", "Before load");
        if (name == null) return false;
        if (controller.saveGame(slot, name)) return true;
        JOptionPane.showMessageDialog(this, controller.getStatusMessage(),
                "Save failed", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private JLabel label(String text, Color color, int style, int size) {
        JLabel label = new JLabel(text); label.setForeground(color);
        label.setFont(new Font("SansSerif", style, size)); label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JButton button(String text, Color color) {
        JButton button = new JButton(text); button.setBackground(color); button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 10)); button.setFocusPainted(false);
        return button;
    }
}
