package core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProjectManager {
    private static Path projectRoot;

    public static void openProject(Path root) {
        if (!isValidProject(root)) {
            throw new IllegalArgumentException("Invalid project directory: " + root);
        }

        projectRoot = root;
    }

    public static void createProject(Path parentDirectory, String projectName) {
        Path newProjectRoot = parentDirectory.resolve(projectName);

        try {
            Files.createDirectories(newProjectRoot);
            Files.createDirectories(newProjectRoot.resolve("Assets"));
            Files.createDirectories(newProjectRoot.resolve("Assets").resolve("Scripts"));
            Files.createDirectories(newProjectRoot.resolve("Assets").resolve("Scenes"));
            Files.createDirectories(newProjectRoot.resolve("ProjectSettings"));

            Path projectFile = newProjectRoot.resolve(projectName + ".lyvex");
            if (!Files.exists(projectFile)) {
                Files.createFile(projectFile);
            }

            projectRoot = newProjectRoot;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create project", e);
        }
    }

    public static boolean isValidProject(Path root) {
        return root != null
                && Files.exists(root)
                && Files.isDirectory(root)
                && Files.exists(root.resolve("Assets"))
                && Files.isDirectory(root.resolve("Assets"))
                && Files.exists(root.resolve("ProjectSettings"))
                && Files.isDirectory(root.resolve("ProjectSettings"));
    }

    public static Path getProjectRoot() {
        if (projectRoot == null) {
            throw new IllegalStateException("No project is currently open");
        }
        return projectRoot;
    }

    public static Path getAssetsPath() {
        return getProjectRoot().resolve("Assets");
    }

    public static Path getScriptsPath() {
        return getAssetsPath().resolve("Scripts");
    }
}