package core.component;

public class Sprite {
    private final int textureId;
    private final int width;
    private final int height;

    public Sprite(int textureId, int width, int height) {
        this.textureId = textureId;
        this.width = width;
        this.height = height;
    }

    public int getTextureId() {
        return textureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}