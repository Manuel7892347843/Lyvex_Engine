package core;

import core.sorting.SortingLayerManager;

public class ProjectSettings {
    private static final SortingLayerManager sortingLayerManager = new SortingLayerManager();

    public static SortingLayerManager getSortingLayerManager() {
        return sortingLayerManager;
    }
}