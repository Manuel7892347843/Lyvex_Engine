package ui.panels;

import core.Log;
import core.Logs;
import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import ui.EditorContext;
import ui.EditorPanel;

import java.util.ArrayList;
import java.util.List;

public class ConsolePanel implements EditorPanel {
    private final int POS_X = 0;
    private final int POS_Y = 656;
    private final int WIDTH = 1000;
    private final int HEIGHT = 353;

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

        for(Log log : Logs.logs) {
            ImGui.text(log.msg);
        }

        optionsMenu();

        ImGui.end();
    }

    @Override
    public void optionsMenu(){
        if (ImGui.beginPopup("ConsoleOptionsMenu")) {
            if (ImGui.menuItem("Clear console")) {
                Logs.logs.clear();
            }

            ImGui.endPopup();
        }
    }
}
