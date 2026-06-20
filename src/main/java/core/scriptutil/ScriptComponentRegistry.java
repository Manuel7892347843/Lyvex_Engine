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
import core.physics.BoxCollider2D;
import core.physics.CircleCollider2D;
import core.physics.Collider2D;
//import core.physics.RaycastHit2D;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

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

        //COLLIDER
        COMPONENT_CLASSES.add(Collider2D.class);
        COMPONENT_CLASSES.add(BoxCollider2D.class);
        COMPONENT_CLASSES.add(CircleCollider2D.class);
        //COMPONENT_CLASSES.add(RaycastHit2D.class); TODO: da implementare, solo forma base attualmente

        //COLLIDER DEBUG
        COMPONENT_CLASSES.add(PhysicsWorld2DDebug.class);

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

        try {
            Files.createDirectories(compiledPath);
        } catch (IOException e) {
            Log.logError("Failed to create compiled scripts folder.", e);
            return;
        }

        try (Stream<Path> paths = Files.walk(scriptsRoot)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> sourceFiles.add(path.toString()));
        } catch (IOException e) {
            Log.logError("Failed to scan script sources.", e);
            return;
        }

        if (sourceFiles.isEmpty()) {
            return;
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            Log.logError("No Java compiler available. Run Lyvex with a JDK, not a JRE.");
            return;
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnostics,
                Locale.getDefault(),
                StandardCharsets.UTF_8
        )) {
            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromStrings(sourceFiles);

            String classpath = ProjectManager.getCompiledPath().toString()
                    + File.pathSeparator
                    + System.getProperty("java.class.path");

            List<String> options = new ArrayList<>();
            options.add("-classpath");
            options.add(classpath);
            options.add("-d");
            options.add(compiledPath.toString());

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    options,
                    null,
                    compilationUnits
            );

            boolean success = Boolean.TRUE.equals(task.call());

            if (!success) {
                Log.logError(formatScriptDiagnostics(diagnostics));
                return;
            }

            Log.logSuccess("Scripts compiled successfully.");

        } catch (IOException e) {
            Log.logError("Failed during script compilation.", e);
        }
    }

    private static String formatScriptDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder builder = new StringBuilder();
        builder.append("Script compilation failed:");

        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            builder.append("\n\n");

            Diagnostic.Kind kind = diagnostic.getKind();
            builder.append(kind);

            JavaFileObject source = diagnostic.getSource();
            if (source != null) {
                builder.append(" in ")
                        .append(Path.of(source.toUri()).getFileName());
            }

            if (diagnostic.getLineNumber() >= 0) {
                builder.append(" at line ")
                        .append(diagnostic.getLineNumber());
            }

            if (diagnostic.getColumnNumber() >= 0) {
                builder.append(", column ")
                        .append(diagnostic.getColumnNumber());
            }

            builder.append("\n")
                    .append(diagnostic.getMessage(Locale.getDefault()));
        }

        return builder.toString();
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
            Log.logError("Failed to load compiled components.", e);
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
        } catch (ClassNotFoundException e) {
            Log.logError("Could not load script class: " + className, e);
        } catch (Throwable throwable) {
            Log.logError("Script class failed while loading: " + className, throwable);
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