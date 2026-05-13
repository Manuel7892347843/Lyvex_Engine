package core.scriptutil;

import core.ProjectManager;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;

import static java.nio.file.StandardWatchEventKinds.*;

public class ScriptAutoRefreshWatcher {
    private WatchService watchService;
    private Thread watcherThread;
    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> pendingRefresh;
    private volatile boolean running = false;

    public void start() {
        if (running) {
            return;
        }

        Path scriptsRoot = ProjectManager.getScriptsPath();
        if (scriptsRoot == null || !Files.exists(scriptsRoot)) {
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            registerAllFolders(scriptsRoot);

            running = true;
            watcherThread = new Thread(this::watchLoop, "ScriptAutoRefreshWatcher");
            watcherThread.setDaemon(true);
            watcherThread.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start script watcher", e);
        }
    }

    public void stop() {
        running = false;

        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException ignored) {
        }

        if (watcherThread != null) {
            watcherThread.interrupt();
        }

        debounceExecutor.shutdownNow();
    }

    private void watchLoop() {
        while (running) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }

            boolean refreshNeeded = false;

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                refreshNeeded = true;
            }

            if (refreshNeeded) {
                scheduleRefresh();
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    private void scheduleRefresh() {
        if (pendingRefresh != null && !pendingRefresh.isDone()) {
            pendingRefresh.cancel(false);
        }

        pendingRefresh = debounceExecutor.schedule(() -> {
            try {
                ScriptComponentRegistry.refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    private void registerAllFolders(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }

        Files.walk(root)
                .filter(Files::isDirectory)
                .forEach(this::registerFolderSafely);
    }

    private void registerFolderSafely(Path folder) {
        try {
            folder.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        } catch (IOException e) {
            throw new RuntimeException("Failed to register folder for watching: " + folder, e);
        }
    }
}