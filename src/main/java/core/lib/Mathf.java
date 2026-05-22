package core.lib;

public final class Mathf {

    public static final float PI = 3.141592653589793f;
    public static final float EPSILON = 1e-6f;

    private Mathf() {}

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp(t, 0f, 1f);
    }

    public static float lerpUnclamped(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float inverseLerp(float a, float b, float value) {
        if (a != b) {
            return clamp((value - a) / (b - a), 0f, 1f);
        }
        return 0f;
    }

    public static float remap(float value, float inMin, float inMax, float outMin, float outMax) {
        float t = inverseLerp(inMin, inMax, value);
        return lerp(outMin, outMax, t);
    }

    public static float moveTowards(float current, float target, float maxDelta) {
        if (Math.abs(target - current) <= maxDelta) {
            return target;
        }
        return current + Math.signum(target - current) * maxDelta;
    }

    public static float smoothStep(float edge0, float edge1, float x) {
        float t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public static float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public static float distanceSquared(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    public static float angleDeg(float x1, float y1, float x2, float y2) {
        return radToDeg((float) Math.atan2(y2 - y1, x2 - x1));
    }

    public static float degToRad(float degrees) {
        return degrees * (PI / 180f);
    }

    public static float radToDeg(float radians) {
        return radians * (180f / PI);
    }

    public static boolean approximately(float a, float b) {
        return approximately(a, b, EPSILON);
    }

    public static boolean approximately(float a, float b, float epsilon) {
        return Math.abs(a - b) < epsilon;
    }

    public static float pingPong(float t, float length) {
        t = repeat(t, length * 2f);
        return length - Math.abs(t - length);
    }

    public static float repeat(float t, float length) {
        return clamp(t - (float) Math.floor(t / length) * length, 0f, length);
    }

    public static float sign(float f) {
        return f >= 0f ? 1f : -1f;
    }

    public static float pow(float f, float p) {
        return (float) Math.pow(f, p);
    }

    public static float sqrt(float f) {
        return (float) Math.sqrt(f);
    }

    public static float abs(float f) {
        return Math.abs(f);
    }

    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    public static float sin(float f) {
        return (float) Math.sin(f);
    }

    public static float cos(float f) {
        return (float) Math.cos(f);
    }

    public static float tan(float f) {
        return (float) Math.tan(f);
    }

    public static float asin(float f) {
        return (float) Math.asin(f);
    }

    public static float acos(float f) {
        return (float) Math.acos(f);
    }

    public static float atan(float f) {
        return (float) Math.atan(f);
    }

    public static float atan2(float y, float x) {
        return (float) Math.atan2(y, x);
    }
}