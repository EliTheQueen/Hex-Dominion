package view;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JFrame;
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
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);

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
