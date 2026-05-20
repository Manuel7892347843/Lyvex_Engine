package ui.panels;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import ui.EditorContext;
import ui.EditorPanel;

public class GamePanel implements EditorPanel {
    private final int POS_X = 350;
    private final int POS_Y = 57;
    private final int WIDTH = 1220;
    private final int HEIGHT = 600;

    private int gameTextureId = 0;
    private int selectedDisplay = 1;
    private int maxDisplays = 4;

    @Override
    public void init() {
        ImGui.setNextWindowPos(POS_X, POS_Y, ImGuiCond.Always);
        ImGui.setNextWindowSize(WIDTH, HEIGHT, ImGuiCond.Always);
    }

    @Override
    public void draw(EditorContext context) {
        init();
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);

        boolean isOpen = ImGui.begin("Game", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize);

        if (isOpen) {
            float availableWidth = ImGui.getContentRegionAvailX();
            float availableHeight = ImGui.getContentRegionAvailY();

            if (gameTextureId != 0) {
                ImGui.image(gameTextureId, availableWidth, availableHeight, 0, 1, 1, 0);
            } else {
                String text = "Game View not available";
                float textWidth = ImGui.calcTextSize(text).x;
                ImGui.setCursorPosX((availableWidth - textWidth) * 0.5f);
                ImGui.setCursorPosY(availableHeight * 0.5f);
                ImGui.text(text);
            }
        }

        ImGui.end();
        ImGui.popStyleVar();
    }

    @Override
    public void optionsMenu() {
    }

    public void setGameTextureId(int textureId) {
        this.gameTextureId = textureId;
    }

    public int getGameTextureId() {
        return gameTextureId;
    }

    public int getSelectedDisplay() {
        return selectedDisplay;
    }

    public void setSelectedDisplay(int selectedDisplay) {
        this.selectedDisplay = Math.max(1, selectedDisplay);
    }

    public int getMaxDisplays() {
        return maxDisplays;
    }

    public void setMaxDisplays(int maxDisplays) {
        this.maxDisplays = Math.max(1, maxDisplays);

        if (selectedDisplay > this.maxDisplays) {
            selectedDisplay = this.maxDisplays;
        }
    }
}