package core.component.tilemap;

import core.component.Component;
import core.component.sprite.Sprite;
import core.component.sprite.SpriteLoader;
import core.assetmanager.AssetManager;
import core.math.vector2D;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Tilemap extends Component {
    private int tileSize = 32;
    private float pixelsPerUnit = 100.0f;
    private String tilesetPath = "";
    private Map<String, Integer> tiles = new HashMap<>();

    private transient int minX = 0, minY = 0, maxX = 0, maxY = 0;
    private String sortingLayer = "Default";
    private int sortingOrder = 0;

    // Cache
    private transient Sprite tilesetSprite;
    private transient int tilesPerRow = 0;

    public Tilemap() {}

    // Genera chiave dalla posizione
    private String key(int x, int y) {
        return x + "," + y;
    }

    public void setTile(int x, int y, int tileId) {
        String k = key(x, y);
        if (tileId == 0) {
            tiles.remove(k); // 0 = cancella
        } else {
            tiles.put(k, tileId);
        }
        updateBounds();
    }

    public int getTile(int x, int y) {
        return tiles.getOrDefault(key(x, y), 0);
    }

    public boolean hasTile(int x, int y) {
        return tiles.containsKey(key(x, y));
    }

    private void updateBounds() {
        if (tiles.isEmpty()) {
            minX = minY = maxX = maxY = 0;
            return;
        }

        minX = Integer.MAX_VALUE;
        minY = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxY = Integer.MIN_VALUE;

        for (String k : tiles.keySet()) {
            String[] parts = k.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);

            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
    }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }

    public int getWidth() {
        if (tiles.isEmpty()) return 0;
        return maxX - minX + 1;
    }

    public int getHeight() {
        if (tiles.isEmpty()) return 0;
        return maxY - minY + 1;
    }

    public void setWidth(int width) {
        // Non fa nulla - la tilemap è infinita
    }

    public void setHeight(int height) {
        // Non fa nulla - la tilemap è infinita
    }

    public int getTileSize() { return tileSize; }
    public void setTileSize(int tileSize) { this.tileSize = tileSize; }

    public String getTilesetPath() { return tilesetPath; }
    public void setTilesetPath(String tilesetPath) { this.tilesetPath = tilesetPath; }

    public Map<String, Integer> getTiles() { return tiles; }

    public void loadTileset() {
        if (tilesetPath == null || tilesetPath.isBlank()) {
            tilesetSprite = null;
            tilesPerRow = 0;
            return;
        }

        try {
            Path path = AssetManager.getAssetPath().resolve(tilesetPath);
            Sprite sprite = SpriteLoader.loadFromFile(path);
            setTilesetSprite(sprite);
        } catch (Exception e) {
            System.err.println("Failed to load tileset: " + tilesetPath);
            e.printStackTrace();
            tilesetSprite = null;
            tilesPerRow = 0;
        }
    }

    public Sprite getTilesetSprite() { return tilesetSprite; }

    public void setTilesetSprite(Sprite sprite) {
        this.tilesetSprite = sprite;
        if (sprite != null && tileSize > 0) {
            this.tilesPerRow = sprite.getWidth() / tileSize;
        }
    }

    public int getTilesPerRow() { return tilesPerRow; }
    public float getPixelsPerUnit() { return pixelsPerUnit; }
    public void setPixelsPerUnit(float ppu) { this.pixelsPerUnit = ppu; }
    public String getSortingLayer() { return sortingLayer; }
    public void setSortingLayer(String sortingLayer) { this.sortingLayer = sortingLayer; }
    public int getSortingOrder() { return sortingOrder; }
    public void setSortingOrder(int sortingOrder) { this.sortingOrder = sortingOrder; }
}