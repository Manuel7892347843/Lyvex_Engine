package core.physics;

import java.util.ArrayList;
import java.util.List;

public class PhysicsLayerManager {
    private List<PhysicsLayer> layers = new ArrayList<>();

    public PhysicsLayerManager() {
        // Layer di default
        layers.add(new PhysicsLayer("Default", 0));
        layers.add(new PhysicsLayer("Player", 1));
        layers.add(new PhysicsLayer("Enemy", 2));
        layers.add(new PhysicsLayer("Ground", 3));
        layers.add(new PhysicsLayer("UI", 4));
    }

    public List<PhysicsLayer> getLayers() {
        return new ArrayList<>(layers);
    }

    public int getLayerCount() {
        return layers.size();
    }

    public PhysicsLayer getLayer(int index) {
        if (index < 0 || index >= layers.size()) return null;
        return layers.get(index);
    }

    public PhysicsLayer getLayer(String name) {
        for (PhysicsLayer layer : layers) {
            if (layer.name.equals(name)) {
                return layer;
            }
        }
        return null;
    }

    public int getLayerIndex(String name) {
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).name.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public int getLayerMask(String name) {
        PhysicsLayer layer = getLayer(name);
        return layer != null ? layer.getMask() : 0xFFFF;
    }

    public int getLayerMask(int index) {
        PhysicsLayer layer = getLayer(index);
        return layer != null ? layer.getMask() : 0xFFFF;
    }

    public boolean layerExists(String name) {
        return getLayer(name) != null;
    }

    public void addLayer(String name) {
        if (layerExists(name)) return;
        int bit = findFreeBit();
        if (bit >= 0) {
            layers.add(new PhysicsLayer(name, bit));
        }
    }

    public void removeLayer(int index) {
        if (index > 0 && index < layers.size()) {
            layers.remove(index);
        }
    }

    public void renameLayer(int index, String newName) {
        if (index >= 0 && index < layers.size() && !layerExists(newName)) {
            layers.get(index).name = newName;
        }
    }

    public void setLayerMask(int layerIndex, int mask) {
        if (layerIndex >= 0 && layerIndex < layers.size()) {
            layers.get(layerIndex).collisionMask = mask;
        }
    }

    public void toggleLayerInMask(int layerIndex, int targetLayerIndex, boolean enabled) {
        if (layerIndex < 0 || layerIndex >= layers.size()) return;
        if (targetLayerIndex < 0 || targetLayerIndex >= layers.size()) return;

        int targetBit = layers.get(targetLayerIndex).bit;
        PhysicsLayer layer = layers.get(layerIndex);

        if (enabled) {
            layer.collisionMask |= (1 << targetBit);
        } else {
            layer.collisionMask &= ~(1 << targetBit);
        }
    }

    public boolean canLayersCollide(int layerA, int layerB) {
        if (layerA < 0 || layerA >= layers.size()) return false;
        if (layerB < 0 || layerB >= layers.size()) return false;

        PhysicsLayer a = layers.get(layerA);
        PhysicsLayer b = layers.get(layerB);

        return ((1 << b.bit) & a.collisionMask) != 0 &&
                ((1 << a.bit) & b.collisionMask) != 0;
    }

    public void moveLayerUp(int index) {
        if (index > 1) {
            PhysicsLayer temp = layers.get(index);
            layers.set(index, layers.get(index - 1));
            layers.set(index - 1, temp);
        }
    }

    public void moveLayerDown(int index) {
        if (index >= 1 && index < layers.size() - 1) {
            PhysicsLayer temp = layers.get(index);
            layers.set(index, layers.get(index + 1));
            layers.set(index + 1, temp);
        }
    }

    private int findFreeBit() {
        for (int bit = 0; bit < 16; bit++) {
            boolean used = false;
            for (PhysicsLayer layer : layers) {
                if (layer.bit == bit) {
                    used = true;
                    break;
                }
            }
            if (!used) return bit;
        }
        return -1; // Tutti i bit occupati (max 16 layer)
    }

    /**
     * Rappresenta un singolo layer fisico.
     */
    public static class PhysicsLayer {
        public String name;
        public int bit;              // Bit position (0-15)
        public int collisionMask;    // Con quali layer può collidere

        public PhysicsLayer(String name, int bit) {
            this.name = name;
            this.bit = bit;
            this.collisionMask = 0xFFFF; // Di default collide con tutti
        }

        public int getMask() {
            return 1 << bit;
        }

        @Override
        public String toString() {
            return name + " (bit " + bit + ")";
        }
    }
}