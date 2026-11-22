package ru.kuzma.presentation.storage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ru.kuzma.presentation.model.SceneData;
import ru.kuzma.presentation.scene.HierarchyValidator;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** JSON-хранилище с валидацией и атомарной заменой файла при сохранении. */
public final class JsonSceneRepository implements SceneRepository {
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final HierarchyValidator validator = new HierarchyValidator();

    @Override
    public SceneData load(Path file) throws IOException {
        SceneData scene = mapper.readValue(file.toFile(), SceneData.class);
        validator.validate(scene);
        return scene;
    }

    @Override
    public void save(Path file, SceneData scene) throws IOException {
        validator.validate(scene);
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        try {
            mapper.writeValue(temporary.toFile(), scene);
            try {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
