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
        if (parentDirectory == null) {
            throw new IllegalArgumentException("Parent directory is null");
        }

        if (!Files.exists(parentDirectory) || !Files.isDirectory(parentDirectory)) {
            throw new IllegalArgumentException("Invalid parent directory: " + parentDirectory);
        }

        String safeProjectName = sanitizeProjectName(projectName);
        Path newProjectRoot = parentDirectory.resolve(safeProjectName).normalize();

        try {
            Files.createDirectories(newProjectRoot);
            Files.createDirectories(newProjectRoot.resolve("Assets"));
            Files.createDirectories(newProjectRoot.resolve("Assets").resolve("Scripts"));
            Files.createDirectories(newProjectRoot.resolve("Assets").resolve("Scenes"));
            Files.createDirectories(newProjectRoot.resolve("Compiled"));
            Files.createDirectories(newProjectRoot.resolve("ProjectSettings"));

            projectRoot = newProjectRoot;

            Path projectFile = getProjectFilePath();
            if (!Files.exists(projectFile) || Files.size(projectFile) == 0) {
                ProjectSettings.save();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create project at: " + newProjectRoot, e);
        }
    }

    private static String sanitizeProjectName(String projectName) {
        if (projectName == null || projectName.isBlank()) {
            throw new IllegalArgumentException("Project name is empty");
        }

        String safeName = projectName.trim().replaceAll("[\\\\/:*?\"<>|]", "_");

        if (safeName.isBlank()) {
            throw new IllegalArgumentException("Project name is invalid");
        }

        return safeName;
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

    public static Path getScenesPath() {
        return getAssetsPath().resolve("Scenes");
    }

    public static Path getProjectSettingsPath() {
        return getProjectRoot().resolve("ProjectSettings");
    }
    public static Path getCompiledPath(){ return getProjectRoot().resolve("Compiled");}

    public static Path getProjectFilePath() {
        String projectName = getProjectRoot().getFileName().toString();
        return getProjectRoot().resolve(projectName + ".lyvex");
    }
}