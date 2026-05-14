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
        // DEBUG: controlla se ci sono finestre non chiuse
        // Non c'è API diretta per lo stack size, ma possiamo usare un workaround
        try {
            ImGui.render();
        } catch (AssertionError e) {
            System.err.println("=== IMGUI BEGIN/END MISMATCH DETECTED ===");
            System.err.println("This means some panel called ImGui.begin() without matching ImGui.end()");
            System.err.println("Check all your panels (ModesPanel, HierarchyPanel, etc.)");
            throw e;
        }
        gl3.renderDrawData(ImGui.getDrawData());
    }

    public void dispose() {
        gl3.dispose();
        glfw.dispose();
        ImGui.destroyContext();
    }
}