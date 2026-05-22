package core.scriptutil;

import core.ProjectManager;
import core.component.*;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import core.component.sprite.SpriteComponent;
import core.component.tilemap.Tilemap;
import core.component.ui.Canvas;
import core.component.ui.uiElements.UIButton;
import core.component.ui.uiElements.UIImage;
import core.component.ui.uiElements.UIPanel;
import core.component.ui.uiElements.UIText;
import core.log.Log;

public class ScriptComponentRegistry {
    private static final List<Class<? extends Component>> COMPONENT_CLASSES = new ArrayList<>();

    public static void refresh() {
        COMPONENT_CLASSES.clear();
        COMPONENT_CLASSES.add(Camera.class);
        COMPONENT_CLASSES.add(Transform.class);
        COMPONENT_CLASSES.add(SpriteComponent.class);
        COMPONENT_CLASSES.add(RigidBody2D.class);
        COMPONENT_CLASSES.add(ParentFollower.class);
        COMPONENT_CLASSES.add(Tilemap.class);
        COMPONENT_CLASSES.add(AudioSource.class);
        COMPONENT_CLASSES.add(AudioDistanceAttenuation.class);

        //UI
        COMPONENT_CLASSES.add(Canvas.class);
        COMPONENT_CLASSES.add(UIPanel.class);
        COMPONENT_CLASSES.add(UIText.class);
        COMPONENT_CLASSES.add(UIImage.class);
        COMPONENT_CLASSES.add(UIButton.class);

        Path scriptsRoot = ProjectManager.getScriptsPath();
        if (scriptsRoot == null || !Files.exists(scriptsRoot)) {
            return;
        }

        compileScripts(scriptsRoot);
        loadCompiledComponents(scriptsRoot);
    }

    private static void compileScripts(Path scriptsRoot) {
        List<String> sourceFiles = new ArrayList<>();

        Path compiledPath = ProjectManager.getCompiledPath();

        try (Stream<Path> paths = Files.walk(scriptsRoot)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> sourceFiles.add(path.toString()));
        } catch (IOException e) {
            Log.logError("Failed to scan script sources: \n" + e);
        }

        if (sourceFiles.isEmpty()) {
            return;
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            Log.logError("No Java compiler available. Run with a JDK, not a JRE.");
        }

        String classpath = ProjectManager.getCompiledPath().toString() + File.pathSeparator + System.getProperty("java.class.path");

        List<String> args = new ArrayList<>();
        args.add("-classpath");
        args.add(classpath);
        args.add("-d");
        args.add(compiledPath.toString());
        args.addAll(sourceFiles);

        int result = compiler.run(null, null, null, args.toArray(new String[0]));
        if (result != 0) {
            Log.logError("Script compilation failed with exit code: \n" + result);
        }
    }

    private static void loadCompiledComponents(Path scriptsRoot) {
        Path compiledPath = ProjectManager.getCompiledPath();

        try (URLClassLoader classLoader = new URLClassLoader(
                new URL[]{compiledPath.toUri().toURL()},
                ScriptComponentRegistry.class.getClassLoader()
        )) {
            try (Stream<Path> paths = Files.walk(scriptsRoot)) {
                paths.filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> tryRegisterScriptClass(path, scriptsRoot, classLoader));
            }
        } catch (IOException e) {
            Log.logError("Failed to load compiled components: \n" + e);
        }
    }

    private static void tryRegisterScriptClass(Path javaFile, Path scriptsRoot, ClassLoader classLoader) {
        String className = toClassName(javaFile, scriptsRoot);
        if (className == null) {
            return;
        }

        try {
            Class<?> rawClass = Class.forName(className, true, classLoader);
            if (Component.class.isAssignableFrom(rawClass) && rawClass != Component.class) {
                @SuppressWarnings("unchecked")
                Class<? extends Component> componentClass = (Class<? extends Component>) rawClass;
                COMPONENT_CLASSES.add(componentClass);
            }
        } catch (ClassNotFoundException ignored) {
            Log.logError("Could not load script class: \n" + className);
        }
    }

    public static Class<? extends Component> findComponentClass(String fullName) {
        for (Class<? extends Component> clazz : COMPONENT_CLASSES) {
            if (clazz.getName().equals(fullName)) {
                return clazz;
            }
        }

        for (Class<? extends Component> clazz : COMPONENT_CLASSES) {
            if (clazz.getSimpleName().equals(fullName)) {
                return clazz;
            }
        }

        return null;
    }

    private static String toClassName(Path javaFile, Path scriptsRoot) {
        Path relative = scriptsRoot.relativize(javaFile);
        String path = relative.toString();

        if (!path.endsWith(".java")) {
            return null;
        }

        String withoutExtension = path.substring(0, path.length() - 5);
        return withoutExtension.replace('\\', '.').replace('/', '.');
    }

    public static List<Class<? extends Component>> getComponentClasses() {
        return Collections.unmodifiableList(COMPONENT_CLASSES);
    }
}