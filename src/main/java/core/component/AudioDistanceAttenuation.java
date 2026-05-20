package core.component;

import core.gameobject.GameObject;

public class AudioDistanceAttenuation extends Component {

    public String listenerName = "Player";
    public float radius = 10.0f;
    public boolean muteOutsideRadius = true;

    private transient AudioSource audioSource;
    private transient GameObject listener;

    @Override
    public void start() {
        audioSource = getGameObject().getComponent(AudioSource.class);

        if (audioSource == null) {
            System.err.println("[AudioDistanceAttenuation] Missing AudioSource on GameObject.");
            return;
        }

        if (listenerName != null && !listenerName.isBlank()) {
            listener = getGameObject().findGameObject(listenerName);
        }

        if (listener == null) {
            System.err.println("[AudioDistanceAttenuation] Listener not found: " + listenerName);
        }
    }

    @Override
    public void update() {
        if (audioSource == null) {
            return;
        }

        if (listener == null && listenerName != null && !listenerName.isBlank()) {
            listener = getGameObject().findGameObject(listenerName);
        }

        if (listener == null) {
            audioSource.setVolumeMultiplier(1.0f);
            return;
        }

        float safeRadius = Math.max(0.001f, radius);

        Transform sourceTransform = getGameObject().getTransform();
        Transform listenerTransform = listener.getTransform();

        float dx = sourceTransform.getX() - listenerTransform.getX();
        float dy = sourceTransform.getY() - listenerTransform.getY();

        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        float multiplier = 1.0f - distance / safeRadius;
        multiplier = Math.max(0.0f, Math.min(1.0f, multiplier));

        if (!muteOutsideRadius && distance > safeRadius) {
            multiplier = 0.0f;
        }

        audioSource.setVolumeMultiplier(multiplier);
    }

    @Override
    public void onDisable() {
        if (audioSource != null) {
            audioSource.setVolumeMultiplier(1.0f);
        }
    }

    @Override
    public void onDestroy() {
        if (audioSource != null) {
            audioSource.setVolumeMultiplier(1.0f);
        }
    }
}