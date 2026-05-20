package core.audio;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbisInfo;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class AudioClip {

    private final int bufferId;

    public AudioClip(Path path) {
        System.out.println("[AudioClip] Creating clip from: " + path);

        bufferId = alGenBuffers();
        checkOpenALError("alGenBuffers");

        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            IntBuffer error = BufferUtils.createIntBuffer(1);

            long decoder = stb_vorbis_open_filename(path.toString(), error, null);
            if (decoder == NULL) {
                throw new RuntimeException("Failed to open audio file: " + path + ", stb_vorbis error: " + error.get(0));
            }

            stb_vorbis_get_info(decoder, info);

            int channels = info.channels();
            int sampleRate = info.sample_rate();
            int samples = stb_vorbis_stream_length_in_samples(decoder);

            System.out.println("[AudioClip] channels = " + channels);
            System.out.println("[AudioClip] sampleRate = " + sampleRate);
            System.out.println("[AudioClip] samples = " + samples);

            ShortBuffer pcm = BufferUtils.createShortBuffer(samples * channels);
            int samplesDecoded = stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            stb_vorbis_close(decoder);

            System.out.println("[AudioClip] samplesDecoded = " + samplesDecoded);

            if (samplesDecoded <= 0) {
                throw new RuntimeException("No audio samples decoded from: " + path);
            }

            pcm.limit(samplesDecoded * channels);
            pcm.position(0);

            int format;
            if (channels == 1) {
                format = AL_FORMAT_MONO16;
            } else if (channels == 2) {
                format = AL_FORMAT_STEREO16;
            } else {
                throw new RuntimeException("Unsupported channel count: " + channels);
            }

            alBufferData(bufferId, format, pcm, sampleRate);
            checkOpenALError("alBufferData");

            System.out.println("[AudioClip] OpenAL buffer created: " + bufferId);
        }
    }

    public int getBufferId() {
        return bufferId;
    }

    public void dispose() {
        alDeleteBuffers(bufferId);
    }

    private void checkOpenALError(String operation) {
        int error = alGetError();
        if (error != AL_NO_ERROR) {
            System.err.println("[AudioClip] OpenAL error after " + operation + ": " + error);
        }
    }
}