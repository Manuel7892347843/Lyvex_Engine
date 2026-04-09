package ui;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

public class ImGuiLayer {

    private final ImGuiImplGlfw glfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 gl3 = new ImGuiImplGl3();

    public void init(long window) {
        ImGui.createContext();
        glfw.init(window, true);
        gl3.init("#version 330");
    }

    public void startFrame() {
        glfw.newFrame();
        ImGui.newFrame();
    }

    public void render() {
        ImGui.render();
        gl3.renderDrawData(ImGui.getDrawData());
    }

    public void dispose() {
        gl3.dispose();
        glfw.dispose();
        ImGui.destroyContext();
    }
}