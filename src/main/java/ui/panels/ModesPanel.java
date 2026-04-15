package ui.panels;

import core.Log;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import ui.EditorContext;
import ui.EditorPanel;

public class ModesPanel implements EditorPanel {
    private final int POS_X = 350;
    private final int POS_Y = 19;
    private final int WIDTH = 1220;
    private final int HEIGHT = 38;

    @Override
    public void init() {
        ImGui.setNextWindowPos(POS_X, POS_Y, ImGuiCond.Always);
        ImGui.setNextWindowSize(WIDTH, HEIGHT, ImGuiCond.Always);
    }

    @Override
    public void draw(EditorContext context) {
        init();

        ImGui.begin("Modes", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoTitleBar);

        boolean isPlaying = context.getEngine() != null && context.getEngine().getEngineState();
        float buttonWidth = 100.0f;

        float availableWidth = ImGui.getContentRegionAvailX();
        float centerX = (availableWidth - buttonWidth) * 0.5f;
        if (centerX > 0) {
            ImGui.setCursorPosX(centerX);
        }

        if (!isPlaying) {
            if (ImGui.button("Play", 100,0)) {
                Log.log("Starting play mode");
                context.getEngine().startPlayMode();
            }
        } else {
            if (ImGui.button("Stop", 100, 0)) {
                Log.log("Stopping play mode");
                context.getEngine().stopPlayMode();
            }
        }

        ImGui.end();
    }

    @Override
    public void optionsMenu() {
    }
}