package ru.kuzma.presentation.importing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuzma.presentation.model.SceneObjectData;
import ru.kuzma.presentation.scene.SceneValidationException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GlbImportServiceTest {
    @TempDir Path temp;

    @Test
    void importsHierarchyAndCreatesSyntheticRootForSeveralRoots() throws Exception {
        String json = """
                {"asset":{"version":"2.0"},"scene":0,"scenes":[{"nodes":[0,1]}],
                "nodes":[{"name":"A","children":[2]},{"name":"B"},{"name":"C","mesh":0,"translation":[1,2,3]}],
                "meshes":[{}]}
                """;
        Path file = writeGlb(json);
        GlbImportService.Result result = new GlbImportService().importGlb(file, "Test", "models/test.glb");
        assertEquals(3, result.nodeCount());
        assertEquals(1, result.meshCount());
        assertEquals(4, result.scene().getObjects().size());
        SceneObjectData synthetic = result.scene().getObjects().get(3);
        assertTrue(synthetic.isSyntheticRoot());
        assertEquals("synthetic-root", result.scene().getModels().get(0).getRootObjectId());
        assertEquals("synthetic-root", result.scene().getObjects().get(0).getParentId());
        assertEquals(2f, result.scene().getObjects().get(2).getTransform().location().y());
    }

    @Test
    void readsBundledDemoGlb() throws Exception {
        Path file = Path.of("data/models/electric_motor.glb");
        GlbImportService.Result result = new GlbImportService().importGlb(file, "Motor", "models/electric_motor.glb");
        assertEquals(4, result.nodeCount());
        assertEquals(3, result.meshCount());
        assertEquals("node-0", result.scene().getModels().get(0).getRootObjectId());
    }

    @Test
    void rejectsWrongSignature() throws Exception {
        Path file = writeGlb("{\"asset\":{\"version\":\"2.0\"},\"nodes\":[{}]}");
        byte[] bytes = Files.readAllBytes(file);
        bytes[0] = 0;
        Files.write(file, bytes);
        assertThrows(SceneValidationException.class, () -> new GlbImportService().importGlb(file, null, "models/a.glb"));
    }

    private Path writeGlb(String json) throws IOException {
        byte[] content = json.getBytes(StandardCharsets.UTF_8);
        int padding = (4 - content.length % 4) % 4;
        int jsonLength = content.length + padding;
        ByteBuffer buffer = ByteBuffer.allocate(12 + 8 + jsonLength).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0x46546C67).putInt(2).putInt(buffer.capacity());
        buffer.putInt(jsonLength).putInt(0x4E4F534A).put(content);
        buffer.put(new byte[padding]);
        Path file = temp.resolve("test.glb");
        Files.write(file, buffer.array());
        return file;
    }
}
