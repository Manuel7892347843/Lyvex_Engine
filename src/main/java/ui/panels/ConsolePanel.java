package ui.panels;

import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import ui.EditorContext;
import ui.EditorPanel;

public class ConsolePanel implements EditorPanel {
    private final int POS_X = 0;
    private final int POS_Y = 599;
    private final int WIDTH = 1000;
    private final int HEIGHT = 410;

    @Override
    public void init() {
        ImGui.setWindowPos(POS_X, POS_Y);
        ImGui.setWindowSize(WIDTH, HEIGHT);
    }

    @Override
    public void draw(EditorContext context){
        ImGui.begin("Console", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);
        ImGui.text("Console");
        init();

        if (ImGui.isWindowHovered() && ImGui.isMouseClicked(ImGuiMouseButton.Right)) {
            ImGui.openPopup("ConsoleOptionsMenu");
        }

        optionsMenu();

        ImGui.end();
    }

    @Override
    public void optionsMenu(){
        if (ImGui.beginPopup("ConsoleOptionsMenu")) {
            if (ImGui.menuItem("Clear console")) {
                System.out.println("Create folder clicked");
            }

            ImGui.endPopup();
        }
    }
}
