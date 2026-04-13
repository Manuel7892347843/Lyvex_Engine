package ui;

import ui.panels.AssetsPanel;
import ui.panels.ConsolePanel;
import ui.panels.HierarchyPanel;
import ui.panels.InspectorPanel;
import ui.panels.ScenePanel;

public class EditorUI {
    private final EditorContext context = new EditorContext();
    private final HierarchyPanel hierarchyPanel = new HierarchyPanel();
    private final ScenePanel scenePanel = new ScenePanel();
    private final InspectorPanel inspectorPanel = new InspectorPanel();
    private final ConsolePanel consolePanel = new ConsolePanel();
    private final AssetsPanel assetsPanel = new AssetsPanel();

    public EditorContext getContext() {
        return context;
    }

    public void draw() {
        hierarchyPanel.draw(context);
        scenePanel.draw(context);
        inspectorPanel.draw(context);
        consolePanel.draw(context);
        assetsPanel.draw(context);
    }
}