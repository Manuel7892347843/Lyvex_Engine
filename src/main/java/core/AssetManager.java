package core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
                        "import core.GameObject;\n" +
                        "public class " + className + " extends Component {\n" +
                        "    public " + className + "() {\n" +
                        "    }\n\n" +
                        "    @Override\n" +
                        "    public void start() {\n" +
                        "    }\n\n" +
                        "    @Override\n" +
                        "    public void update() {\n" +
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

    private static String capitalizeFirstLetter(String text) {
        if (text == null || text.isBlank()) {
            return "NewComponent";
        }

        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}