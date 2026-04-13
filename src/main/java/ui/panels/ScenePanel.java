package ui.panels;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import ui.EditorContext;
import ui.EditorPanel;

public class ScenePanel implements EditorPanel {
    private final int POS_X = 350;
    private final int POS_Y = 0;
    private final int WIDTH = 1220;
    private final int HEIGHT = 600;

    @Override
    public void init(){
        ImGui.setNextWindowPos(POS_X, POS_Y, ImGuiCond.Always);
        ImGui.setNextWindowSize(WIDTH, HEIGHT, ImGuiCond.Always);
    }

    @Override
    public void draw(EditorContext context){
        init();
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
        ImGui.begin("Scene", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize);

        float imageX = ImGui.getCursorScreenPosX();
        float imageY = ImGui.getCursorScreenPosY();
        float availableWidth = ImGui.getContentRegionAvailX();
        float availableHeight = ImGui.getContentRegionAvailY();

        context.setSceneViewportX(imageX);
        context.setSceneViewportY(imageY);
        context.setSceneViewportWidth(availableWidth);
        context.setSceneViewportHeight(availableHeight);
        context.setSceneHovered(ImGui.isWindowHovered());

        int sceneTextureId = context.getSceneTextureId();
        if(sceneTextureId != 0){
            ImGui.image(sceneTextureId, availableWidth, availableHeight, 0, 1, 1, 0);
        }
        else{
            ImGui.text("No scene loaded");
        }

        ImGui.end();
        ImGui.popStyleVar();
    }

    @Override
    public void optionsMenu(){

    }
}