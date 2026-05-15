package core.component.tilemap;

public class Tile {
    private int tileId;
    private boolean solid;
    public Tile(){}
    public Tile(int tileId){
        this.tileId = tileId;
    }

    public int getTileId() { return tileId; }
    public void setTileId(int tileId) { this.tileId = tileId; }
    public boolean isSolid() { return solid; }
    public void setSolid(boolean solid) { this.solid = solid; }
}
