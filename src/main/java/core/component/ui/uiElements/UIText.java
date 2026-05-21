package core.component.ui.uiElements;

import core.assetmanager.AssetManager;
import core.component.sprite.Sprite;
import core.component.sprite.SpriteLoader;
import core.component.ui.UIElement;
import core.component.ui.color.UIColor;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UIText extends UIElement {
    public String text = "Text";
    public int fontSize = 32;
    public String fontName = "Arial";

    public String fontAssetPath = "";

    public UIColor color = UIColor.white();

    private transient Sprite cachedSprite;
    private transient boolean dirty = true;

    private transient String lastText = "";
    private transient int lastFontSize = -1;
    private transient String lastFontName = "";
    private transient String lastFontAssetPath = "";
    private transient float lastR = -1;
    private transient float lastG = -1;
    private transient float lastB = -1;
    private transient float lastA = -1;

    public String getText() {
        return text;
    }

    public UIText setText(String text) {
        this.text = text == null ? "" : text;
        this.dirty = true;
        return this;
    }

    public int getFontSize() {
        return fontSize;
    }

    public UIText setFontSize(int fontSize) {
        this.fontSize = Math.max(1, fontSize);
        this.dirty = true;
        return this;
    }

    public String getFontName() {
        return fontName;
    }

    public UIText setFontName(String fontName) {
        this.fontName = fontName == null ? "Arial" : fontName;
        this.dirty = true;
        return this;
    }

    public String getFontAssetPath() {
        return fontAssetPath;
    }

    public UIText setFontAssetPath(String fontAssetPath) {
        this.fontAssetPath = fontAssetPath == null ? "" : fontAssetPath;
        this.dirty = true;
        return this;
    }

    public UIColor getColor() {
        return color;
    }

    public UIText setColor(UIColor color) {
        this.color = color == null ? UIColor.white() : color;
        this.dirty = true;
        return this;
    }

    public Sprite getSprite() {
        if (color == null) {
            color = UIColor.white();
        }

        boolean changed =
                !safeEquals(text, lastText)
                        || fontSize != lastFontSize
                        || !safeEquals(fontName, lastFontName)
                        || !safeEquals(fontAssetPath, lastFontAssetPath)
                        || color.r != lastR
                        || color.g != lastG
                        || color.b != lastB
                        || color.a != lastA;

        if (dirty || changed || cachedSprite == null) {
            cachedSprite = buildTextSprite();
            dirty = false;

            lastText = text;
            lastFontSize = fontSize;
            lastFontName = fontName;
            lastFontAssetPath = fontAssetPath;
            lastR = color.r;
            lastG = color.g;
            lastB = color.b;
            lastA = color.a;
        }

        return cachedSprite;
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        return a.equals(b);
    }

    private Sprite buildTextSprite() {
        try {
            String safeText = text == null ? "" : text;
            int safeFontSize = Math.max(1, fontSize);

            Font font = loadFont(safeFontSize);

            BufferedImage measuringImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D measuringGraphics = measuringImage.createGraphics();
            measuringGraphics.setFont(font);
            FontMetrics metrics = measuringGraphics.getFontMetrics();

            int textWidth = Math.max(1, metrics.stringWidth(safeText));
            int textHeight = Math.max(1, metrics.getHeight());

            measuringGraphics.dispose();

            BufferedImage image = new BufferedImage(textWidth, textHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();

            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setFont(font);
            graphics.setColor(new Color(color.r, color.g, color.b, color.a));
            graphics.drawString(safeText, 0, metrics.getAscent());
            graphics.dispose();

            Path tempFile = Files.createTempFile("lyvex-ui-text-", ".png");
            ImageIO.write(image, "png", tempFile.toFile());

            Sprite sprite = SpriteLoader.loadFromFile(tempFile);
            Files.deleteIfExists(tempFile);

            width = textWidth;
            height = textHeight;

            return sprite;
        } catch (IOException e) {
            throw new RuntimeException("Failed to build UI text texture", e);
        }
    }

    private Font loadFont(int size) {
        if (fontAssetPath != null && !fontAssetPath.isBlank()) {
            try {
                Path fontPath = AssetManager.getAssetPath().resolve(fontAssetPath).normalize();

                if (Files.exists(fontPath)) {
                    Font loadedFont = Font.createFont(Font.TRUETYPE_FONT, fontPath.toFile());
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(loadedFont);
                    return loadedFont.deriveFont(Font.PLAIN, (float) size);
                }

                System.err.println("[UIText] Font not found in Assets: " + fontPath);
            } catch (IOException | FontFormatException e) {
                System.err.println("[UIText] Failed to load font asset: " + fontAssetPath);
                e.printStackTrace();
            }
        }

        String safeFontName = fontName == null || fontName.isBlank() ? "Arial" : fontName;
        return new Font(safeFontName, Font.PLAIN, size);
    }
}