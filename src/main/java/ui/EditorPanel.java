package ui;

import java.io.IOException;

public interface EditorPanel {
    void init();
    void draw(EditorContext context) throws IOException;
    void optionsMenu();
}
