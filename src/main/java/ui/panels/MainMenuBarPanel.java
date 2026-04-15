package ui.panels;

import core.Engine;
import core.ProjectManager;
import imgui.ImGui;
import ui.EditorContext;
import ui.EditorPanel;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Path;

public class MainMenuBarPanel implements EditorPanel {
    private final int POS_X = 0;
    private final int POS_Y = 0;
    private final int WIDTH = 1920;
    private final int HEIGHT = 10;

    @Override
    public void init(){
        ImGui.setWindowPos(POS_X, POS_Y);
        ImGui.setWindowSize(WIDTH, HEIGHT);
    }

    @Override
    public void draw(EditorContext context) {
        init();
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Open")) {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Open Scene");
                    chooser.setCurrentDirectory(ProjectManager.getScenesPath().toFile());
                    chooser.setFileFilter(new FileNameExtensionFilter("Lyvex Scene (*.lyvexscene)", "lyvexscene"));

                    int result = chooser.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        Path selectedScene = chooser.getSelectedFile().toPath();
                        context.getEngine().openScene(selectedScene);
                    }
                }

                if (ImGui.menuItem("Save")) {
                    Engine.saveCurrentScenePublic();
                }

                ImGui.endMenu();
            }

            ImGui.endMainMenuBar();
        }
    }

    @Override
    public void optionsMenu() {

    }
}