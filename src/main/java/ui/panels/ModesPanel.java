package ui.panels;

import core.log.Log;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
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

        boolean isOpen = ImGui.begin("Modes", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoTitleBar);

        if (isOpen) {
            if (context.getEngine() == null) {
                ImGui.text("Engine not ready...");
            } else {
                boolean isPlaying = context.getEngine().getEngineState();
                float availableWidth = ImGui.getContentRegionAvailX();
                float totalButtonsWidth = isPlaying ? 270.0f : 170.0f;
                float centerX = (availableWidth - totalButtonsWidth) * 0.5f;
                if (centerX > 0) {
                    ImGui.setCursorPosX(centerX);
                }

                if (!isPlaying) {
                    if (ImGui.button("Play", 80, 0)) {
                        Log.log("Starting play mode");
                        context.getEngine().startPlayMode();
                    }
                } else {
                    if (ImGui.button("Stop", 80, 0)) {
                        Log.log("Stopping play mode");
                        context.getEngine().stopPlayMode();
                    }
                }

                ImGui.sameLine();
                boolean showGame = context.getEngine().getEditorUI().isShowingGameView();
                if (showGame) {
                    ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
                }
                if (ImGui.button("Game", 80, 0)) {
                    context.getEngine().getEditorUI().setShowGameView(true);
                }
                if (showGame) {
                    ImGui.popStyleColor();
                }

                ImGui.sameLine();
                boolean showScene = !showGame;
                if (showScene) {
                    ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
                }
                if (ImGui.button("Scene", 80, 0)) {
                    context.getEngine().getEditorUI().setShowGameView(false);
                }
                if (showScene) {
                    ImGui.popStyleColor();
                }
            }
        }

        ImGui.end();
    }

    @Override
    public void optionsMenu() {
    }
}