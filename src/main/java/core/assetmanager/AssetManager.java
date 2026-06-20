package core.assetmanager;

import core.ProjectManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AssetManager {

    public static void createFolder(Path targetDirectory, String folderName) {
        Path folderPath = targetDirectory.resolve(folderName);

        try {
            if (!Files.exists(folderPath)) {
                Files.createDirectory(folderPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createNewComponent(Path targetDirectory, String componentName) {
        String className = capitalizeFirstLetter(componentName);
        Path componentPath = targetDirectory.resolve(className + ".java");

        String packageName = buildPackageFromScriptsPath(targetDirectory);

        String content =
                (packageName.isBlank() ? "" : "package " + packageName + ";\n\n") +
                        "import core.component.Component;\n" +
                        "\n" +
                        "public class " + className + " extends Component {\n" +
                        "    public " + className + "() {\n" +
                        "\n" +
                        "    }\n\n" +
                        "    @Override\n" +
                        "    public void start() {\n" +
                        "\n" +
                        "    }\n\n" +
                        "    @Override\n" +
                        "    public void update() {\n" +
                        "\n" +
                        "    }\n" +
                        "}\n";

        try {
            if (!Files.exists(componentPath)) {
                Files.writeString(componentPath, content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createNewScene(Path targetDirectory, String sceneName){
        String fileName = capitalizeFirstLetter(sceneName) + ".lyvexscene";
        Path scenePath = targetDirectory.resolve(fileName);

        try {
            if (!Files.exists(scenePath)) {
                Files.createFile(scenePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String buildPackageFromScriptsPath(Path targetDirectory) {
        Path scriptsRoot = ProjectManager.getScriptsPath();
        Path relative = scriptsRoot.relativize(targetDirectory);

        String packageName = relative.toString()
                .replace('\\', '.')
                .replace('/', '.')
                .trim();

        if (packageName.isBlank() || packageName.equals(".")) {
            return "";
        }

        return packageName;
    }

    public static Path getAssetPath() {
        Path assetsPath = ProjectManager.getAssetsPath();

        try {
            if (!Files.exists(assetsPath)) {
                Files.createDirectories(assetsPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return assetsPath;
    }

    public static Path getScriptsPath() {
        Path scriptsPath = ProjectManager.getScriptsPath();

        try {
            if (!Files.exists(scriptsPath)) {
                Files.createDirectories(scriptsPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return scriptsPath;
    }

    public static Path getScenePath(){
        Path scenePath = getAssetPath().resolve("Scenes");

        try{
            if(!Files.exists(scenePath)){
                Files.createDirectories(scenePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return scenePath;
    }

    private static String capitalizeFirstLetter(String text) {
        if (text == null || text.isBlank()) {
            return "NewComponent";
        }

        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    public static void exportAllAssetsToZip(Path destinationDirectory, String zipFileName) throws IOException {
        Path assetsRoot = getAssetPath();
        exportPathsToZip(List.of(assetsRoot), assetsRoot, destinationDirectory, zipFileName);
    }

    public static void exportPathsToZip(List<Path> pathsToExport, Path assetsRoot, Path destinationDirectory, String zipFileName) throws IOException {
        if (pathsToExport == null || pathsToExport.isEmpty()) {
            throw new IllegalArgumentException("No assets selected for export.");
        }

        if (assetsRoot == null || !Files.exists(assetsRoot) || !Files.isDirectory(assetsRoot)) {
            throw new IllegalArgumentException("Invalid Assets folder.");
        }

        if (destinationDirectory == null) {
            throw new IllegalArgumentException("Invalid destination directory.");
        }

        Files.createDirectories(destinationDirectory);

        String safeZipFileName = sanitizeZipFileName(zipFileName);
        Path zipPath = destinationDirectory.resolve(safeZipFileName);

        try (OutputStream outputStream = Files.newOutputStream(zipPath);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

            for (Path path : pathsToExport) {
                if (path == null || !Files.exists(path)) {
                    continue;
                }

                Path normalizedPath = path.toAbsolutePath().normalize();
                Path normalizedAssetsRoot = assetsRoot.toAbsolutePath().normalize();

                if (!normalizedPath.startsWith(normalizedAssetsRoot)) {
                    continue;
                }

                if (Files.isDirectory(normalizedPath)) {
                    zipDirectory(normalizedPath, normalizedAssetsRoot, zipOutputStream);
                } else {
                    zipFile(normalizedPath, normalizedAssetsRoot, zipOutputStream);
                }
            }
        }
    }

    private static void zipDirectory(Path directory, Path assetsRoot, ZipOutputStream zipOutputStream) throws IOException {
        try (var paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            zipFile(path, assetsRoot, zipOutputStream);
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

    private static void zipFile(Path file, Path assetsRoot, ZipOutputStream zipOutputStream) throws IOException {
        Path relativePath = assetsRoot.relativize(file);
        String zipEntryName = relativePath.toString().replace('\\', '/');

        ZipEntry zipEntry = new ZipEntry(zipEntryName);
        zipOutputStream.putNextEntry(zipEntry);
        Files.copy(file, zipOutputStream);
        zipOutputStream.closeEntry();
    }

    private static String sanitizeZipFileName(String zipFileName) {
        if (zipFileName == null || zipFileName.isBlank()) {
            return "Assets.zip";
        }

        String safeName = zipFileName.trim()
                .replace("\\", "_")
                .replace("/", "_")
                .replace(":", "_")
                .replace("*", "_")
                .replace("?", "_")
                .replace("\"", "_")
                .replace("<", "_")
                .replace(">", "_")
                .replace("|", "_");

        if (!safeName.toLowerCase().endsWith(".zip")) {
            safeName += ".zip";
        }

        return safeName;
    }
}