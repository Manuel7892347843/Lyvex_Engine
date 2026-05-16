package core.component.sprite;

import core.component.Component;

public class SpriteComponent extends Component {
    private Sprite sprite;
    private String spriteAssetPath = "";
    private String sortingLayer = "Default";
    private int sortingOrder = 0;

    public SpriteComponent() {
    }

    public SpriteComponent(Sprite sprite) {
        this.sprite = sprite;
    }

    public Sprite getSprite() {
        return sprite;
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }

    public String getSpriteAssetPath() {
        return spriteAssetPath;
    }

    public void setSpriteAssetPath(String spriteAssetPath) {
        this.spriteAssetPath = spriteAssetPath;
    }

    public String getSortingLayer() {
        return sortingLayer;
    }

    public void setSortingLayer(String sortingLayer){
        this.sortingLayer = sortingLayer;
    }
    public int getSortingOrder(){
        return sortingOrder;
    }

    public void setSortingOrder(int sortingOrder){
        this.sortingOrder = sortingOrder;
    }

    @Override
    public String toString() {
        return "SpriteComponent{" +
                "sprite=" + sprite +
                ", spriteAssetPath='" + spriteAssetPath + '\'' +
                '}';
    }
}
