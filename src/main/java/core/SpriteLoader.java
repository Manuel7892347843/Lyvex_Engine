package core;

import core.component.Sprite;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class SpriteLoader {

    public static Sprite loadFromFile(Path path) {
        if (path == null || !Files.exists(path)) {
            throw new IllegalArgumentException("Invalid sprite path: " + path);
        }

        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                throw new IllegalStateException("Failed to load image: " + path);
            }

            int width = image.getWidth();
            int height = image.getHeight();

            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

            for (int y = height - 1; y >= 0; y--) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];

                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                    buffer.put((byte) (pixel & 0xFF));
                    buffer.put((byte) ((pixel >> 24) & 0xFF));
                }
            }

            buffer.flip();

            int textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glTexImage2D(
                    GL_TEXTURE_2D,
                    0,
                    GL_RGBA,
                    width,
                    height,
                    0,
                    GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    buffer
            );

            glBindTexture(GL_TEXTURE_2D, 0);

            return new Sprite(textureId, width, height);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image: " + path, e);
        }
    }
}