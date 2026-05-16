package core.sorting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SortingLayerManager {
    private final List<String> layers = new ArrayList<>();

    public SortingLayerManager() {
        layers.add("Default");
    }

    public List<String> getLayers() {
        return new ArrayList<>(layers);
    }

    public void addLayer(String name) {
        if (!layerExists(name)) {
            layers.add(name);
        }
    }

    public void removeLayer(int index) {
        if (index >= 0 && index < layers.size() && !layers.get(index).equals("Default")) {
            layers.remove(index);
        }
    }

    public void renameLayer(int index, String newName) {
        if (index >= 0 && index < layers.size() && !layerExists(newName)) {
            layers.set(index, newName);
        }
    }

    public void moveLayerUp(int index) {
        if (index > 0) {
            Collections.swap(layers, index, index - 1);
        }
    }

    public void moveLayerDown(int index) {
        if (index < layers.size() - 1) {
            Collections.swap(layers, index, index + 1);
        }
    }

    public boolean layerExists(String name) {
        return layers.contains(name);
    }

    public int getLayerPriority(String name) {
        return layers.indexOf(name);
    }

    public String getLayerName(int priority) {
        if (priority >= 0 && priority < layers.size()) {
            return layers.get(priority);
        }
        return "Default";
    }
}