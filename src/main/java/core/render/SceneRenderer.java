package core.render;

import core.InputManager;
import core.Scene;
import core.component.EditorCamera2D;
import core.GameObject;
import core.component.Sprite;
import core.component.SpriteComponent;
import ui.EditorContext;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class SceneRenderer {
    private final int viewportWidth;
    private final int viewportHeight;
    private final EditorCamera2D camera = new EditorCamera2D();

    private int shaderProgram;
    private int vao;
    private int vbo;

    private Scene scene;

    public SceneRenderer(int viewportWidth, int viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        createShader();
        createQuad();
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public void updateCamera(EditorContext context) {
        if (!context.isSceneHovered()) {
            return;
        }

        if (InputManager.isKeyDown(GLFW_KEY_A)) {
            camera.setX(camera.getX() - camera.getMoveSpeed() * 0.01f);
        }
        if (InputManager.isKeyDown(GLFW_KEY_D)) {
            camera.setX(camera.getX() + camera.getMoveSpeed() * 0.01f);
        }
        if (InputManager.isKeyDown(GLFW_KEY_W)) {
            camera.setY(camera.getY() + camera.getMoveSpeed() * 0.01f);
        }
        if (InputManager.isKeyDown(GLFW_KEY_S)) {
            camera.setY(camera.getY() - camera.getMoveSpeed() * 0.01f);
        }

        float newZoom = camera.getZoom() + InputManager.getScrollY() * camera.getZoomSpeed();
        if (newZoom < 0.1f) {
            newZoom = 0.1f;
        }
        camera.setZoom(newZoom);
    }

    public void render() {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glViewport(0, 0, viewportWidth, viewportHeight);
        glClearColor(0.15f, 0.15f, 0.18f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        if (scene == null) {
            return;
        }

        glUseProgram(shaderProgram);

        int cameraPosLocation = glGetUniformLocation(shaderProgram, "uCameraPos");
        int zoomLocation = glGetUniformLocation(shaderProgram, "uZoom");
        int objectPosLocation = glGetUniformLocation(shaderProgram, "uObjectPos");
        int objectScaleLocation = glGetUniformLocation(shaderProgram, "uObjectScale");

        glUniform2f(cameraPosLocation, camera.getX(), camera.getY());
        glUniform1f(zoomLocation, camera.getZoom());

        glBindVertexArray(vao);
        for (GameObject root : scene.getRootObjects()) {
            renderGameObjectRecursive(root, objectPosLocation, objectScaleLocation, 0.0f, 0.0f);
        }
        glBindVertexArray(0);

        glUseProgram(0);
    }

    private void renderGameObjectRecursive(GameObject gameObject, int objectPosLocation, int objectScaleLocation, float parentX, float parentY) {
        float worldX = parentX + gameObject.getTransform().x;
        float worldY = parentY + gameObject.getTransform().y;

        SpriteComponent spriteComponent = gameObject.getComponent(SpriteComponent.class);
        if (spriteComponent != null && spriteComponent.getSprite() != null) {
            glUniform2f(objectPosLocation, worldX, worldY);
            glUniform2f(objectScaleLocation, gameObject.getTransform().scaleX, gameObject.getTransform().scaleY);

            Sprite sprite = spriteComponent.getSprite();
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, sprite.getTextureId());
            glDrawArrays(GL_TRIANGLES, 0, 6);
        }

        for (GameObject child : gameObject.getChildren()) {
            renderGameObjectRecursive(child, objectPosLocation, objectScaleLocation, worldX, worldY);
        }
    }

    public EditorCamera2D getCamera() {
        return camera;
    }

    public void dispose() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        glDeleteProgram(shaderProgram);
    }

    private void createQuad() {
        float[] vertices = {
                // pos      // uv
                -0.5f, -0.5f, 0.0f, 0.0f,
                0.5f, -0.5f, 1.0f, 0.0f,
                0.5f,  0.5f, 1.0f, 1.0f,

                -0.5f, -0.5f, 0.0f, 0.0f,
                0.5f,  0.5f, 1.0f, 1.0f,
                -0.5f,  0.5f, 0.0f, 1.0f
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void createShader() {
        String vertexShaderSource =
                "#version 330 core\n" +
                        "layout (location = 0) in vec2 aPos;\n" +
                        "layout (location = 1) in vec2 aUV;\n" +
                        "out vec2 vUV;\n" +
                        "uniform vec2 uCameraPos;\n" +
                        "uniform float uZoom;\n" +
                        "uniform vec2 uObjectPos;\n" +
                        "uniform vec2 uObjectScale;\n" +
                        "void main() {\n" +
                        "    vUV = aUV;\n" +
                        "    vec2 scaledPos = aPos * uObjectScale;\n" +
                        "    vec2 worldPos = scaledPos + uObjectPos;\n" +
                        "    vec2 viewPos = (worldPos - uCameraPos) * uZoom;\n" +
                        "    gl_Position = vec4(viewPos, 0.0, 1.0);\n" +
                        "}";

        String fragmentShaderSource =
                "#version 330 core\n" +
                        "in vec2 vUV;\n" +
                        "out vec4 FragColor;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "void main() {\n" +
                        "    FragColor = texture(uTexture, vUV);\n" +
                        "}";

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);

        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == 0) {
            throw new IllegalStateException("Vertex shader error: " + glGetShaderInfoLog(vertexShader));
        }

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);

        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == 0) {
            throw new IllegalStateException("Fragment shader error: " + glGetShaderInfoLog(fragmentShader));
        }

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);

        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == 0) {
            throw new IllegalStateException("Shader program link error: " + glGetProgramInfoLog(shaderProgram));
        }

        glUseProgram(shaderProgram);
        int textureLocation = glGetUniformLocation(shaderProgram, "uTexture");
        glUniform1i(textureLocation, 0);
        glUseProgram(0);

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }
}