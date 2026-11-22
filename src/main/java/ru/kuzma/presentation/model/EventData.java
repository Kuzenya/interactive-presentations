package ru.kuzma.presentation.model;

import java.util.ArrayList;
import java.util.List;

/** Событие объединяет несколько анимаций и запускается повторным действием в обратную сторону. */
public class EventData {
    private String id;
    private String ownerObjectId;
    private String name;
    private List<AnimationSpec> animations = new ArrayList<>();

    public EventData() { }

    public EventData(String id, String ownerObjectId, String name) {
        this.id = id;
        this.ownerObjectId = ownerObjectId;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerObjectId() { return ownerObjectId; }
    public void setOwnerObjectId(String ownerObjectId) { this.ownerObjectId = ownerObjectId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<AnimationSpec> getAnimations() { return animations; }
    public void setAnimations(List<AnimationSpec> animations) { this.animations = animations; }
}
