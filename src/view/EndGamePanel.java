package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JPanel;

import model.GameMap;
import model.GameState;
import model.Player;
import model.ScoreCalculator;

/** Cinematic end-of-game screen with the full score breakdown. */
public class EndGamePanel extends JPanel {
    private final int totalScore;
    private final GameState gameState;
    private final MainWindow mainWindow;
    private final JButton playAgainBtn;
    private final JButton exitBtn;

    private static final Color BG = new Color(8, 10, 22);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color TEXT = new Color(200, 190, 150);

    public EndGamePanel(int totalScore, GameState gameState, MainWindow mainWindow) {
        this.totalScore = totalScore;
        this.gameState = gameState;
        this.mainWindow = mainWindow;
        setBackground(BG);
        setLayout(null);

        playAgainBtn = createBtn("PLAY AGAIN", new Color(50, 90, 50));
        exitBtn = createBtn("EXIT", new Color(90, 35, 35));
        add(playAgainBtn);
        add(exitBtn);

        playAgainBtn.addActionListener(e -> mainWindow.startGame());
        exitBtn.addActionListener(e -> System.exit(0));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        GradientPaint bgGrad = new GradientPaint(0, 0, BG, w, h, new Color(20, 25, 55));
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, w, h);

        boolean victory = totalScore >= 300;
        g2.setFont(new Font("Georgia", Font.BOLD, 56));
        String title = victory ? "VICTORY!" : "GAME OVER";
        g2.setColor(victory ? GOLD : new Color(220, 80, 80));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (w - fm.stringWidth(title)) / 2, h / 2 - 150);

        Player p = gameState.getPlayer();
        GameMap map = gameState.getMap();

        String[] lines = {
                "Territory Score:     " + ScoreCalculator.territoryScore(p),
                "Building Score:      " + ScoreCalculator.buildingScore(p),
                "Technology Score:    " + ScoreCalculator.techScore(p),
                "Exploration Score:   " + ScoreCalculator.explorationScore(map),
                "Resource Score:      " + ScoreCalculator.resourceScore(p)
        };

        g2.setFont(new Font("Monospaced", Font.PLAIN, 20));
        fm = g2.getFontMetrics();
        int startY = h / 2 - 80;
        for (String line : lines) {
            g2.setColor(TEXT);
            g2.drawString(line, (w - fm.stringWidth(line)) / 2, startY);
            startY += 34;
        }

        g2.setColor(GOLD);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(w / 2 - 160, startY, w / 2 + 160, startY);
        startY += 32;

        g2.setFont(new Font("Georgia", Font.BOLD, 30));
        fm = g2.getFontMetrics();
        g2.setColor(GOLD);
        String totalStr = "TOTAL SCORE:  " + totalScore;
        g2.drawString(totalStr, (w - fm.stringWidth(totalStr)) / 2, startY);

        // Reason line.
        g2.setFont(new Font("SansSerif", Font.ITALIC, 14));
        g2.setColor(new Color(150, 140, 110));
        String reason = gameState.getGameOverReason();
        if (reason != null && !reason.isEmpty()) {
            FontMetrics rfm = g2.getFontMetrics();
            g2.drawString(reason, (w - rfm.stringWidth(reason)) / 2, startY + 28);
        }

        g2.dispose();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int w = getWidth(), h = getHeight();
        playAgainBtn.setBounds(w / 2 - 150, h / 2 + 130, 140, 46);
        exitBtn.setBounds(w / 2 + 14, h / 2 + 130, 140, 46);
    }

    private JButton createBtn(String text, Color bg) {
        JButton btn = new JButton(text) {
            boolean hovered = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(hovered ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(GOLD);
                g2.setStroke(new BasicStroke(hovered ? 2.5f : 1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Georgia", Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
