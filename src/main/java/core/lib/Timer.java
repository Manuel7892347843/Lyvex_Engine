package core.lib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Timer {

    private static final List<TimerTask> tasks = new ArrayList<>();

    private Timer() {}

    public static TimerTask after(float seconds, Runnable action) {
        TimerTask task = new TimerTask(seconds, false, action);
        tasks.add(task);
        return task;
    }

    public static TimerTask every(float seconds, Runnable action) {
        TimerTask task = new TimerTask(seconds, true, action);
        tasks.add(task);
        return task;
    }

    public static void update() {
        float dt = Time.deltaTime();

        Iterator<TimerTask> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            TimerTask task = iterator.next();

            if (task.cancelled) {
                iterator.remove();
                continue;
            }

            task.elapsed += dt;

            if (task.elapsed >= task.duration) {
                if (task.action != null) {
                    task.action.run();
                }

                if (task.repeating) {
                    task.elapsed = 0f;
                } else {
                    iterator.remove();
                }
            }
        }
    }

    public static void cancel(TimerTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    public static void cancelAll() {
        tasks.clear();
    }

    public static int count() {
        return tasks.size();
    }

    public static final class TimerTask {
        private final float duration;
        private final boolean repeating;
        private final Runnable action;

        private float elapsed;
        private boolean cancelled;

        private TimerTask(float duration, boolean repeating, Runnable action) {
            this.duration = Math.max(0f, duration);
            this.repeating = repeating;
            this.action = action;
        }

        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public boolean isRepeating() {
            return repeating;
        }

        public float getDuration() {
            return duration;
        }

        public float getElapsed() {
            return elapsed;
        }

        public float getRemaining() {
            return Math.max(0f, duration - elapsed);
        }

        public float getProgress() {
            if (duration <= 0f) {
                return 1f;
            }

            return Mathf.clamp(elapsed / duration, 0f, 1f);
        }
    }
}