package ui;
import ui.panels.*;

public class EditorUI {
    private final EditorContext context = EditorContext.getInstance();
    private final HierarchyPanel hierarchyPanel = new HierarchyPanel();
    private final ModesPanel modesPanel = new ModesPanel();
    private final ScenePanel scenePanel = new ScenePanel();
    private final InspectorPanel inspectorPanel = new InspectorPanel();
    private final ConsolePanel consolePanel = new ConsolePanel();
    private final AssetsPanel assetsPanel = new AssetsPanel();
    private final MainMenuBarPanel mainMenuBarPanel = new MainMenuBarPanel();

    public EditorContext getContext() {
        return context;
    }

    public void draw() {
        mainMenuBarPanel.draw(context);
        hierarchyPanel.draw(context);
        modesPanel.draw(context);
        scenePanel.draw(context);
        inspectorPanel.draw(context);
        consolePanel.draw(context);
        assetsPanel.draw(context);
    }
}