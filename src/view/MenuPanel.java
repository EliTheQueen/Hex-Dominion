package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Animated Civ-style main menu with a glowing title over a hex lattice. */
public class MenuPanel extends JPanel {
    private final MainWindow mainWindow;
    private final JButton newGameBtn;
    private final JButton settingsBtn;
    private final JButton exitBtn;
    private float glowPhase = 0f;
    private final Timer animTimer;

    private static final Color BG_DARK = new Color(10, 12, 25);
    private static final Color BG_MID = new Color(20, 24, 50);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color GOLD_BRIGHT = new Color(255, 215, 80);
    private static final Color HEX_COLOR = new Color(30, 35, 70, 80);

    public MenuPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(null);
        setBackground(BG_DARK);

        animTimer = new Timer(50, e -> {
            glowPhase += 0.05f;
            if (glowPhase > 2 * Math.PI) glowPhase -= (float) (2 * Math.PI);
            repaint();
        });

        newGameBtn = createStyledButton("NEW GAME", new Color(50, 90, 50), new Color(80, 140, 80));
        settingsBtn = createStyledButton("SETTINGS", new Color(40, 60, 100), new Color(70, 100, 150));
        exitBtn = createStyledButton("EXIT", new Color(90, 30, 30), new Color(140, 50, 50));

        newGameBtn.addActionListener(e -> {
            animTimer.stop();
            mainWindow.startGame();
        });
        settingsBtn.addActionListener(e -> mainWindow.showSettings());
        exitBtn.addActionListener(e -> mainWindow.confirmExit());

        animTimer.start();
    }

    private JButton createStyledButton(String text, Color normalColor, Color hoverColor) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Color c = hovered ? hoverColor : normalColor;
                GradientPaint gp = new GradientPaint(0, 0, c.brighter(), 0, getHeight(), c.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(GOLD);
                g2.setStroke(new BasicStroke(hovered ? 2.5f : 1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Georgia", Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(btn);
        return btn;
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int w = getWidth();
        int h = getHeight();
        int btnW = 260, btnH = 52;
        int centerX = w / 2 - btnW / 2;
        newGameBtn.setBounds(centerX, h / 2 + 30, btnW, btnH);
        settingsBtn.setBounds(centerX, h / 2 + 30 + (btnH + 14), btnW, btnH);
        exitBtn.setBounds(centerX, h / 2 + 30 + 2 * (btnH + 14), btnW, btnH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        GradientPaint bg = new GradientPaint(0, 0, BG_DARK, w, h, BG_MID);
        g2.setPaint(bg);
        g2.fillRect(0, 0, w, h);

        drawHexBackground(g2, w, h);

        float glow = 0.5f + 0.5f * (float) Math.sin(glowPhase);
        drawGlowingTitle(g2, w, h, glow);

        g2.setFont(new Font("Georgia", Font.ITALIC, 16));
        g2.setColor(new Color(160, 140, 100));
        String sub = "A Civilization of Hexagons Awaits";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(sub, (w - fm.stringWidth(sub)) / 2, h / 2 + 20);

        g2.dispose();
    }

    private void drawHexBackground(Graphics2D g2, int w, int h) {
        int hexR = 40;
        double hexW = hexR * 2;
        double hexH = Math.sqrt(3) * hexR;
        g2.setColor(HEX_COLOR);
        g2.setStroke(new BasicStroke(1f));
        int col = 0;
        for (double x = -hexW; x < w + hexW; x += hexW * 0.75) {
            double offsetY = (col % 2 == 0) ? 0 : hexH / 2;
            for (double y = -hexH; y < h + hexH; y += hexH) {
                drawHexOutline(g2, (int) x, (int) (y + offsetY), hexR);
            }
            col++;
        }
    }

    private void drawHexOutline(Graphics2D g2, int cx, int cy, int r) {
        int[] xp = new int[6], yp = new int[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 180 * (60 * i);
            xp[i] = cx + (int) (r * Math.cos(angle));
            yp[i] = cy + (int) (r * Math.sin(angle));
        }
        g2.drawPolygon(xp, yp, 6);
    }

    private void drawGlowingTitle(Graphics2D g2, int w, int h, float glow) {
        String title = "HEX DOMINION";
        Font titleFont = new Font("Georgia", Font.BOLD, 72);
        g2.setFont(titleFont);
        FontMetrics fm = g2.getFontMetrics();

        int totalW = fm.stringWidth(title);
        int tx = (w - totalW) / 2;
        int ty = h / 2 - 80;

        for (int layer = 5; layer >= 1; layer--) {
            float alpha = Math.min(1f, glow * 0.06f * layer);
            g2.setColor(new Color(1f, 0.85f, 0.2f, alpha));
            g2.drawString(title, tx + layer, ty + layer);
            g2.drawString(title, tx - layer, ty - layer);
        }

        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, tx + 3, ty + 3);

        GradientPaint textGrad = new GradientPaint(tx, ty - 60, GOLD_BRIGHT, tx, ty, GOLD);
        g2.setPaint(textGrad);
        g2.drawString(title, tx, ty);
    }
}
