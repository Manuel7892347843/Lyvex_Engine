package core.lib;

import core.ProjectManager;
import core.audio.AudioClip;
import core.component.sprite.Sprite;
import core.component.sprite.SpriteLoader;
import core.log.Log;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Assets {

    private Assets() {}

    public static Path assetsPath() {
        return ProjectManager.getAssetsPath();
    }

    public static Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            Log.logError("Asset path cannot be null or blank");
            return assetsPath();
        }

        return assetsPath().resolve(normalize(relativePath));
    }

    public static boolean exists(String relativePath) {
        return Files.exists(resolve(relativePath));
    }

    public static boolean isFile(String relativePath) {
        return Files.isRegularFile(resolve(relativePath));
    }

    public static boolean isDirectory(String relativePath) {
        return Files.isDirectory(resolve(relativePath));
    }

    public static Sprite loadSprite(String relativePath) {
        Path path = resolve(relativePath);

        if (!Files.exists(path)) {
            Log.logError("Sprite asset not found: " + relativePath);
            return null;
        }

        return SpriteLoader.loadFromFile(path);
    }

    public static AudioClip loadAudio(String relativePath) {
        Path path = resolve(relativePath);

        if (!Files.exists(path)) {
            Log.logError("Audio asset not found: " + relativePath);
            return null;
        }

        return new AudioClip(path);
    }

    public static String getExtension(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "";
        }

        String fileName = Path.of(relativePath).getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    public static String getFileName(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "";
        }

        return Path.of(relativePath).getFileName().toString();
    }

    public static String getFileNameWithoutExtension(String relativePath) {
        String fileName = getFileName(relativePath);
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex < 0) {
            return fileName;
        }

        return fileName.substring(0, dotIndex);
    }

    private static String normalize(String path) {
        return path.replace('\\', '/');
    }
}