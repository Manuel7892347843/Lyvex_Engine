package ui.panels;

import core.log.Log;
import core.log.Logs;
import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import ui.EditorContext;
import ui.EditorPanel;

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

        for(Log log : Logs.all) {
            drawLog(log);
        }

        optionsMenu();

        ImGui.end();
    }

    private void drawLog(Log log) {
        float r = 0.7f;
        float g = 0.9f;
        float b = 1.0f;
        float a = 1.0f;

        if (log.type == Log.Type.WARNING) {
            r = 1.0f;
            g = 0.8f;
            b = 0.2f;
        } else if (log.type == Log.Type.ERROR) {
            r = 1.0f;
            g = 0.25f;
            b = 0.25f;
        } else if (log.type == Log.Type.SUCCESS) {
            r = 0.2f;
            g = 0.8f;
            b = 0.2f;
        }

        ImGui.pushTextWrapPos();
        ImGui.textColored(r, g, b, a, log.msg);
        ImGui.popTextWrapPos();
    }

    @Override
    public void optionsMenu(){
        if (ImGui.beginPopup("ConsoleOptionsMenu")) {
            if (ImGui.menuItem("Clear console")) {
                Logs.clear();
            }

            ImGui.endPopup();
        }
    }
}