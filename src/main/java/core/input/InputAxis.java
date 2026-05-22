package core.input;

import java.util.ArrayList;
import java.util.List;

public class InputAxis {
    public String name;
    public float deadZone = 0.1f;
    public float sensitivity = 1.0f;
    public boolean invert = false;

    public List<AxisBinding> bindings = new ArrayList<>();

    public InputAxis() {}

    public InputAxis(String name) {
        this.name = name;
    }

    public static class AxisBinding {
        public boolean positive = true;
        public Key key;
        public int mouseButton = -1;

        public AxisBinding() {}

        public AxisBinding(boolean positive, Key key) {
            this.positive = positive;
            this.key = key;
        }

        public AxisBinding(boolean positive, int mouseButton) {
            this.positive = positive;
            this.mouseButton = mouseButton;
        }
    }
}