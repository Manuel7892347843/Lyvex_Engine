package core.component;

import core.assetmanager.AssetManager;
import core.audio.AudioClip;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.openal.AL10.*;

public class AudioSource extends Component {

    private transient int sourceId;
    private transient AudioClip clip;
    private transient float volumeMultiplier = 1.0f;

    public String audioPath = "";
    public boolean playOnAwake = false;
    public boolean loop = false;
    public float volume = 1.0f;
    public float pitch = 1.0f;

    public AudioSource() {
    }

    @Override
    public void awake() {
        System.out.println("[AudioSource] awake");
        System.out.println("[AudioSource] audioPath = " + audioPath);
        System.out.println("[AudioSource] playOnAwake = " + playOnAwake);
        System.out.println("[AudioSource] loop = " + loop);
        System.out.println("[AudioSource] volume = " + volume);
        System.out.println("[AudioSource] pitch = " + pitch);

        sourceId = alGenSources();
        checkOpenALError("alGenSources");

        applyGain();
        alSourcef(sourceId, AL_PITCH, pitch);
        alSource3f(sourceId, AL_POSITION, 0.0f, 0.0f, 0.0f);
        alSourcei(sourceId, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
        checkOpenALError("source setup");

        if (audioPath != null && !audioPath.isBlank()) {
            setAudioPath(audioPath);
        } else {
            System.err.println("[AudioSource] audioPath is empty.");
        }

        if (playOnAwake) {
            play();
        }
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;

        if (sourceId == 0) {
            System.out.println("[AudioSource] setAudioPath saved only, source is not initialized yet.");
            return;
        }

        if (clip != null) {
            clip.dispose();
            clip = null;
        }

        try {
            Path path = Path.of(audioPath);

            if (!path.isAbsolute()) {
                path = AssetManager.getAssetPath().resolve(audioPath);
            }

            path = path.normalize();

            System.out.println("[AudioSource] Loading audio:");
            System.out.println("[AudioSource]   raw path      = " + audioPath);
            System.out.println("[AudioSource]   resolved path = " + path);
            System.out.println("[AudioSource]   exists        = " + Files.exists(path));

            if (!Files.exists(path)) {
                System.err.println("[AudioSource] Audio file does not exist: " + path);
                return;
            }

            clip = new AudioClip(path);
            alSourcei(sourceId, AL_BUFFER, clip.getBufferId());
            checkOpenALError("alSourcei AL_BUFFER");

            System.out.println("[AudioSource] Audio clip loaded successfully.");
        } catch (Exception e) {
            System.err.println("[AudioSource] Failed to load audio clip: " + audioPath);
            e.printStackTrace();
        }
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void play() {
        System.out.println("[AudioSource] play requested.");

        if (sourceId == 0) {
            System.err.println("[AudioSource] Cannot play: sourceId is 0.");
            return;
        }

        if (clip == null) {
            System.err.println("[AudioSource] Cannot play: clip is null.");
            return;
        }

        alSourcePlay(sourceId);
        checkOpenALError("alSourcePlay");

        int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
        System.out.println("[AudioSource] state after play = " + state + " / AL_PLAYING = " + AL_PLAYING);
    }

    public void pause() {
        if (sourceId == 0) {
            return;
        }

        alSourcePause(sourceId);
    }

    public void stop() {
        if (sourceId == 0) {
            return;
        }

        alSourceStop(sourceId);
    }

    public void setLooping(boolean loop) {
        this.loop = loop;

        if (sourceId != 0) {
            alSourcei(sourceId, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
        }
    }

    public boolean isLooping() {
        return loop;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        applyGain();
    }

    public float getVolume() {
        return volume;
    }

    public void setPitch(float pitch) {
        this.pitch = Math.max(0.01f, pitch);

        if (sourceId != 0) {
            alSourcef(sourceId, AL_PITCH, this.pitch);
        }
    }

    public float getPitch() {
        return pitch;
    }

    public void setVolumeMultiplier(float volumeMultiplier) {
        this.volumeMultiplier = Math.max(0.0f, Math.min(1.0f, volumeMultiplier));
        applyGain();
    }

    public float getVolumeMultiplier() {
        return volumeMultiplier;
    }

    public void setPosition(float x, float y, float z) {
        if (sourceId != 0) {
            alSource3f(sourceId, AL_POSITION, x, y, z);
        }
    }

    public boolean isPlaying() {
        if (sourceId == 0) {
            return false;
        }

        return alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PLAYING;
    }

    public boolean isPlayOnAwake() {
        return playOnAwake;
    }

    public void setPlayOnAwake(boolean playOnAwake) {
        this.playOnAwake = playOnAwake;
    }

    private void applyGain() {
        if (sourceId != 0) {
            alSourcef(sourceId, AL_GAIN, volume * volumeMultiplier);
        }
    }

    @Override
    public void onDestroy() {
        dispose();
    }

    public void dispose() {
        if (sourceId != 0) {
            stop();
            alDeleteSources(sourceId);
            sourceId = 0;
        }

        if (clip != null) {
            clip.dispose();
            clip = null;
        }
    }

    private void checkOpenALError(String operation) {
        int error = alGetError();
        if (error != AL_NO_ERROR) {
            System.err.println("[AudioSource] OpenAL error after " + operation + ": " + error);
        }
    }
}