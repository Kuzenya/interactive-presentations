package ru.kuzma.presentation.model;

import java.util.ArrayList;
import java.util.List;

/** Корневой документ локальной сцены. Все пути относительны каталогу data. */
public class SceneData {
    private int schemaVersion = 1;
    private String id;
    private String name;
    private String backgroundColor = "#202830";
    private String hdrFilePath;
    private float hdrRotation;
    private float hdrIntensity = 1;
    private List<ModelResourceData> models = new ArrayList<>();
    private List<SceneObjectData> objects = new ArrayList<>();
    private List<EventData> events = new ArrayList<>();

    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
    public String getHdrFilePath() { return hdrFilePath; }
    public void setHdrFilePath(String hdrFilePath) { this.hdrFilePath = hdrFilePath; }
    public float getHdrRotation() { return hdrRotation; }
    public void setHdrRotation(float hdrRotation) { this.hdrRotation = hdrRotation; }
    public float getHdrIntensity() { return hdrIntensity; }
    public void setHdrIntensity(float hdrIntensity) { this.hdrIntensity = hdrIntensity; }
    public List<ModelResourceData> getModels() { return models; }
    public void setModels(List<ModelResourceData> models) { this.models = models; }
    public List<SceneObjectData> getObjects() { return objects; }
    public void setObjects(List<SceneObjectData> objects) { this.objects = objects; }
    public List<EventData> getEvents() { return events; }
    public void setEvents(List<EventData> events) { this.events = events; }
}
