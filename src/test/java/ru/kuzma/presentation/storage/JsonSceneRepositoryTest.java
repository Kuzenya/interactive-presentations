package ru.kuzma.presentation.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuzma.presentation.model.SceneData;
import ru.kuzma.presentation.scene.DemoSceneFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonSceneRepositoryTest {
    @TempDir Path temp;

    @Test
    void savesAndLoadsCompleteScene() throws Exception {
        JsonSceneRepository repository = new JsonSceneRepository();
        Path file = temp.resolve("nested/scene.json");
        SceneData original = new DemoSceneFactory().create();
        repository.save(file, original);
        SceneData loaded = repository.load(file);
        assertEquals(original.getId(), loaded.getId());
        assertEquals(4, loaded.getObjects().size());
        assertEquals("motor", loaded.getObjects().get(1).getParentId());
        assertEquals(2, loaded.getEvents().get(0).getAnimations().size());
        assertEquals("models/electric_motor.glb", loaded.getModels().get(0).getFilePath());
        assertTrue(Files.readString(file).contains("descriptionMarkdown"));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(Exception.class, () -> new JsonSceneRepository().load(Files.writeString(temp.resolve("bad.json"), "{broken")));
    }
}
