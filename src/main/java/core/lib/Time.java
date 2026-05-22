package core.lib;

import core.Engine;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public class Time {
    public static float deltaTime(){
        return Engine.getDeltaTime();
    }
}
