package core;

import core.component.ComponentData;

import java.util.ArrayList;
import java.util.List;

public class GameObjectData {
    public String id;
    public String name;

    public List<ComponentData> components = new ArrayList<>();
    public List<GameObjectData> children = new ArrayList<>();
}