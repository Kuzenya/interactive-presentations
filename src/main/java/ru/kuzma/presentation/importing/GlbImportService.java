package ru.kuzma.presentation.importing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import ru.kuzma.presentation.model.*;
import ru.kuzma.presentation.scene.HierarchyValidator;
import ru.kuzma.presentation.scene.SceneValidationException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Читает GLB 2.0 без стороннего native-кода и переносит узлы glTF в SceneObjectData.
 * Сложность: O(b + n + v), где b — размер файла, n — число узлов, v — число проверяемых ссылок.
 */
public final class GlbImportService {
    private static final int GLB_MAGIC = 0x46546C67;
    private static final int JSON_CHUNK = 0x4E4F534A;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HierarchyValidator validator = new HierarchyValidator();

    public record Result(SceneData scene, int nodeCount, int meshCount, int animationCount) { }

    public Result importGlb(Path source, String sceneName, String storedModelPath) throws IOException {
        byte[] bytes = Files.readAllBytes(source);
        JsonNode gltf = readGlbJson(bytes);
        validateResources(gltf);

        JsonNode nodes = gltf.path("nodes");
        if (!nodes.isArray() || nodes.isEmpty()) {
            throw new SceneValidationException("GLB не содержит узлов nodes");
        }

        SceneData scene = new SceneData();
        scene.setId("scene-" + UUID.randomUUID());
        scene.setName(sceneName == null || sceneName.isBlank() ? source.getFileName().toString() : sceneName);

        int[] parents = new int[nodes.size()];
        Arrays.fill(parents, -1);
        for (int i = 0; i < nodes.size(); i++) {
            JsonNode children = nodes.get(i).path("children");
            if (!children.isArray()) continue;
            for (JsonNode child : children) {
                int index = child.asInt(-1);
                if (index < 0 || index >= nodes.size()) {
                    throw new SceneValidationException("Некорректный индекс дочернего узла: " + index);
                }
                if (parents[index] != -1) {
                    throw new SceneValidationException("Узел " + index + " имеет несколько родителей");
                }
                parents[index] = i;
            }
        }

        List<SceneObjectData> objects = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            JsonNode node = nodes.get(i);
            String id = "node-" + i;
            String name = node.path("name").asText("node_" + i);
            SceneObjectData object = new SceneObjectData(id,
                    parents[i] < 0 ? null : "node-" + parents[i], name,
                    node.has("mesh") ? SceneObjectType.MESH : SceneObjectType.GROUP);
            object.setSourceNodeKey(name);
            object.setTransform(readTransform(node));
            object.setDescriptionMarkdown("## " + name + "\n\nОписание компонента можно изменить в JSON-файле сцены.");
            objects.add(object);
        }

