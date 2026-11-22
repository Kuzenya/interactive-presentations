package ru.kuzma.presentation.model;

/** Ссылка на локальный GLB-файл и корневой объект его иерархии. */
public class ModelResourceData {
    private String id;
    private String name;
    private String filePath;
    private String rootObjectId;

    public ModelResourceData() { }

    public ModelResourceData(String id, String name, String filePath, String rootObjectId) {
        this.id = id;
        this.name = name;
        this.filePath = filePath;
        this.rootObjectId = rootObjectId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getRootObjectId() { return rootObjectId; }
    public void setRootObjectId(String rootObjectId) { this.rootObjectId = rootObjectId; }
}
