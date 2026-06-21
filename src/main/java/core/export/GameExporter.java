package core.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.Engine;
import core.ProjectManager;
import core.ProjectSettings;
import core.lib.SceneManager;
import core.log.Log;
import core.scriptutil.ScriptComponentRegistry;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GameExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private GameExporter() {
    }

    public static void exportCurrentProject(GameExportSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Export settings cannot be null.");
        }

        if (settings.destinationDirectory == null) {
            throw new IllegalArgumentException("Destination directory cannot be null.");
        }

        String safeGameName = sanitizeFileName(settings.gameName);
        Path buildRoot = settings.destinationDirectory.resolve(safeGameName);

        try {
            Engine.saveCurrentScenePublic();
            ProjectSettings.save();
            ScriptComponentRegistry.refresh();

            normalizeAndValidateSettings(settings);

            recreateDirectory(buildRoot);

            copyDirectory(ProjectManager.getAssetsPath(), buildRoot.resolve("Assets"));
            copyDirectory(ProjectManager.getProjectSettingsPath(), buildRoot.resolve("ProjectSettings"));

            Path compiledPath = ProjectManager.getCompiledPath();
            if (Files.exists(compiledPath)) {
                copyDirectory(compiledPath, buildRoot.resolve("Compiled"));
            } else {
                Files.createDirectories(buildRoot.resolve("Compiled"));
            }

            writeExportedProjectFile(buildRoot, safeGameName, settings);

            copyRuntimeJar(buildRoot);
            createWindowsLauncher(buildRoot, safeGameName);

            Log.logSuccess("Game exported successfully to: " + buildRoot);
        } catch (IOException e) {
            Log.logError("Failed to export game.", e);
            throw new RuntimeException("Failed to export game.", e);
        }
    }

    private static void normalizeAndValidateSettings(GameExportSettings settings) {
        if (settings.gameName == null || settings.gameName.isBlank()) {
            throw new IllegalArgumentException("Game name cannot be empty.");
        }

        if (settings.windowWidth <= 0 || settings.windowHeight <= 0) {
            throw new IllegalArgumentException("Invalid window size.");
        }

        List<GameExportSettings.SceneBuildEntry> validScenes = new ArrayList<>();

        for (GameExportSettings.SceneBuildEntry scene : settings.scenes) {
            if (scene == null || !scene.inBuild) {
                continue;
            }

            if (scene.fileName == null || scene.fileName.isBlank()) {
                continue;
            }

            Path scenePath = ProjectManager.getScenesPath().resolve(scene.fileName);

            if (!Files.exists(scenePath)) {
                Log.logWarning("Scene skipped because file does not exist: " + scene.fileName);
                continue;
            }

            validScenes.add(scene);
        }

        if (validScenes.isEmpty()) {
            throw new IllegalArgumentException("No valid scenes selected for export. Add at least one existing scene to build.");
        }

        boolean startupExists = false;

        if (settings.startupScene != null && !settings.startupScene.isBlank()) {
            Path startupPath = ProjectManager.getScenesPath().resolve(settings.startupScene);

            if (Files.exists(startupPath)) {
                for (GameExportSettings.SceneBuildEntry scene : validScenes) {
                    if (scene.fileName.equals(settings.startupScene)) {
                        startupExists = true;
                        break;
                    }
                }
            }
        }

        if (!startupExists) {
            settings.startupScene = validScenes.get(0).fileName;
            Log.logWarning("Startup scene was invalid or missing. Using: " + settings.startupScene);
        }

        settings.scenes.clear();
        settings.scenes.addAll(validScenes);
    }

    private static void writeExportedProjectFile(Path buildRoot, String safeGameName, GameExportSettings settings) throws IOException {
        ProjectSettings.ProjectData data = readCurrentProjectData();

        data.projectName = safeGameName;
        data.gameName = settings.gameName;
        data.startupScene = settings.startupScene;
        data.windowWidth = settings.windowWidth;
        data.windowHeight = settings.windowHeight;
        data.fullscreen = settings.fullscreen;
        data.vsync = settings.vsync;

        List<SceneManager.SceneEntry> exportedScenes = new ArrayList<>();

        for (GameExportSettings.SceneBuildEntry scene : settings.scenes) {
            Path scenePath = ProjectManager.getScenesPath().resolve(scene.fileName);

            if (!Files.exists(scenePath)) {
                continue;
            }

            SceneManager.SceneEntry entry = new SceneManager.SceneEntry(scene.name, scene.fileName);
            entry.inBuild = true;
            exportedScenes.add(entry);
        }

        data.scenes = exportedScenes;

        Path exportedProjectFile = buildRoot.resolve(safeGameName + ".lyvex");
        Files.writeString(exportedProjectFile, GSON.toJson(data));
    }

    private static ProjectSettings.ProjectData readCurrentProjectData() throws IOException {
        Path projectFile = ProjectManager.getProjectFilePath();

        if (!Files.exists(projectFile)) {
            ProjectSettings.ProjectData fallback = new ProjectSettings.ProjectData();
            fallback.projectName = ProjectManager.getProjectRoot().getFileName().toString();
            fallback.scenes = ProjectSettings.getSceneManager().getSceneEntries();
            return fallback;
        }

        String json = Files.readString(projectFile);
        ProjectSettings.ProjectData data = GSON.fromJson(json, ProjectSettings.ProjectData.class);

        if (data == null) {
            data = new ProjectSettings.ProjectData();
        }

        if (data.scenes == null) {
            data.scenes = ProjectSettings.getSceneManager().getSceneEntries();
        }

        return data;
    }

    private static void copyRuntimeJar(Path buildRoot) throws IOException {
        Path runtimeJar = findCurrentApplicationJar();

        if (runtimeJar == null || !Files.exists(runtimeJar)) {
            throw new IOException("Runtime jar not found. Build the engine with shadowJar before exporting.");
        }

        Files.copy(
                runtimeJar,
                buildRoot.resolve("LyvexRuntime.jar"),
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    private static Path findCurrentApplicationJar() {
        try {
            Path codeSource = Path.of(GameExporter.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            if (Files.isRegularFile(codeSource) && codeSource.toString().endsWith(".jar")) {
                return codeSource;
            }

            Path shadowJar = Path.of(System.getProperty("user.dir"))
                    .resolve("build")
                    .resolve("libs")
                    .resolve("LyvexEngine-1.0-all.jar");

            if (Files.exists(shadowJar)) {
                return shadowJar;
            }

            Path normalJar = Path.of(System.getProperty("user.dir"))
                    .resolve("build")
                    .resolve("libs")
                    .resolve("LyvexEngine-1.0.jar");

            if (Files.exists(normalJar)) {
                return normalJar;
            }

            return null;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to locate runtime jar.", e);
        }
    }

    private static void createWindowsLauncher(Path buildRoot, String gameName) throws IOException {
        String content = """
                @echo off
                setlocal

                cd /d "%~dp0"

                java -jar LyvexRuntime.jar --runtime "%CD%"

                if errorlevel 1 (
                    echo.
                    echo The game could not start.
                    echo.
                    java -version
                    pause
                    exit /b 1
                )

                endlocal
                """;

        Files.writeString(buildRoot.resolve(gameName + ".bat"), content);
    }

    private static void recreateDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IOException ioException) {
                    throw ioException;
                }

                throw e;
            }
        }

        Files.createDirectories(directory);
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }

        try (var paths = Files.walk(source)) {
            paths.forEach(sourcePath -> {
                try {
                    Path relativePath = source.relativize(sourcePath);
                    Path targetPath = target.resolve(relativePath);

                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }

            throw e;
        }
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "LyvexGame";
        }

        String safeName = fileName.trim()
                .replace("\\", "_")
                .replace("/", "_")
                .replace(":", "_")
                .replace("*", "_")
                .replace("?", "_")
                .replace("\"", "_")
                .replace("<", "_")
                .replace(">", "_")
                .replace("|", "_");

        if (safeName.isBlank()) {
            return "LyvexGame";
        }

        return safeName;
    }
}