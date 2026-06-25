package view;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import controller.GameController;

public class MainWindow extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final MenuPanel menuPanel;
    private GamePanel gamePanel;
    private final GameController controller;

    public MainWindow() {
        super("Hex Dominion");
        // Confirm before exiting (handled in confirmExit) rather than closing immediately.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { confirmExit(); }
        });
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);

        SoundManager.getInstance().startMusic();

        controller = new GameController();
        controller.setMainWindow(this);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(Color.BLACK);

        menuPanel = new MenuPanel(this);
        cardPanel.add(menuPanel, "MENU");

        add(cardPanel);
        cardLayout.show(cardPanel, "MENU");
        setVisible(true);
    }

    public GameController getController() { return controller; }

    public void showMenu() {
        cardLayout.show(cardPanel, "MENU");
    }

    /** Opens the settings dialog (music volume, mute). */
    public void showSettings() {
        new SettingsPanel(this).setVisible(true);
    }

    /** Asks the player to confirm before quitting the game. */
    public void confirmExit() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit Hex Dominion?", "Exit game",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    public void startGame() {
        controller.startNewGame();
        if (gamePanel != null) {
            cardPanel.remove(gamePanel);
        }
        gamePanel = new GamePanel(controller, this);
        cardPanel.add(gamePanel, "GAME");
        cardLayout.show(cardPanel, "GAME");
        gamePanel.requestFocusInWindow();
    }

    public void showEndGame(int score) {
        EndGamePanel endPanel = new EndGamePanel(score, controller.getGameState(), this);
        cardPanel.add(endPanel, "ENDGAME");
        cardLayout.show(cardPanel, "ENDGAME");
    }
}
