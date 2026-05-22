package core.lib;

import core.log.Log;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Random {
    public static int range(int minInclusive, int maxInclusive) {
        if (minInclusive > maxInclusive) {
            Log.logError("min must be <= max");
        }
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    public static float range(float minInclusive, float maxInclusive) {
        if (minInclusive > maxInclusive) {
            Log.logError("min must be <= max");
            return 0f;
        }
        return minInclusive + ThreadLocalRandom.current().nextFloat() * (maxInclusive - minInclusive);
    }

    public static boolean chance(float probability) {
        if (probability <= 0f) return false;
        if (probability >= 1f) return true;
        return ThreadLocalRandom.current().nextFloat() < probability;
    }

    public static <T> T pick(T[] items) {
        if (items == null || items.length == 0) {
            Log.logError("Array must not be null or empty");
            return null;
        }
        return items[ThreadLocalRandom.current().nextInt(items.length)];
    }

    public static <T> T pick(List<T> items) {
        if (items == null || items.isEmpty()) {
            Log.logError("List must not be null or empty");
            return null;
        }
        return items.get(ThreadLocalRandom.current().nextInt(items.size()));
    }

    public static <T> void shuffle(List<T> list) {
        java.util.Collections.shuffle(list, ThreadLocalRandom.current());
    }

    public static float gaussian(float mean, float stdDev) {
        return mean + (float) ThreadLocalRandom.current().nextGaussian() * stdDev;
    }
}
