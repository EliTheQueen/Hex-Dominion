package app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import view.MainWindow;

public class Main {
    public static void main(String[] args) {
        // Use the cross-platform (Metal) look and feel rather than the native one.
        // On macOS the native Aqua L&F ignores JButton.setBackground, which would make
        // light-on-light button text unreadable in the dialogs.
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to whatever default is available.
        }
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
