package ru.kuzma.presentation.storage;

import ru.kuzma.presentation.model.SceneData;

import java.io.IOException;
import java.nio.file.Path;

public interface SceneRepository {
    SceneData load(Path file) throws IOException;
    void save(Path file, SceneData scene) throws IOException;
}
