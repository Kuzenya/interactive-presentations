package ru.kuzma.presentation.model;

import java.util.ArrayList;
import java.util.List;

/** Объект сцены. Дочерние объекты не дублируются: связь задаётся через parentId. */
public class SceneObjectData {
    private String id;
    private String parentId;
    private String name;
    private SceneObjectType objectType = SceneObjectType.EMPTY;
    private String sourceNodeKey;
    private TransformData transform = TransformData.identity();
    private String descriptionMarkdown = "";
    private List<String> imagePaths = new ArrayList<>();
    private boolean rotationEnabled = true;
    private boolean syntheticRoot;

    public SceneObjectData() { }

    public SceneObjectData(String id, String parentId, String name, SceneObjectType objectType) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.objectType = objectType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public SceneObjectType getObjectType() { return objectType; }
    public void setObjectType(SceneObjectType objectType) { this.objectType = objectType; }
    public String getSourceNodeKey() { return sourceNodeKey; }
    public void setSourceNodeKey(String sourceNodeKey) { this.sourceNodeKey = sourceNodeKey; }
    public TransformData getTransform() { return transform; }
    public void setTransform(TransformData transform) { this.transform = transform; }
    public String getDescriptionMarkdown() { return descriptionMarkdown; }
    public void setDescriptionMarkdown(String descriptionMarkdown) { this.descriptionMarkdown = descriptionMarkdown; }
    public List<String> getImagePaths() { return imagePaths; }
    public void setImagePaths(List<String> imagePaths) { this.imagePaths = imagePaths; }
    public boolean isRotationEnabled() { return rotationEnabled; }
    public void setRotationEnabled(boolean rotationEnabled) { this.rotationEnabled = rotationEnabled; }
    public boolean isSyntheticRoot() { return syntheticRoot; }
    public void setSyntheticRoot(boolean syntheticRoot) { this.syntheticRoot = syntheticRoot; }
}
