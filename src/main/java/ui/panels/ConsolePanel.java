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

        for(Log log : Logs.logs) {
            ImGui.textColored(0.7f, 0.9f, 1.0f, 1.0f, log.msg); // Normal
        }

        for(Log log : Logs.logs_warning){
            ImGui.textColored(1.0f, 0.8f, 0.2f, 1.0f, log.msg); // Waring
        }

        for(Log log : Logs.logs_error){
            ImGui.textColored(1.0f, 0.25f, 0.25f, 1.0f, log.msg); // Error
        }

        for(Log log : Logs.logs_success){
            ImGui.textColored(0.2f, 0.8f, 0.2f, 1.0f, log.msg); // Success
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