        List<Integer> roots = sceneRoots(gltf, nodes.size(), parents);
        String rootObjectId;
        if (roots.size() == 1) {
            rootObjectId = "node-" + roots.get(0);
        } else {
            SceneObjectData synthetic = new SceneObjectData("synthetic-root", null,
                    scene.getName(), SceneObjectType.EMPTY);
            synthetic.setSyntheticRoot(true);
            synthetic.setDescriptionMarkdown("# " + scene.getName() + "\n\nСинтетический корень импортированной GLB-модели.");
            objects.add(synthetic);
            for (int root : roots) objects.get(root).setParentId("synthetic-root");
            rootObjectId = "synthetic-root";
        }
        scene.setObjects(objects);
        ModelResourceData model = new ModelResourceData("model-1", scene.getName(), storedModelPath, rootObjectId);
        scene.setModels(List.of(model));
        validator.validate(scene);
        return new Result(scene, nodes.size(), gltf.path("meshes").size(), gltf.path("animations").size());
    }

    private JsonNode readGlbJson(byte[] bytes) throws IOException {
        if (bytes.length < 20) throw new SceneValidationException("Файл слишком мал для GLB 2.0");
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (buffer.getInt(0) != GLB_MAGIC) throw new SceneValidationException("Неверная GLB-сигнатура");
        if (buffer.getInt(4) != 2) throw new SceneValidationException("Поддерживается только GLB версии 2");
        int declaredLength = buffer.getInt(8);
        if (declaredLength != bytes.length) {
            throw new SceneValidationException("Длина GLB в заголовке не совпадает с размером файла");
        }
        int offset = 12;
        while (offset + 8 <= bytes.length) {
            int chunkLength = buffer.getInt(offset);
            int chunkType = buffer.getInt(offset + 4);
            offset += 8;
            if (chunkLength < 0 || offset + chunkLength > bytes.length) {
                throw new SceneValidationException("Поврежден chunk GLB");
            }
            if (chunkType == JSON_CHUNK) {
                String json = new String(bytes, offset, chunkLength, StandardCharsets.UTF_8);
                return mapper.readTree(json);
            }
            offset += chunkLength;
        }
        throw new SceneValidationException("В GLB отсутствует JSON-chunk");
    }

    private void validateResources(JsonNode gltf) {
        for (JsonNode extension : gltf.path("extensionsRequired")) {
            if ("KHR_draco_mesh_compression".equals(extension.asText())) {
                throw new SceneValidationException("Draco-сжатие не поддерживается этим прототипом");
            }
        }
        for (JsonNode buffer : gltf.path("buffers")) {
            if (buffer.has("uri")) throw new SceneValidationException("Используйте GLB со встроенными buffers, без внешних URI");
        }
        for (JsonNode image : gltf.path("images")) {
            if (image.has("uri")) throw new SceneValidationException("Используйте GLB со встроенными изображениями, без внешних URI");
        }
    }

    private List<Integer> sceneRoots(JsonNode gltf, int nodeCount, int[] parents) {
        int sceneIndex = gltf.path("scene").asInt(0);
        JsonNode scenes = gltf.path("scenes");
        JsonNode roots = scenes.isArray() && sceneIndex >= 0 && sceneIndex < scenes.size()
                ? scenes.get(sceneIndex).path("nodes") : null;
        LinkedHashSet<Integer> result = new LinkedHashSet<>();
        if (roots != null && roots.isArray()) {
            for (JsonNode root : roots) {
                int index = root.asInt(-1);
                if (index < 0 || index >= nodeCount) throw new SceneValidationException("Некорректный корневой узел glTF");
                result.add(index);
            }
        }
        if (result.isEmpty()) {
            for (int i = 0; i < nodeCount; i++) if (parents[i] == -1) result.add(i);
        }
        if (result.isEmpty()) throw new SceneValidationException("Не удалось определить корневые узлы");
        return List.copyOf(result);
    }

    private TransformData readTransform(JsonNode node) {
        if (node.has("matrix")) return decomposeMatrix(node.path("matrix"));
        return new TransformData(
                vector(node.path("translation"), Vector3Data.zero()),
                quaternion(node.path("rotation")),
                vector(node.path("scale"), Vector3Data.one())
        );
    }

    private Vector3Data vector(JsonNode node, Vector3Data defaultValue) {
        if (!node.isArray()) return defaultValue;
        if (node.size() != 3) throw new SceneValidationException("Вектор glTF должен содержать 3 компонента");
        return new Vector3Data(node.get(0).floatValue(), node.get(1).floatValue(), node.get(2).floatValue());
    }

    private QuaternionData quaternion(JsonNode node) {
        if (!node.isArray()) return QuaternionData.identity();
        if (node.size() != 4) throw new SceneValidationException("Кватернион glTF должен содержать 4 компонента");
        return new QuaternionData(node.get(0).floatValue(), node.get(1).floatValue(),
                node.get(2).floatValue(), node.get(3).floatValue());
    }

    private TransformData decomposeMatrix(JsonNode node) {
        if (!node.isArray() || node.size() != 16) {
            throw new SceneValidationException("Матрица узла glTF должна содержать 16 компонентов");
        }
        float[] a = new float[16];
        for (int i = 0; i < 16; i++) a[i] = node.get(i).floatValue();
        // glTF хранит column-major, конструктор Matrix4f принимает row-major.
        Matrix4f matrix = new Matrix4f(a[0], a[4], a[8], a[12],
                a[1], a[5], a[9], a[13], a[2], a[6], a[10], a[14], a[3], a[7], a[11], a[15]);
        Vector3f translation = matrix.toTranslationVector();
        Vector3f scale = matrix.toScaleVector();
        Quaternion rotation = matrix.toRotationQuat();
        return new TransformData(Vector3Data.fromJme(translation), QuaternionData.fromJme(rotation), Vector3Data.fromJme(scale));
    }
}
