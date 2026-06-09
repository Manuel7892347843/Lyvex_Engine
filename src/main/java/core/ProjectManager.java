package core;

import java.io.IOException;
import java.net.URISyntaxException;
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
            Files.createDirectories(newProjectRoot.resolve("Libs"));

            createGradleFiles(newProjectRoot, safeProjectName);
            copyLyvexApiJar(newProjectRoot);

            projectRoot = newProjectRoot;

            Path projectFile = getProjectFilePath();
            if (!Files.exists(projectFile) || Files.size(projectFile) == 0) {
                ProjectSettings.save();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create project at: " + newProjectRoot, e);
        }
    }

    private static void copyLyvexApiJar(Path newProjectRoot) throws IOException {
        Path targetApiJar = newProjectRoot.resolve("Libs").resolve("lyvex-api.jar");
        Path sourceApiJar = findLyvexApiJar();

        if (sourceApiJar == null || !Files.exists(sourceApiJar)) {
            System.err.println("lyvex-api.jar not found. The project was created, but VS Code may show import errors.");
            System.err.println("Expected lyvex-api.jar near the engine executable or in build/libs.");
            return;
        }

        Files.copy(sourceApiJar, targetApiJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Copied lyvex-api.jar to: " + targetApiJar);
    }

    private static Path findLyvexApiJar() {
        try {
            Path codeSource = Path.of(ProjectManager.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            Path appDirectory = Files.isRegularFile(codeSource)
                    ? codeSource.getParent()
                    : codeSource;

            Path releaseApiJar = appDirectory.resolve("lyvex-api.jar");
            if (Files.exists(releaseApiJar)) {
                return releaseApiJar;
            }

            Path devApiJar = Path.of(System.getProperty("user.dir"))
                    .resolve("build")
                    .resolve("libs")
                    .resolve("lyvex-api.jar");

            if (Files.exists(devApiJar)) {
                return devApiJar;
            }

            Path projectRootApiJar = Path.of(System.getProperty("user.dir"))
                    .resolve("lyvex-api.jar");

            if (Files.exists(projectRootApiJar)) {
                return projectRootApiJar;
            }

            return null;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to locate lyvex-api.jar", e);
        }
    }

    private static void createGradleFiles(Path projectRoot, String projectName) throws IOException {
        Path settingsGradle = projectRoot.resolve("settings.gradle");
        Path buildGradle = projectRoot.resolve("build.gradle");

        String settingsContent = """
                pluginManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }

                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                    repositories {
                        mavenCentral()
                    }
                }

                rootProject.name = "%sScripts"
                """.formatted(projectName);

        String buildContent = """
                plugins {
                    id 'java'
                }

                java {
                    toolchain {
                        languageVersion = JavaLanguageVersion.of(21)
                    }
                }

                sourceSets {
                    main {
                        java {
                            srcDirs = ['Assets/Scripts']
                        }
                    }
                }

                dependencies {
                    implementation files('Libs/lyvex-api.jar')
                }
                """;

        if (!Files.exists(settingsGradle)) {
            Files.writeString(settingsGradle, settingsContent);
        }

        if (!Files.exists(buildGradle)) {
            Files.writeString(buildGradle, buildContent);
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