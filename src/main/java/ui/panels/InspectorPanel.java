package ui.panels;

import core.assetmanager.AssetManager;
import core.component.tilemap.Tilemap;
import core.component.ui.color.UIColor;
import core.component.ui.uiElements.UIButton;
import core.component.ui.uiElements.UIImage;
import core.gameobject.GameObject;
import core.math.vector2D;
import core.math.vector2f;
import core.math.vector3f;
import core.scriptutil.ScriptComponentRegistry;
import core.component.sprite.SpriteLoader;
import core.component.Component;
import core.component.sprite.Sprite;
import core.component.sprite.SpriteComponent;
import core.component.Transform;
import core.component.ui.uiElements.UIText;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import ui.EditorContext;
import ui.EditorPanel;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class InspectorPanel implements EditorPanel {
    private final int POS_X = 1570;
    private final int POS_Y = 19;
    private final int WIDTH = 350;
    private final int HEIGHT = 637;

    private SpriteComponent spritePickerTarget;
    private UIText fontPickerTarget;
    private Tilemap editingTilemap = null;
    private int selectedTileId = 1;
    private float paletteZoom = 1.0f;
    private ImBoolean tilemapEditorOpen = new ImBoolean(false);

    // NUOVO: target generico per lo sprite picker (UIImage / UIButton)
    private SpritePickerTarget currentSpritePicker = null;

    @Override
    public void init() {
        ImGui.setNextWindowPos(POS_X, POS_Y, ImGuiCond.Always);
        ImGui.setNextWindowSize(WIDTH, HEIGHT, ImGuiCond.Always);
    }

    @Override
    public void draw(EditorContext context) {
        init();

        ImGui.begin("Inspector", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);
        ImGui.text("Inspector");
        ImGui.separator();

        GameObject selected = context.getSelectedGameObject();

        if (selected == null) {
            ImGui.text("No object selected");
            ImGui.end();
            return;
        }

        ImGui.text("Selected GameObject");
        ImGui.separator();
        ImGui.text("ID: " + selected.getId());

        if(ImGui.button("Add GameObject Children")){
            GameObject child = new GameObject("New Child", context);
            context.getCurrentScene().addChildObject(context.getSelectedGameObject(), child);
            context.setSelectedGameObject(child);
            context.setSceneDirty(true);
        }

        if(ImGui.button("Remove GameObject")){
            selected.destroy(context);
            context.setSelectedGameObject(null);
            context.setSceneDirty(true);
        }

        ImString name = new ImString(selected.getName(), 128);
        if (ImGui.inputText("Name", name, ImGuiInputTextFlags.EnterReturnsTrue)) {
            selected.setName(name.get());
            context.setSceneDirty(true);
        }

        ImGui.separator();
        ImGui.text("Components: " + selected.getComponents().size());

        if (ImGui.button("Add Component")) {
            ImGui.openPopup("AddComponentPopup");
        }

        if (ImGui.beginPopup("AddComponentPopup")) {
            for (Class<? extends Component> componentClass : ScriptComponentRegistry.getComponentClasses()) {
                String componentName = componentClass.getSimpleName();
                if (ImGui.menuItem(componentName)) {
                    try {
                        Component component = componentClass.getDeclaredConstructor().newInstance();
                        selected.addComponent(component);
                        context.setSceneDirty(true);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create component: " + componentName, e);
                    }
                }
            }
            ImGui.endPopup();
        }

        ImGui.separator();
        ImGui.text("Component List:");

        List<Component> removableComponents = new ArrayList<>();

        for (Component component : selected.getComponents()) {
            ImGui.separator();
            ImGui.text(component.getClass().getSimpleName());

            drawComponentFields(component, context);

            if (component instanceof UIText text) {
                drawUITextFontField(text, context);
            }

            // ============== SPRITE COMPONENT ==============
            if (component instanceof SpriteComponent spriteComponent) {
                drawSpriteField(spriteComponent, context);

                ImGui.text("Sorting");

                ImString layer = new ImString(spriteComponent.getSortingLayer(), 64);
                if (ImGui.inputText("Sorting Layer", layer)) {
                    spriteComponent.setSortingLayer(layer.get());
                    context.setSceneDirty(true);
                }

                int[] order = { spriteComponent.getSortingOrder() };
                if (ImGui.dragInt("Order in Layer", order, 1)) {
                    spriteComponent.setSortingOrder(order[0]);
                    context.setSceneDirty(true);
                }
            }

            // ============== UI IMAGE ==============
            if (component instanceof UIImage image) {
                drawUIImageSpriteField(image, context);
            }

            // ============== UI BUTTON ==============
            if (component instanceof UIButton button) {
                drawUIButtonSpriteFields(button, context);
            }

            if (component instanceof Transform transform) {
                drawTransformField(transform, context);
                ImGui.text("Transform is required and cannot be removed.");
            } else if (component instanceof Tilemap tilemap) {
                drawTilemapEditor(tilemap, context);
                if (ImGui.button("Remove##" + component.hashCode())) {
                    removableComponents.add(component);
                }
            } else {
                if (ImGui.button("Remove##" + component.hashCode())) {
                    removableComponents.add(component);
                }
            }
        }

        for (Component component : removableComponents) {
            selected.removeComponent(component);
            context.setSceneDirty(true);
        }

        drawSpritePickerPopup(context);
        drawFontPickerPopup(context);

        ImGui.end();

        if (tilemapEditorOpen.get() && editingTilemap != null) {
            drawTilemapEditorWindow(context);
        }
    }

    private void drawComponentFields(Component component, EditorContext context) {
        Field[] fields = component.getClass().getFields();

        for (Field field : fields) {
            try {
                Class<?> type = field.getType();
                String fieldName = field.getName();
                Object value = field.get(component);

                // Salta i campi sprite/path che gestiamo con i picker dedicati
                if (fieldName.equals("sprite") || fieldName.endsWith("SpritePath") || fieldName.equals("spriteAssetPath")) {
                    continue;
                }

                if(type == vector2D.class || type.getSimpleName().equals("vector2D")){
                    vector2D vec = (vector2D) value;
                    if (vec == null) {
                        vec = new vector2D(0, 0);
                        field.set(component, vec);
                    }

                    float[] v = { vec.x, vec.y };
                    if (ImGui.dragFloat2(fieldName, v, 0.05f)) {
                        vec.x = v[0];
                        vec.y = v[1];
                        context.setSceneDirty(true);
                    }
                    continue;
                }

                if(type == vector3f.class || type.getSimpleName().equals("vector3f")){
                    vector3f vec = (vector3f) value;
                    if(vec == null){
                        vec = new vector3f(0, 0, 0);
                        field.set(component, vec);
                    }

                    float[] v = {vec.x, vec.y, vec.z};
                    if(ImGui.dragFloat3(fieldName, v, 0.05f)){
                        vec.x = v[0];
                        vec.y = v[1];
                        vec.z = v[2];
                        context.setSceneDirty(true);
                    }
                    continue;
                }

                if(type == UIColor.class || type.getSimpleName().equals("UIColor")){
                    UIColor color = (UIColor)value;
                    if(color == null){
                        color = new UIColor(1, 1, 1, 1);
                        field.set(component, color);
                    }

                    float[] c = { color.r, color.g, color.b, color.a };
                    if(ImGui.colorEdit4(fieldName, c)){
                        color.r = clamp01(c[0]);
                        color.g = clamp01(c[1]);
                        color.b = clamp01(c[2]);
                        color.a = clamp01(c[3]);
                        field.set(component, color);
                        context.setSceneDirty(true);
                    }
                    continue;
                }

                if (type == float.class) {
                    float[] v = { field.getFloat(component) };
                    if (ImGui.dragFloat(fieldName, v, 0.05f)) {
                        field.setFloat(component, v[0]);
                        context.setSceneDirty(true);
                    }
                } else if (type == int.class) {
                    int[] v = { field.getInt(component) };
                    if (ImGui.dragInt(fieldName, v, 1.0f)) {
                        field.setInt(component, v[0]);
                        context.setSceneDirty(true);
                    }
                } else if (type == boolean.class) {
                    boolean current = field.getBoolean(component);
                    if (ImGui.button(fieldName + ": " + (current ? "true" : "false"))) {
                        field.setBoolean(component, !current);
                        context.setSceneDirty(true);
                    }
                } else if (type == String.class) {
                    ImString str = new ImString(value != null ? value.toString() : "", 256);
                    if (ImGui.inputText(fieldName, str)) {
                        field.set(component, str.get());
                        context.setSceneDirty(true);
                    }
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    }

    public void drawTransformField(Transform transform, EditorContext context){
        ImGui.separator();
        ImGui.text("Position");
        float[] pos = {transform.getX(), transform.getY()};
        if(ImGui.dragFloat2("##Position", pos, 0.05f)){
            transform.setPosition(pos[0], pos[1]);
            context.setSceneDirty(true);
        }

        ImGui.text("Rotation");
        float[] rot = { transform.getRotation() };
        if (ImGui.dragFloat("##Rotation", rot, 0.5f)) {
            transform.setRotation(rot[0]);
            context.setSceneDirty(true);
        }

        ImGui.text("Scale");
        float[] scale = {transform.getScale().x, transform.getScale().y};
        if(ImGui.dragFloat2("##Scale", scale, 0.05f)){
            transform.setScale(scale[0], scale[1]);
            context.setSceneDirty(true);
        }
    }

    // ============== SPRITE COMPONENT (esistente) ==============
    private void drawSpriteField(SpriteComponent spriteComponent, EditorContext context) {
        ImGui.separator();
        ImGui.text("Sprite");

        String spritePath = spriteComponent.getSpriteAssetPath();
        if (spritePath == null || spritePath.isBlank()) {
            ImGui.text("Selected: None");
        } else {
            ImGui.text("Selected: " + spritePath);
        }

        if (ImGui.button("Choose Sprite##SpriteComponent")) {
            spritePickerTarget = spriteComponent;
            currentSpritePicker = null;
            ImGui.openPopup("SpritePickerPopup");
        }
    }

    // ============== UI IMAGE SPRITE ==============
    private void drawUIImageSpriteField(UIImage image, EditorContext context) {
        ImGui.separator();
        ImGui.text("Image Sprite");

        String spritePath = image.getSpriteAssetPath();
        if (spritePath == null || spritePath.isBlank()) {
            ImGui.text("Selected: None");
        } else {
            ImGui.text("Selected: " + spritePath);
        }

        if (ImGui.button("Choose Sprite##UIImage")) {
            spritePickerTarget = null;
            currentSpritePicker = new SpritePickerTarget(
                    path -> image.setSpriteAssetPath(path),
                    image.getSpriteAssetPath()
            );
            ImGui.openPopup("SpritePickerPopup");
        }
        ImGui.sameLine();
        if (ImGui.button("Clear##UIImage")) {
            image.setSpriteAssetPath("");
            context.setSceneDirty(true);
        }
    }

    // ============== UI BUTTON SPRITES ==============
    private void drawUIButtonSpriteFields(UIButton button, EditorContext context) {
        ImGui.separator();
        ImGui.text("Button Sprites");

        // Normal Sprite
        ImGui.text("Normal:");
        String normalPath = button.getNormalSpritePath();
        ImGui.text(normalPath == null || normalPath.isBlank() ? "  None" : "  " + normalPath);
        if (ImGui.button("Choose##BtnNormal")) {
            spritePickerTarget = null;
            currentSpritePicker = new SpritePickerTarget(
                    path -> button.setNormalSpritePath(path),
                    button.getNormalSpritePath()
            );
            ImGui.openPopup("SpritePickerPopup");
        }
        ImGui.sameLine();
        if (ImGui.button("Clear##BtnNormal")) {
            button.setNormalSpritePath("");
            context.setSceneDirty(true);
        }

        // Hover Sprite
        ImGui.text("Hover:");
        String hoverPath = button.getHoverSpritePath();
        ImGui.text(hoverPath == null || hoverPath.isBlank() ? "  None" : "  " + hoverPath);
        if (ImGui.button("Choose##BtnHover")) {
            spritePickerTarget = null;
            currentSpritePicker = new SpritePickerTarget(
                    path -> button.setHoverSpritePath(path),
                    button.getHoverSpritePath()
            );
            ImGui.openPopup("SpritePickerPopup");
        }
        ImGui.sameLine();
        if (ImGui.button("Clear##BtnHover")) {
            button.setHoverSpritePath("");
            context.setSceneDirty(true);
        }

        // Pressed Sprite
        ImGui.text("Pressed:");
        String pressedPath = button.getPressedSpritePath();
        ImGui.text(pressedPath == null || pressedPath.isBlank() ? "  None" : "  " + pressedPath);
        if (ImGui.button("Choose##BtnPressed")) {
            spritePickerTarget = null;
            currentSpritePicker = new SpritePickerTarget(
                    path -> button.setPressedSpritePath(path),
                    button.getPressedSpritePath()
            );
            ImGui.openPopup("SpritePickerPopup");
        }
        ImGui.sameLine();
        if (ImGui.button("Clear##BtnPressed")) {
            button.setPressedSpritePath("");
            context.setSceneDirty(true);
        }
    }

    // ============== SPRITE PICKER POPUP (modificato per supportare target generico) ==============
    private void drawSpritePickerPopup(EditorContext context) {
        if (ImGui.beginPopup("SpritePickerPopup")) {
            Path assetsRoot = AssetManager.getAssetPath();

            ImGui.text("Select a sprite from Assets");
            ImGui.separator();

            try (Stream<Path> paths = Files.walk(assetsRoot)) {
                paths.filter(Files::isRegularFile)
                        .filter(this::isImageFile)
                        .forEach(path -> {
                            String relative = assetsRoot.relativize(path).toString().replace('\\', '/');
                            if (ImGui.selectable(relative)) {
                                if (spritePickerTarget != null) {
                                    // Vecchio sistema per SpriteComponent
                                    spritePickerTarget.setSpriteAssetPath(relative);
                                    try {
                                        Path realPath = assetsRoot.resolve(relative);
                                        Sprite sprite = SpriteLoader.loadFromFile(realPath);
                                        spritePickerTarget.setSprite(sprite);
                                    } catch (Exception e) {
                                        throw new RuntimeException("Failed to load sprite: " + relative, e);
                                    }
                                } else if (currentSpritePicker != null) {
                                    // Nuovo sistema per UIImage/UIButton
                                    currentSpritePicker.onSelect.accept(relative);
                                }
                                context.setSceneDirty(true);
                                ImGui.closeCurrentPopup();
                            }
                        });
            } catch (IOException e) {
                ImGui.text("Failed to read assets folder");
            }

            if (ImGui.button("Close")) {
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }

    // ============== CLASSE HELPER PER LO SPRITE PICKER ==============
    private static class SpritePickerTarget {
        final Consumer<String> onSelect;
        final String currentPath;

        SpritePickerTarget(Consumer<String> onSelect, String currentPath) {
            this.onSelect = onSelect;
            this.currentPath = currentPath;
        }
    }

    private void drawUITextFontField(UIText text, EditorContext context) {
        ImGui.separator();
        ImGui.text("Font");

        String fontPath = text.getFontAssetPath();
        if (fontPath == null || fontPath.isBlank()) {
            ImGui.text("Selected Font Asset: None");
            ImGui.textDisabled("Using system font: " + text.getFontName());
        } else {
            ImGui.text("Selected Font Asset:");
            ImGui.textWrapped(fontPath);
        }

        if (ImGui.button("Choose Font")) {
            fontPickerTarget = text;
            ImGui.openPopup("FontPickerPopup");
        }

        ImGui.sameLine();

        if (ImGui.button("Clear Font")) {
            text.setFontAssetPath("");
            context.setSceneDirty(true);
        }
    }

    private void drawFontPickerPopup(EditorContext context) {
        if (ImGui.beginPopup("FontPickerPopup")) {
            Path assetsRoot = AssetManager.getAssetPath();

            ImGui.text("Select a font from Assets");
            ImGui.separator();

            try (Stream<Path> paths = Files.walk(assetsRoot)) {
                paths.filter(Files::isRegularFile)
                        .filter(this::isFontFile)
                        .forEach(path -> {
                            String relative = assetsRoot.relativize(path).toString().replace('\\', '/');

                            if (ImGui.selectable(relative)) {
                                if (fontPickerTarget != null) {
                                    fontPickerTarget.setFontAssetPath(relative);
                                }

                                context.setSceneDirty(true);
                                ImGui.closeCurrentPopup();
                            }
                        });
            } catch (IOException e) {
                ImGui.text("Failed to read assets folder");
            }

            if (ImGui.button("Close")) {
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }

    private boolean isFontFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".ttf")
                || name.endsWith(".otf");
    }

    private void drawTilemapEditor(Tilemap tilemap, EditorContext context) {
        ImGui.separator();
        ImGui.text("Tilemap");

        int[] tileSize = { tilemap.getTileSize() };
        if (ImGui.dragInt("Tile Size", tileSize, 1, 8, 256)) {
            tilemap.setTileSize(tileSize[0]);
            context.setSceneDirty(true);
        }

        ImGui.text("Sorting");

        ImString layer = new ImString(tilemap.getSortingLayer(), 64);
        if (ImGui.inputText("Sorting Layer", layer)) {
            tilemap.setSortingLayer(layer.get());
            context.setSceneDirty(true);
        }

        int[] order = { tilemap.getSortingOrder() };
        if (ImGui.dragInt("Order in Layer", order, 1)) {
            tilemap.setSortingOrder(order[0]);
            context.setSceneDirty(true);
        }

        ImGui.text("Tiles placed: " + tilemap.getTiles().size());

        ImString tilesetPath = new ImString(tilemap.getTilesetPath() != null ? tilemap.getTilesetPath() : "", 256);
        if (ImGui.inputText("Tileset Path", tilesetPath)) {
            tilemap.setTilesetPath(tilesetPath.get());
            tilemap.loadTileset();
            context.setSceneDirty(true);
        }
        ImGui.sameLine();
        if (ImGui.button("Choose##Tileset")) {
            ImGui.openPopup("TilesetPickerPopup");
        }

        if (ImGui.beginPopup("TilesetPickerPopup")) {
            Path assetsRoot = AssetManager.getAssetPath();
            try (Stream<Path> paths = Files.walk(assetsRoot)) {
                paths.filter(Files::isRegularFile)
                        .filter(this::isImageFile)
                        .forEach(path -> {
                            String relative = assetsRoot.relativize(path).toString().replace('\\', '/');
                            if (ImGui.selectable(relative)) {
                                tilemap.setTilesetPath(relative);
                                tilemap.loadTileset();
                                context.setSceneDirty(true);
                                ImGui.closeCurrentPopup();
                            }
                        });
            } catch (IOException e) {
                ImGui.text("Failed to read assets folder");
            }
            if (ImGui.button("Close")) {
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }

        if (ImGui.button("Open Tilemap Editor", 200, 30)) {
            tilemapEditorOpen.set(true);
            editingTilemap = tilemap;
        }
    }

    private void drawTilemapEditorWindow(EditorContext context) {
        ImGui.setNextWindowSize(800, 600, ImGuiCond.FirstUseEver);
        ImGui.begin("Tilemap Editor", tilemapEditorOpen, ImGuiWindowFlags.MenuBar);

        if (!tilemapEditorOpen.get()) {
            editingTilemap = null;
            ImGui.end();
            return;
        }

        Tilemap tilemap = editingTilemap;

        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("Tools")) {
                if (ImGui.menuItem("Clear All")) {
                    tilemap.getTiles().clear();
                    context.setSceneDirty(true);
                }
                ImGui.endMenu();
            }
            ImGui.endMenuBar();
        }

        float paletteWidth = 350;

        ImVec2 windowPos = ImGui.getWindowPos();
        float controlsY = ImGui.getCursorPosY();

        ImGui.text("Palette");
        ImGui.sameLine();
        if (ImGui.button("-", 24, 24)) {
            paletteZoom = Math.max(0.25f, paletteZoom - 0.25f);
        }
        ImGui.sameLine();
        ImGui.text(String.format("%.0f%%", paletteZoom * 100));
        ImGui.sameLine();
        if (ImGui.button("+", 24, 24)) {
            paletteZoom = Math.min(4.0f, paletteZoom + 0.25f);
        }
        ImGui.sameLine();
        if (ImGui.button("Reset", 50, 24)) {
            paletteZoom = 1.0f;
        }
        ImGui.separator();

        float controlsHeight = ImGui.getCursorPosY() - controlsY;
        float availableHeight = ImGui.getContentRegionAvail().y;

        ImGui.beginChild("Palette", paletteWidth, availableHeight, true, ImGuiWindowFlags.HorizontalScrollbar);

        if (tilemap.getTilesetSprite() != null) {
            int texId = tilemap.getTilesetSprite().getTextureId();
            float texWidth = tilemap.getTilesetSprite().getWidth();
            float texHeight = tilemap.getTilesetSprite().getHeight();
            float tileSize = tilemap.getTileSize();

            int tilesPerRow = (int)(texWidth / tileSize);
            int tilesPerCol = (int)(texHeight / tileSize);
            int totalTiles = tilesPerRow * tilesPerCol;
            int basePaletteTileSize = 48;
            int pSize = (int)(basePaletteTileSize * paletteZoom);

            ImGui.pushID("palette_eraser");
            boolean sel = (selectedTileId == 0);
            if (sel) {
                ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.6f, 1.0f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.7f, 1.0f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.6f, 1.0f, 1.0f);
            }
            if (ImGui.button("Er", pSize, pSize)) {
                selectedTileId = 0;
            }
            if (sel) ImGui.popStyleColor(3);
            ImGui.popID();

            for (int i = 1; i <= totalTiles; i++) {
                ImGui.pushID("palette_" + i);
                boolean isSel = (selectedTileId == i);

                if (i > 1 && (i - 1) % tilesPerRow != 0) {
                    ImGui.sameLine(0, 2);
                }

                if (isSel) {
                    ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.6f, 1.0f, 1.0f);
                    ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.7f, 1.0f, 1.0f);
                    ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.6f, 1.0f, 1.0f);
                }

                int tileX = (i - 1) % tilesPerRow;
                int tileY = (i - 1) / tilesPerRow;

                float u1 = (tileX * tileSize) / texWidth;
                float v1 = (tileY * tileSize) / texHeight;
                float u2 = ((tileX + 1) * tileSize) / texWidth;
                float v2 = ((tileY + 1) * tileSize) / texHeight;

                boolean clicked = ImGui.imageButton(texId, pSize, pSize, u1, v1, u2, v2);

                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("Tile " + i);
                }

                if (isSel) ImGui.popStyleColor(3);
                if (clicked) selectedTileId = i;

                ImGui.popID();
            }

            ImGui.separator();
            if (selectedTileId == 0) {
                ImGui.textColored(1.0f, 0.3f, 0.3f, 1.0f, "Tool: Eraser");
            } else {
                int selCol = (selectedTileId - 1) % tilesPerRow;
                int selRow = (selectedTileId - 1) / tilesPerRow;
                ImGui.text("Selected: Tile " + selectedTileId + " (col " + selCol + ", row " + selRow + ")");
            }

        } else {
            ImGui.text("No tileset loaded!");
        }

        ImGui.endChild();
        ImGui.sameLine();

        float canvasHeight = ImGui.getContentRegionAvail().y;
        ImGui.beginChild("Canvas", 0, canvasHeight, true);

        ImVec2 canvasPos = ImGui.getCursorScreenPos();
        ImVec2 mousePos = ImGui.getMousePos();
        vector2f relMouse = new vector2f(mousePos.x - canvasPos.x, mousePos.y - canvasPos.y);

        int tileSize = tilemap.getTileSize();
        int hoverX = (int)(relMouse.x / tileSize);
        int hoverY = (int)(relMouse.y / tileSize);

        float offsetX = 0, offsetY = 0;

        ImVec2 canvasSize = ImGui.getContentRegionAvail();
        int colsVisible = (int)(canvasSize.x / tileSize) + 2;
        int rowsVisible = (int)(canvasSize.y / tileSize) + 2;
        int startCol = (int)(-offsetX / tileSize);
        int startRow = (int)(-offsetY / tileSize);

        boolean mouseDown = ImGui.isMouseDown(0);
        boolean mouseClicked = ImGui.isMouseClicked(0);

        if (tilemap.getTilesetSprite() != null) {
            int texId = tilemap.getTilesetSprite().getTextureId();
            float texWidth = tilemap.getTilesetSprite().getWidth();
            float texHeight = tilemap.getTilesetSprite().getHeight();
            int tpr = (int)(texWidth / tileSize);

            for (Map.Entry<String, Integer> entry : tilemap.getTiles().entrySet()) {
                String[] p = entry.getKey().split(",");
                int tx = Integer.parseInt(p[0]);
                int ty = Integer.parseInt(p[1]);
                int tileId = entry.getValue();
                if (tileId <= 0) continue;

                float sx = (float)Math.floor(canvasPos.x + tx * tileSize + offsetX);
                float sy = (float)Math.floor(canvasPos.y + ty * tileSize + offsetY);

                int tileCol = (tileId - 1) % tpr;
                int tileRow = (tileId - 1) / tpr;

                float u1 = (tileCol * tileSize) / texWidth;
                float v1 = (tileRow * tileSize) / texHeight;
                float u2 = ((tileCol + 1) * tileSize) / texWidth;
                float v2 = ((tileRow + 1) * tileSize) / texHeight;

                ImGui.getWindowDrawList().addImage(texId, sx, sy, sx + tileSize, sy + tileSize, u1, v1, u2, v2);
            }
        }

        for (int row = startRow; row < startRow + rowsVisible; row++) {
            for (int col = startCol; col < startCol + colsVisible; col++) {
                float x = (float)Math.floor(canvasPos.x + col * tileSize + offsetX);
                float y = (float)Math.floor(canvasPos.y + row * tileSize + offsetY);
                ImGui.getWindowDrawList().addRect(x, y, x + tileSize, y + tileSize, 0x40FFFFFF);
            }
        }

        if (hoverX >= startCol && hoverX < startCol + colsVisible &&
                hoverY >= startRow && hoverY < startRow + rowsVisible) {
            float hx = (float)Math.floor(canvasPos.x + hoverX * tileSize + offsetX);
            float hy = (float)Math.floor(canvasPos.y + hoverY * tileSize + offsetY);
            ImGui.getWindowDrawList().addRectFilled(hx, hy, hx + tileSize, hy + tileSize, 0x40FF0000);
        }

        if (ImGui.isWindowHovered() && (mouseClicked || (mouseDown && ImGui.isMouseDragging(0)))) {
            tilemap.setTile(hoverX, hoverY, selectedTileId);
            context.setSceneDirty(true);
        }

        ImGui.text("Hover: " + hoverX + ", " + hoverY);
        ImGui.text("Tiles: " + tilemap.getTiles().size());

        ImGui.endChild();
        ImGui.end();
    }

    private boolean isImageFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".png")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".bmp")
                || name.endsWith(".gif");
    }

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    @Override
    public void optionsMenu() {
    }
}