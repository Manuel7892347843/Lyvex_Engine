package core.input;

import java.util.ArrayList;
import java.util.List;

public class InputAction {
    public String name;
    public List<ActionBinding> bindings = new ArrayList<>();

    public InputAction() {}

    public InputAction(String name) {
        this.name = name;
    }

    public static class ActionBinding {
        public Key key;
        public int mouseButton = -1;
        public boolean shift = false;
        public boolean ctrl = false;
        public boolean alt = false;

        public ActionBinding() {}

        public ActionBinding(Key key) {
            this.key = key;
        }

        public ActionBinding(int mouseButton) {
            this.mouseButton = mouseButton;
        }

        public ActionBinding(Key key, boolean shift, boolean ctrl, boolean alt) {
            this.key = key;
            this.shift = shift;
            this.ctrl = ctrl;
            this.alt = alt;
        }
    }
}