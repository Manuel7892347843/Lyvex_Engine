package ui;

import ui.panels.*;

public class EditorUI {
    private final EditorContext context = EditorContext.getInstance();
    private final HierarchyPanel hierarchyPanel = new HierarchyPanel();
    private final ModesPanel modesPanel = new ModesPanel();
    private final ScenePanel scenePanel = new ScenePanel();
    private final GamePanel gamePanel = new GamePanel();
    private final InspectorPanel inspectorPanel = new InspectorPanel();
    private final ConsolePanel consolePanel = new ConsolePanel();
    private final AssetsPanel assetsPanel = new AssetsPanel();
    private final MainMenuBarPanel mainMenuBarPanel = new MainMenuBarPanel();

    private boolean showGameView = false;

    public EditorContext getContext() {
        return context;
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }

    public void setShowGameView(boolean show) {
        this.showGameView = show;
    }

    public boolean isShowingGameView() {
        return showGameView;
    }

    public void draw() {
        try {
            mainMenuBarPanel.draw(context);
        } catch (Exception e) {
            System.err.println("Error in MainMenuBarPanel: " + e.getMessage());
        }

        try {
            hierarchyPanel.draw(context);
        } catch (Exception e) {
            System.err.println("Error in HierarchyPanel: " + e.getMessage());
        }

        try {
            modesPanel.draw(context);
        } catch (Exception e) {
            System.err.println("Error in ModesPanel: " + e.getMessage());
        }

        try {
            if (showGameView) {
                gamePanel.draw(context);
            } else {
                scenePanel.draw(context);
            }
        } catch (Exception e) {
            System.err.println("Error in Scene/Game Panel: " + e.getMessage());
        }

        try {
            inspectorPanel.draw(context);
        } catch (Exception e) {
            System.err.println("Error in InspectorPanel: " + e.getMessage());
        }

        try {
            consolePanel.draw(context);
        } catch (Exception e) {
            System.err.println("Error in ConsolePanel: " + e.getMessage());
        }

        try {
            assetsPanel.draw(context);
        } catch (Exception e) {
            System.err.println("Error in AssetsPanel: " + e.getMessage());
        }
    }
}