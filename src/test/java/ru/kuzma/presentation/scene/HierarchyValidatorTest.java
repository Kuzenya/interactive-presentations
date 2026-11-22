package ru.kuzma.presentation.scene;

import org.junit.jupiter.api.Test;
import ru.kuzma.presentation.model.SceneData;
import ru.kuzma.presentation.model.SceneObjectData;
import ru.kuzma.presentation.model.SceneObjectType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HierarchyValidatorTest {
    private SceneData scene(SceneObjectData... objects) {
        SceneData scene = new SceneData();
        scene.setId("test");
        scene.setName("Test");
        scene.setObjects(List.of(objects));
        return scene;
    }

    @Test
    void acceptsNormalTree() {
        SceneData value = scene(new SceneObjectData("a", null, "A", SceneObjectType.GROUP),
                new SceneObjectData("b", "a", "B", SceneObjectType.MESH));
        assertDoesNotThrow(() -> new HierarchyValidator().validate(value));
    }

    @Test
    void rejectsMissingParent() {
        SceneData value = scene(new SceneObjectData("a", "missing", "A", SceneObjectType.MESH));
        assertThrows(SceneValidationException.class, () -> new HierarchyValidator().validate(value));
    }

    @Test
    void rejectsCycle() {
        SceneData value = scene(new SceneObjectData("a", "b", "A", SceneObjectType.GROUP),
                new SceneObjectData("b", "a", "B", SceneObjectType.GROUP));
        SceneValidationException error = assertThrows(SceneValidationException.class,
                () -> new HierarchyValidator().validate(value));
        assertTrue(error.getMessage().contains("цикл"));
    }

    @Test
    void rejectsDuplicateIds() {
        SceneData value = scene(new SceneObjectData("a", null, "A", SceneObjectType.GROUP),
                new SceneObjectData("a", null, "Again", SceneObjectType.GROUP));
        assertThrows(SceneValidationException.class, () -> new HierarchyValidator().validate(value));
    }
}
