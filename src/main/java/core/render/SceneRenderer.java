package core.render;

import core.Engine;
import core.ProjectSettings;
import core.component.Camera;
import core.component.EditorCamera2D;
import core.component.sprite.Sprite;
import core.component.sprite.SpriteComponent;
import core.component.tilemap.Tilemap;
import core.component.ui.color.UIColor;
import core.component.ui.uiElements.UIButton;
import core.component.ui.uiElements.UIImage;
import core.component.ui.uiElements.UIPanel;
import core.component.ui.uiElements.UIText;
import core.gameobject.GameObject;
import core.input.InputManager;
import core.math.matrix4f;
import core.scene.Scene;
import ui.EditorContext;

import core.component.ui.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private int targetDisplay = 1;
    private int shaderProgram;
    private int vao;
    private int vbo;

    private static class UIRenderable {
        Canvas canvas;
        GameObject gameObject;
        UIElement element;
        int canvasOrder;
        int elementOrder;
    }

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

    public void setTargetDisplay(int targetDisplay) {
        this.targetDisplay = Math.max(1, targetDisplay);
    }

    public int getTargetDisplay() {
        return targetDisplay;
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

        float scroll = InputManager.getScrollY();
        if (scroll != 0) {
            float newZoom = editorCamera.getZoom() + scroll * editorCamera.getZoomSpeed();
            if (newZoom < 0.1f) newZoom = 0.1f;
            if (newZoom > 10.0f) newZoom = 10.0f;
            editorCamera.setZoom(newZoom);
        }
    }

    private static class Renderable {
        GameObject gameObject;
        SpriteComponent sprite;
        Tilemap tilemap;
        float worldY;
        int layerPriority;
        int sortingOrder;
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

        matrix4f viewProj;
        if (useSceneCamera) {
            Camera sceneCamera = findCameraForDisplay(targetDisplay);
            if (sceneCamera == null) {
                glUseProgram(0);
                return;
            }

            viewProj = sceneCamera.getViewProjectionMatrix(viewportWidth, viewportHeight);
        } else {
            viewProj = getEditorCameraMatrix();
        }

        int vpLocation = glGetUniformLocation(shaderProgram, "uViewProjection");
        if (vpLocation != -1) {
            float[] vpArray = new float[16];
            viewProj.get(vpArray);
            glUniformMatrix4fv(vpLocation, false, vpArray);
        }

        int modelLocation = glGetUniformLocation(shaderProgram, "uModel");

        List<Renderable> renderables = new ArrayList<>();
        for (GameObject root : scene.getRootObjects()) {
            collectRenderables(root, renderables);
        }

        renderables.sort((a, b) -> {
            if (a.layerPriority != b.layerPriority) {
                return Integer.compare(a.layerPriority, b.layerPriority);
            }
            return Integer.compare(a.sortingOrder, b.sortingOrder);
        });

        glBindVertexArray(vao);

        for (Renderable r : renderables) {
            if (r.sprite != null) {
                renderSprite(r.gameObject, r.sprite, modelLocation);
            } else if (r.tilemap != null) {
                renderTilemap(r.tilemap, modelLocation);
            }
        }

        renderUI(modelLocation, vpLocation);

        glBindVertexArray(0);
        glUseProgram(0);
    }

    private void renderUI(int modelLocation, int vpLocation) {
        List<UIRenderable> uiRenderables = new ArrayList<>();

        for (GameObject root : scene.getRootObjects()) {
            collectUIRenderables(root, null, uiRenderables);
        }

        uiRenderables.sort((a, b) -> {
            if (a.canvasOrder != b.canvasOrder) {
                return Integer.compare(a.canvasOrder, b.canvasOrder);
            }

            return Integer.compare(a.elementOrder, b.elementOrder);
        });

        matrix4f uiProjection = new matrix4f().ortho(
                0.0f,
                viewportWidth,
                0.0f,
                viewportHeight,
                -100.0f,
                100.0f
        );

        if (vpLocation != -1) {
            float[] vpArray = new float[16];
            uiProjection.get(vpArray);
            glUniformMatrix4fv(vpLocation, false, vpArray);
        }

        EditorContext context = EditorContext.getInstance();

        float viewportScreenX = useSceneCamera ? context.getGameViewportX() : context.getSceneViewportX();
        float viewportScreenY = useSceneCamera ? context.getGameViewportY() : context.getSceneViewportY();
        float viewportScreenWidth = useSceneCamera ? context.getGameViewportWidth() : context.getSceneViewportWidth();
        float viewportScreenHeight = useSceneCamera ? context.getGameViewportHeight() : context.getSceneViewportHeight();
        boolean viewportHovered = useSceneCamera ? context.isGameHovered() : context.isSceneHovered();

        float mouseX = InputManager.getMouseX();
        float mouseY = InputManager.getMouseY();

        boolean mouseInsideViewport =
                viewportHovered
                        && mouseX >= viewportScreenX
                        && mouseX <= viewportScreenX + viewportScreenWidth
                        && mouseY >= viewportScreenY
                        && mouseY <= viewportScreenY + viewportScreenHeight;

        float framebufferMouseX = -999999.0f;
        float framebufferMouseY = -999999.0f;

        if (mouseInsideViewport && viewportScreenWidth > 0 && viewportScreenHeight > 0) {
            float localMouseX = mouseX - viewportScreenX;
            float localMouseY = mouseY - viewportScreenY;

            framebufferMouseX = localMouseX * ((float) viewportWidth / viewportScreenWidth);
            framebufferMouseY = viewportHeight - localMouseY * ((float) viewportHeight / viewportScreenHeight);
        }

        for (UIRenderable renderable : uiRenderables) {
            Canvas canvas = renderable.canvas;
            UIElement element = renderable.element;

            if (!element.isVisible()) {
                continue;
            }

            if (!useSceneCamera && !canvas.isVisibleInSceneView()) {
                continue;
            }

            float scaleX = canvas.getScaleX(viewportWidth);
            float scaleY = canvas.getScaleY(viewportHeight);

            float uiMouseX = framebufferMouseX / scaleX;
            float uiMouseY = framebufferMouseY / scaleY;

            if (element instanceof UIButton button) {
                button.processInput(uiMouseX, uiMouseY);
                renderButton(button, scaleX, scaleY, modelLocation);
            } else if (element instanceof UIImage image) {
                renderImage(image, scaleX, scaleY, modelLocation);
            } else if (element instanceof UIText text) {
                renderText(text, scaleX, scaleY, modelLocation);
            } else if (element instanceof UIPanel panel) {
                renderPanel(panel, scaleX, scaleY, modelLocation);
            }
        }
    }

    private void collectUIRenderables(GameObject gameObject, Canvas currentCanvas, List<UIRenderable> list) {
        if (!gameObject.isActive()) {
            return;
        }

        Canvas canvas = gameObject.getComponent(Canvas.class);
        if (canvas != null) {
            currentCanvas = canvas;
        }

        UIElement element = gameObject.getComponent(UIElement.class);
        if (currentCanvas != null && element != null) {
            UIRenderable renderable = new UIRenderable();
            renderable.canvas = currentCanvas;
            renderable.gameObject = gameObject;
            renderable.element = element;
            renderable.canvasOrder = currentCanvas.getSortingOrder();
            renderable.elementOrder = element.getOrder();
            list.add(renderable);
        }

        for (GameObject child : gameObject.getChildren()) {
            collectUIRenderables(child, currentCanvas, list);
        }
    }

    private void renderPanel(UIPanel panel, float scaleX, float scaleY, int modelLocation) {
        renderColoredQuad(
                panel.getX() * scaleX,
                panel.getY() * scaleY,
                panel.getWidth() * scaleX,
                panel.getHeight() * scaleY,
                panel.getPivotX(),
                panel.getPivotY(),
                panel.getColor(),
                modelLocation
        );
    }

    private void renderImage(UIImage image, float scaleX, float scaleY, int modelLocation) {
        renderTexturedQuad(
                image.getX() * scaleX,
                image.getY() * scaleY,
                image.getWidth() * scaleX,
                image.getHeight() * scaleY,
                image.getPivotX(),
                image.getPivotY(),
                image.getTint(),
                image.getSprite() == null ? 0 : image.getSprite().getTextureId(),
                modelLocation
        );
    }

    private void renderText(UIText text, float scaleX, float scaleY, int modelLocation) {
        if (text.getSprite() == null) {
            return;
        }

        renderTexturedQuad(
                text.getX() * scaleX,
                text.getY() * scaleY,
                text.getWidth() * scaleX,
                text.getHeight() * scaleY,
                text.getPivotX(),
                text.getPivotY(),
                UIColor.white(),
                text.getSprite().getTextureId(),
                modelLocation
        );
    }

    private void renderButton(UIButton button, float scaleX, float scaleY, int modelLocation) {
        int textureId = button.getCurrentSprite() == null ? 0 : button.getCurrentSprite().getTextureId();

        renderTexturedQuad(
                button.getX() * scaleX,
                button.getY() * scaleY,
                button.getWidth() * scaleX,
                button.getHeight() * scaleY,
                button.getPivotX(),
                button.getPivotY(),
                button.getCurrentColor(),
                textureId,
                modelLocation
        );
    }

    private void renderColoredQuad(float x, float y, float width, float height, float pivotX, float pivotY, UIColor color, int modelLocation) {
        renderTexturedQuad(x, y, width, height, pivotX, pivotY, color, 0, modelLocation);
    }

    private void renderTexturedQuad(float x, float y, float width, float height, float pivotX, float pivotY, UIColor color, int textureId, int modelLocation) {
        UIColor drawColor = color == null ? UIColor.white() : color;

        matrix4f model = new matrix4f()
                .translate(x - width * pivotX + width * 0.5f, y - height * pivotY + height * 0.5f, 0.0f)
                .scale(width, height, 1.0f);

        if (modelLocation != -1) {
            float[] modelArray = new float[16];
            model.get(modelArray);
            glUniformMatrix4fv(modelLocation, false, modelArray);
        }

        int uvOffsetLoc = glGetUniformLocation(shaderProgram, "uUVOffset");
        int uvScaleLoc = glGetUniformLocation(shaderProgram, "uUVScale");
        int colorLoc = glGetUniformLocation(shaderProgram, "uColor");
        int useTextureLoc = glGetUniformLocation(shaderProgram, "uUseTexture");

        if (uvOffsetLoc != -1) glUniform2f(uvOffsetLoc, 0, 0);
        if (uvScaleLoc != -1) glUniform2f(uvScaleLoc, 1, 1);
        if (colorLoc != -1) glUniform4f(colorLoc, drawColor.r, drawColor.g, drawColor.b, drawColor.a);
        if (useTextureLoc != -1) glUniform1i(useTextureLoc, textureId != 0 ? 1 : 0);

        if (textureId != 0) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureId);
        } else {
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        glDrawArrays(GL_TRIANGLES, 0, 6);

        if (colorLoc != -1) glUniform4f(colorLoc, 1, 1, 1, 1);
        if (useTextureLoc != -1) glUniform1i(useTextureLoc, 1);
    }

    private void collectRenderables(GameObject gameObject, List<Renderable> list) {
        if (!gameObject.isActive()) return;

        SpriteComponent sprite = gameObject.getComponent(SpriteComponent.class);
        if (sprite != null && sprite.getSprite() != null) {
            Renderable r = new Renderable();
            r.gameObject = gameObject;
            r.sprite = sprite;
            r.worldY = gameObject.getTransform().getY();
            r.layerPriority = getLayerPriority(sprite.getSortingLayer());
            r.sortingOrder = sprite.getSortingOrder();
            list.add(r);
        }

        Tilemap tilemap = gameObject.getComponent(Tilemap.class);
        if (tilemap != null && tilemap.getTilesetSprite() != null) {
            Renderable r = new Renderable();
            r.gameObject = gameObject;
            r.tilemap = tilemap;
            r.layerPriority = getLayerPriority(tilemap.getSortingLayer());
            r.sortingOrder = tilemap.getSortingOrder();
            list.add(r);
        }

        for (GameObject child : gameObject.getChildren()) {
            collectRenderables(child, list);
        }
    }

    private int getLayerPriority(String layerName) {
        return ProjectSettings.getSortingLayerManager().getLayerPriority(layerName);
    }

    private Camera findCameraForDisplay(int display) {
        if (scene == null) return null;

        for (GameObject go : scene.getRootObjects()) {
            Camera camera = findCameraForDisplayRecursive(go, display);

            if (camera != null) {
                return camera;
            }
        }

        return null;
    }

    private Camera findCameraForDisplayRecursive(GameObject gameObject, int display) {
        Camera camera = gameObject.getComponent(Camera.class);

        if (camera != null && camera.primary && camera.targetDisplay == display) {
            return camera;
        }

        for (GameObject child : gameObject.getChildren()) {
            Camera found = findCameraForDisplayRecursive(child, display);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private Camera findPrimaryCameraRecursive(GameObject gameObject) {
        Camera camera = gameObject.getComponent(Camera.class);

        if (camera != null && camera.primary) {
            return camera;
        }

        for (GameObject child : gameObject.getChildren()) {
            Camera found = findPrimaryCameraRecursive(child);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /*
    private Camera findPrimaryCamera() {
        return findCameraForDisplay(1);
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
     */

    private matrix4f getEditorCameraMatrix() {
        matrix4f view = new matrix4f()
                .translate(-editorCamera.getX(), -editorCamera.getY(), 0.0f);

        float aspect = (float) viewportWidth / viewportHeight;

        float halfHeight = editorCamera.getOrthoSize() / editorCamera.getZoom();
        float halfWidth = halfHeight * aspect;

        matrix4f proj = new matrix4f().ortho(
                -halfWidth, halfWidth,
                -halfHeight, halfHeight,
                -100.0f, 100.0f
        );

        return proj.mul(view);
    }

    private void renderGameObjectRecursive(GameObject gameObject, int modelLocation) {
        if (!gameObject.isActive()) return;

        SpriteComponent spriteComponent = gameObject.getComponent(SpriteComponent.class);
        if (spriteComponent != null && spriteComponent.getSprite() != null) {
            matrix4f model = gameObject.getTransform().getModelMatrix();
            if (modelLocation != -1) {
                float[] modelArray = new float[16];
                model.get(modelArray);
                glUniformMatrix4fv(modelLocation, false, modelArray);
            }

            int uvOffsetLoc = glGetUniformLocation(shaderProgram, "uUVOffset");
            int uvScaleLoc = glGetUniformLocation(shaderProgram, "uUVScale");
            if (uvOffsetLoc != -1) glUniform2f(uvOffsetLoc, 0, 0);
            if (uvScaleLoc != -1) glUniform2f(uvScaleLoc, 1, 1);

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, spriteComponent.getSprite().getTextureId());
            glDrawArrays(GL_TRIANGLES, 0, 6);
        }

        Tilemap tilemap = gameObject.getComponent(Tilemap.class);
        if (tilemap != null) {
            renderTilemap(tilemap, modelLocation);
        }

        for (GameObject child : gameObject.getChildren()) {
            renderGameObjectRecursive(child, modelLocation);
        }
    }

    private void renderSprite(GameObject gameObject, SpriteComponent spriteComponent, int modelLocation) {
        if (spriteComponent == null || spriteComponent.getSprite() == null) return;

        matrix4f model = gameObject.getTransform().getModelMatrix();
        if (modelLocation != -1) {
            float[] modelArray = new float[16];
            model.get(modelArray);
            glUniformMatrix4fv(modelLocation, false, modelArray);
        }

        int uvOffsetLoc = glGetUniformLocation(shaderProgram, "uUVOffset");
        int uvScaleLoc = glGetUniformLocation(shaderProgram, "uUVScale");
        if (uvOffsetLoc != -1) glUniform2f(uvOffsetLoc, 0, 0);
        if (uvScaleLoc != -1) glUniform2f(uvScaleLoc, 1, 1);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, spriteComponent.getSprite().getTextureId());
        glDrawArrays(GL_TRIANGLES, 0, 6);
    }

    private void renderTilemap(Tilemap tilemap, int modelLocation) {
        Sprite tileset = tilemap.getTilesetSprite();
        if (tileset == null) return;

        int tileSize = tilemap.getTileSize();
        int tilesPerRow = tilemap.getTilesPerRow();
        if (tilesPerRow <= 0) return;

        int texId = tileset.getTextureId();
        float texWidth = tileset.getWidth();
        float texHeight = tileset.getHeight();

        int uvOffsetLoc = glGetUniformLocation(shaderProgram, "uUVOffset");
        int uvScaleLoc = glGetUniformLocation(shaderProgram, "uUVScale");

        matrix4f modelMatrix = new matrix4f();

        for (Map.Entry<String, Integer> entry : tilemap.getTiles().entrySet()) {
            String[] parts = entry.getKey().split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int tileId = entry.getValue();

            if (tileId <= 0) continue;

            int tileX = (tileId - 1) % tilesPerRow;
            int tileY = (tileId - 1) / tilesPerRow;

            float u1 = (tileX * tileSize) / texWidth;
            float v1 = (tileY * tileSize) / texHeight;
            float uSize = (float) tileSize / texWidth;
            float vSize = (float) tileSize / texHeight;

            if (uvOffsetLoc != -1) glUniform2f(uvOffsetLoc, u1, v1);
            if (uvScaleLoc != -1) glUniform2f(uvScaleLoc, uSize, vSize);

            float worldScale = tileSize / tilemap.getPixelsPerUnit();
            float worldX = x * worldScale;
            float worldY = y * worldScale;

            modelMatrix.identity()
                    .translate(worldX, worldY, 0)
                    .scale(worldScale, worldScale, 1);

            if (modelLocation != -1) {
                float[] modelArray = new float[16];
                modelMatrix.get(modelArray);
                glUniformMatrix4fv(modelLocation, false, modelArray);
            }

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, texId);
            glDrawArrays(GL_TRIANGLES, 0, 6);
        }

        if (uvOffsetLoc != -1) glUniform2f(uvOffsetLoc, 0, 0);
        if (uvScaleLoc != -1) glUniform2f(uvScaleLoc, 1, 1);
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
        String vertexShaderSource =
                "#version 330 core\n" +
                        "layout (location = 0) in vec2 aPos;\n" +
                        "layout (location = 1) in vec2 aUV;\n" +
                        "out vec2 vUV;\n" +
                        "uniform mat4 uViewProjection;\n" +
                        "uniform mat4 uModel;\n" +
                        "uniform vec2 uUVOffset;\n" +
                        "uniform vec2 uUVScale;\n" +
                        "void main() {\n" +
                        "    vUV = uUVOffset + aUV * uUVScale;\n" +
                        "    vec4 worldPos = uModel * vec4(aPos, 0.0, 1.0);\n" +
                        "    gl_Position = uViewProjection * worldPos;\n" +
                        "}";

        String fragmentShaderSource =
                "#version 330 core\n" +
                        "in vec2 vUV;\n" +
                        "out vec4 FragColor;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "uniform vec4 uColor;\n" +
                        "uniform int uUseTexture;\n" +
                        "void main() {\n" +
                        "    vec4 baseColor = uUseTexture == 1 ? texture(uTexture, vUV) : vec4(1.0);\n" +
                        "    FragColor = baseColor * uColor;\n" +
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
        glUniform4f(glGetUniformLocation(shaderProgram, "uColor"), 1, 1, 1, 1);
        glUniform1i(glGetUniformLocation(shaderProgram, "uUseTexture"), 1);
        glUseProgram(0);
        glUseProgram(0);

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }
}