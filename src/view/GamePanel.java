package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import controller.GameController;

/** Container for the in-game HUD, map and side panel. */
public class GamePanel extends JPanel {
    private final GameController controller;
    private final MapPanel mapPanel;
    private final HudPanel hudPanel;
    private final SidePanel sidePanel;

    public GamePanel(GameController controller, MainWindow mainWindow) {
        this.controller = controller;
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(10, 12, 25));

        mapPanel = new MapPanel(controller, this);
        hudPanel = new HudPanel(controller, this);
        sidePanel = new SidePanel(controller, this);

        add(hudPanel, BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);
        add(sidePanel, BorderLayout.EAST);

        // ESC deselects the current unit.
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "deselect");
        getActionMap().put("deselect", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                controller.deselectUnit();
                repaintAll();
            }
        });
    }

    public void repaintAll() {
        mapPanel.repaint();
        hudPanel.update();
        sidePanel.update();
    }

    public MapPanel getMapPanel() { return mapPanel; }
    public HudPanel getHudPanel() { return hudPanel; }
    public SidePanel getSidePanel() { return sidePanel; }
}
