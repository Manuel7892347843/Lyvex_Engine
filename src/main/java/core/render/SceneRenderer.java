package core.render;

import core.Engine;
import core.component.Camera;
import core.component.EditorCamera2D;
import core.component.sprite.SpriteComponent;
import core.gameobject.GameObject;
import core.input.InputManager;
import core.math.matrix4f;
import core.scene.Scene;
import ui.EditorContext;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class SceneRenderer {
    private final int viewportWidth;
    private final int viewportHeight;
    private final EditorCamera2D editorCamera = new EditorCamera2D();
    private boolean useSceneCamera = false;
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

    public void setUseSceneCamera(boolean use) {
        this.useSceneCamera = use;
    }

    public boolean isUsingSceneCamera() {
        return useSceneCamera;
    }

    public EditorCamera2D getEditorCamera() {
        return editorCamera;
    }

    public void updateCamera(EditorContext context) {
        if (!context.isSceneHovered()) {
            return;
        }

        // Velocità in unità world, scalata per deltaTime
        float speed = editorCamera.getMoveSpeed() * Engine.getDeltaTime();

        if (InputManager.isKeyDown(GLFW_KEY_A)) {
            editorCamera.setX(editorCamera.getX() - speed);
        }
        if (InputManager.isKeyDown(GLFW_KEY_D)) {
            editorCamera.setX(editorCamera.getX() + speed);
        }
        if (InputManager.isKeyDown(GLFW_KEY_W)) {
            editorCamera.setY(editorCamera.getY() + speed);
        }
        if (InputManager.isKeyDown(GLFW_KEY_S)) {
            editorCamera.setY(editorCamera.getY() - speed);
        }

        // Zoom con limiti
        float scroll = InputManager.getScrollY();
        if (scroll != 0) {
            float newZoom = editorCamera.getZoom() + scroll * editorCamera.getZoomSpeed();
            if (newZoom < 0.1f) newZoom = 0.1f;
            if (newZoom > 10.0f) newZoom = 10.0f;
            editorCamera.setZoom(newZoom);
        }
    }

    public void render() {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glViewport(0, 0, viewportWidth, viewportHeight);
        glClearColor(0.15f, 0.15f, 0.18f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        if (scene == null) return;

        glUseProgram(shaderProgram);

        // --- CALCOLO VIEW PROJECTION ---
        matrix4f viewProj;

        if (useSceneCamera) {
            Camera sceneCamera = findPrimaryCamera();
            if (sceneCamera != null) {
                viewProj = sceneCamera.getViewProjectionMatrix(viewportWidth, viewportHeight);
            } else {
                viewProj = getEditorCameraMatrix();
            }
        } else {
            viewProj = getEditorCameraMatrix();
        }

        // Passa ViewProjection allo shader
        int vpLocation = glGetUniformLocation(shaderProgram, "uViewProjection");
        if (vpLocation != -1) {
            float[] vpArray = new float[16];
            viewProj.get(vpArray);
            glUniformMatrix4fv(vpLocation, false, vpArray);
        }

        // NUOVO: Location per la matrice modello
        int modelLocation = glGetUniformLocation(shaderProgram, "uModel");

        glBindVertexArray(vao);
        for (GameObject root : scene.getRootObjects()) {
            renderGameObjectRecursive(root, modelLocation);
        }
        glBindVertexArray(0);

        glUseProgram(0);
    }

    private Camera findPrimaryCamera() {
        if (scene == null) return null;
        for (GameObject go : scene.getRootObjects()) {
            Camera cam = go.getComponent(Camera.class);
            if (cam != null && cam.primary) return cam;
            Camera childCam = findCameraInChildren(go);
            if (childCam != null) return childCam;
        }
        return null;
    }

    private Camera findCameraInChildren(GameObject parent) {
        for (GameObject child : parent.getChildren()) {
            Camera cam = child.getComponent(Camera.class);
            if (cam != null && cam.primary) return cam;
            Camera found = findCameraInChildren(child);
            if (found != null) return found;
        }
        return null;
    }

    private matrix4f getEditorCameraMatrix() {
        // La view matrix trasla in direzione opposta alla camera
        matrix4f view = new matrix4f()
                .translate(-editorCamera.getX(), -editorCamera.getY(), 0.0f);

        float aspect = (float) viewportWidth / viewportHeight;

        // L'ortho size è indipendente dallo zoom
        // Zoom > 1 = zoom in (vedi meno area)
        // Zoom < 1 = zoom out (vedi più area)
        float halfHeight = editorCamera.getOrthoSize() / editorCamera.getZoom();
        float halfWidth = halfHeight * aspect;

        matrix4f proj = new matrix4f().ortho(
                -halfWidth, halfWidth,
                -halfHeight, halfHeight,
                -100.0f, 100.0f
        );

        return proj.mul(view);
    }

    // NUOVO: Renderizza usando la matrice modello del Transform
    private void renderGameObjectRecursive(GameObject gameObject, int modelLocation) {
        if (!gameObject.isActive()) return;

        SpriteComponent spriteComponent = gameObject.getComponent(SpriteComponent.class);
        if (spriteComponent != null && spriteComponent.getSprite() != null) {
            // Ottieni la matrice modello dal Transform
            matrix4f model = gameObject.getTransform().getModelMatrix();

            // Passa allo shader
            if (modelLocation != -1) {
                float[] modelArray = new float[16];
                model.get(modelArray);
                glUniformMatrix4fv(modelLocation, false, modelArray);
            }

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, spriteComponent.getSprite().getTextureId());
            glDrawArrays(GL_TRIANGLES, 0, 6);
        }

        for (GameObject child : gameObject.getChildren()) {
            renderGameObjectRecursive(child, modelLocation);
        }
    }

    public void dispose() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        glDeleteProgram(shaderProgram);
    }

    private void createQuad() {
        float[] vertices = {
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
        // NUOVO SHADER: usa uModel invece di uObjectPos/uObjectScale/uObjectRotation
        String vertexShaderSource =
                "#version 330 core\n" +
                        "layout (location = 0) in vec2 aPos;\n" +
                        "layout (location = 1) in vec2 aUV;\n" +
                        "out vec2 vUV;\n" +
                        "uniform mat4 uViewProjection;\n" +
                        "uniform mat4 uModel;\n" +
                        "void main() {\n" +
                        "    vUV = aUV;\n" +
                        "    vec4 worldPos = uModel * vec4(aPos, 0.0, 1.0);\n" +
                        "    gl_Position = uViewProjection * worldPos;\n" +
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
            throw new IllegalStateException("Shader link error: " + glGetProgramInfoLog(shaderProgram));
        }

        glUseProgram(shaderProgram);
        glUniform1i(glGetUniformLocation(shaderProgram, "uTexture"), 0);
        glUseProgram(0);

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }
}