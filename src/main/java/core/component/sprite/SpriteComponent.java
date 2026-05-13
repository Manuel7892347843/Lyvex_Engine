package core.component.sprite;

import core.component.Component;

public class SpriteComponent extends Component {
    private Sprite sprite;
    private String spriteAssetPath = "";

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

    @Override
    public String toString() {
        return "SpriteComponent{" +
                "sprite=" + sprite +
                ", spriteAssetPath='" + spriteAssetPath + '\'' +
                '}';
    }
}
