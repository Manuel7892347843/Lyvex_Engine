package core.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class AudioManager {

    private static long device;
    private static long context;

    private static float masterVolume = 1.0f;
    private static boolean muted = false;

    private AudioManager() {
    }

    public static void init() {
        device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL) {
            throw new IllegalStateException("Failed to open OpenAL device");
        }

        ALCCapabilities deviceCapabilities = ALC.createCapabilities(device);

        context = alcCreateContext(device, (IntBuffer) null);
        if (context == NULL) {
            throw new IllegalStateException("Failed to create OpenAL context");
        }

        alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCapabilities);

        alListener3f(AL_POSITION, 0.0f, 0.0f, 0.0f);
        alListener3f(AL_VELOCITY, 0.0f, 0.0f, 0.0f);

        applyMasterVolume();
    }

    public static void setMasterVolume(float volume) {
        masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
        applyMasterVolume();
    }

    public static float getMasterVolume() {
        return masterVolume;
    }

    public static void setMuted(boolean value) {
        muted = value;
        applyMasterVolume();
    }

    public static boolean isMuted() {
        return muted;
    }

    private static void applyMasterVolume() {
        alListenerf(AL_GAIN, muted ? 0.0f : masterVolume);
    }

    public static void cleanup() {
        if (context != NULL) {
            alcDestroyContext(context);
            context = NULL;
        }

        if (device != NULL) {
            alcCloseDevice(device);
            device = NULL;
        }
    }
}