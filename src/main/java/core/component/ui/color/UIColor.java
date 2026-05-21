package core.component.ui.color;

public class UIColor {
    public float r;
    public float g;
    public float b;
    public float a;

    public UIColor() {
        this(1, 1, 1, 1);
    }

    public UIColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public static UIColor white() {
        return new UIColor(1, 1, 1, 1);
    }

    public static UIColor black() {
        return new UIColor(0, 0, 0, 1);
    }

    public static UIColor transparent() {
        return new UIColor(0, 0, 0, 0);
    }
}