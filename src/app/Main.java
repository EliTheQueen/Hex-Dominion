package app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import view.MainWindow;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the default look and feel.
        }
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
