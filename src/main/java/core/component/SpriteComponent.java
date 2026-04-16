package core.component;

public class SpriteComponent extends Component {
    private Sprite sprite;
    public String spriteAssetPath = "";

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
}
