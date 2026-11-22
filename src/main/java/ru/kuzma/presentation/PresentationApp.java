package ru.kuzma.presentation;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.collision.CollisionResult;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.ui.Picture;
import ru.kuzma.presentation.animation.AnimationRuntime;
import ru.kuzma.presentation.interaction.SelectionService;
import ru.kuzma.presentation.model.*;
import ru.kuzma.presentation.scene.DemoSceneFactory;
import ru.kuzma.presentation.storage.JsonSceneRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Локальный проигрыватель сцены. Никакого сервера или аккаунтов в этом прототипе нет. */
public final class PresentationApp extends SimpleApplication {
    private final Path sceneFile;
    private final JsonSceneRepository repository = new JsonSceneRepository();
    private final SelectionService selectionService = new SelectionService();
    private final Map<String, SceneObjectData> objectsById = new HashMap<>();
    private final Map<String, Spatial> spatialsById = new HashMap<>();

    private SceneData scene;
    private AnimationRuntime animationRuntime;
    private String hoveredId;
    private String selectedId;
    private String activeEventId;
    private BitmapText infoText;
    private BitmapText statusText;
    private Picture preview;
    private Geometry highlight;

    private PresentationApp(Path sceneFile) { this.sceneFile = sceneFile; }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--import")) {
            ImportCli.run(args);
            return;
        }
        Path sceneFile = Path.of("data", "demo-scene.json");
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--scene")) sceneFile = Path.of(args[i + 1]);
        }
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Интерактивные 3D-презентации — локальный прототип");
        settings.setResolution(1280, 720);
        settings.setVSync(true);
        settings.setAudioRenderer(null); // Прототип не использует звук и работает без аудиоустройства.
        PresentationApp app = new PresentationApp(sceneFile);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.setDisplayFps(false);
        app.setDisplayStatView(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        try {
            assetManager.registerLocator("data", FileLocator.class);
            if (!Files.exists(sceneFile) && sceneFile.endsWith(Path.of("data", "demo-scene.json"))) {
                repository.save(sceneFile, new DemoSceneFactory().create());
            }
            scene = repository.load(sceneFile);
            for (SceneObjectData object : scene.getObjects()) objectsById.put(object.getId(), object);

            viewPort.setBackgroundColor(parseColor(scene.getBackgroundColor()));
            flyCam.setMoveSpeed(4f);
            flyCam.setDragToRotate(true);
            inputManager.setCursorVisible(true);
            resetCamera();
            addLightsAndFloor();
            loadModels();
            createHighlight();
            createHud();
            registerInput();
            animationRuntime = new AnimationRuntime(scene, spatialsById);
            selectObject(scene.getModels().isEmpty() ? scene.getObjects().get(0).getId()
                    : scene.getModels().get(0).getRootObjectId());
        } catch (Exception exception) {
            System.err.println("Не удалось открыть сцену: " + exception.getMessage());
            exception.printStackTrace();
            stop();
        }
    }

    private void loadModels() {
        for (ModelResourceData modelResource : scene.getModels()) {
            Spatial loaded = assetManager.loadModel(modelResource.getFilePath());
            convertPbrToLighting(loaded);
            Spatial rootSpatial = loaded;
            SceneObjectData rootObject = objectsById.get(modelResource.getRootObjectId());
            if (rootObject != null && rootObject.isSyntheticRoot()) {
                Node wrapper = new Node(rootObject.getName());
                wrapper.attachChild(loaded);
                rootSpatial = wrapper;
            }
            rootNode.attachChild(rootSpatial);
            indexSpatial(rootSpatial, modelResource.getRootObjectId());
            for (SceneObjectData object : scene.getObjects()) {
                Spatial mapped = spatialsById.get(object.getId());
                if (mapped != null) applyTransform(mapped, object.getTransform());
            }
            Spatial mappedRoot = spatialsById.get(modelResource.getRootObjectId());
            if (mappedRoot == null) {
                mappedRoot = rootSpatial;
                spatialsById.put(modelResource.getRootObjectId(), mappedRoot);
            }
            mappedRoot.setUserData(SelectionService.OBJECT_ID, modelResource.getRootObjectId());
            if (rootObject != null && spatialsById.get(rootObject.getId()) == mappedRoot) {
                applyTransform(mappedRoot, rootObject.getTransform());
            }
        }
        rootNode.updateGeometricState();
    }

    /** Для простого desktop-прототипа PBR переводится в Lighting, чтобы не требовать HDR/LightProbe. */
    private void convertPbrToLighting(Spatial spatial) {
        if (spatial instanceof Geometry geometry && geometry.getMaterial() != null
                && geometry.getMaterial().getMaterialDef().getName().contains("PBR")) {
            Material old = geometry.getMaterial();
            ColorRGBA base = demoColor(geometry.getName());
            if (base == null) base = old.getParamValue("BaseColor") instanceof ColorRGBA color ? color : ColorRGBA.LightGray;
            Material replacement = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            // Без этого флага Lighting.j3md игнорирует Diffuse/Ambient и выводит модель серой.
            replacement.setBoolean("UseMaterialColors", true);
            replacement.setColor("Diffuse", base.clone());
            replacement.setColor("Ambient", base.clone());
            replacement.setColor("Specular", ColorRGBA.White.mult(0.25f));
            replacement.setFloat("Shininess", 24);
            if (old.getParamValue("BaseColorMap") instanceof com.jme3.texture.Texture texture) {
                replacement.setTexture("DiffuseMap", texture);
            }
            geometry.setMaterial(replacement);
        }
        if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) convertPbrToLighting(child);
        }
    }

    private static ColorRGBA demoColor(String name) {
        String value = name == null ? "" : name.toLowerCase();
        if (value.contains("stator")) return new ColorRGBA(0.263f, 0.412f, 0.510f, 1);
        if (value.contains("front") || value.contains("shaft")) return new ColorRGBA(0.698f, 0.737f, 0.765f, 1);
        if (value.contains("rear") || value.contains("fan")) return new ColorRGBA(0.204f, 0.247f, 0.282f, 1);
        return null;
    }

    private void applyKnownColor(Spatial spatial, String objectId) {
        ColorRGBA color = switch (objectId) {
            case "stator" -> new ColorRGBA(0.263f, 0.412f, 0.510f, 1);
            case "shaft" -> new ColorRGBA(0.698f, 0.737f, 0.765f, 1);
            case "fan" -> new ColorRGBA(0.204f, 0.247f, 0.282f, 1);
            default -> null;
        };
        if (color == null) return;
        if (spatial instanceof Geometry geometry && geometry.getMaterial() != null) {
            geometry.getMaterial().setColor("Diffuse", color.clone());
            geometry.getMaterial().setColor("Ambient", color.clone());
        }
        if (spatial instanceof Node node) for (Spatial child : node.getChildren()) applyKnownColor(child, objectId);
    }

    private void indexSpatial(Spatial spatial, String rootObjectId) {
        SceneObjectData match = scene.getObjects().stream()
                .filter(o -> spatial.getName() != null && spatial.getName().equals(o.getSourceNodeKey()))
                .findFirst().orElse(null);
        if (match != null) {
            spatialsById.put(match.getId(), spatial);
            spatial.setUserData(SelectionService.OBJECT_ID, match.getId());
            applyKnownColor(spatial, match.getId());
        }
        if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) indexSpatial(child, rootObjectId);
        }
    }

    private static void applyTransform(Spatial spatial, TransformData transform) {
        if (transform == null) return;
        spatial.setLocalTranslation(transform.location().toJme());
        spatial.setLocalRotation(transform.rotation().toJme());
        spatial.setLocalScale(transform.scale().toJme());
    }

    private void addLightsAndFloor() {
        DirectionalLight sun = new DirectionalLight(new Vector3f(-1, -2, -1).normalizeLocal(), ColorRGBA.White.mult(0.9f));
        DirectionalLight fill = new DirectionalLight(new Vector3f(1, -0.5f, 1).normalizeLocal(), ColorRGBA.White.mult(0.45f));
        AmbientLight ambient = new AmbientLight(ColorRGBA.White.mult(0.55f));
        rootNode.addLight(sun);
        rootNode.addLight(fill);
        rootNode.addLight(ambient);

        Geometry floor = new Geometry("Floor", new Box(8, 0.05f, 8));
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", new ColorRGBA(0.12f, 0.15f, 0.18f, 1));
        floor.setMaterial(material);
        floor.setLocalTranslation(0, -0.38f, 0);
        rootNode.attachChild(floor);
    }

    private void createHighlight() {
        highlight = new Geometry("Highlight", new WireBox(1, 1, 1));
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", ColorRGBA.Yellow);
        highlight.setMaterial(material);
        highlight.setCullHint(Spatial.CullHint.Always);
        rootNode.attachChild(highlight);
    }

    private void createHud() {
        BitmapFont font = assetManager.loadFont("fonts/DejaVuSans.fnt");
        BitmapText title = new BitmapText(font);
        title.setText(scene.getName());
        title.setLocalTranslation(20, settings.getHeight() - 20, 0);
        guiNode.attachChild(title);

        infoText = new BitmapText(font);
        infoText.setSize(21);
        infoText.setLocalTranslation(20, settings.getHeight() - 90, 0);
        guiNode.attachChild(infoText);

        BitmapText help = new BitmapText(font);
        help.setSize(17);
        help.setText("ЛКМ — выбрать   E / ПКМ — событие   F5 — сохранить   R — камера   Esc — выход\nWASD + перетаскивание ЛКМ — перемещение камеры");
        help.setLocalTranslation(20, 48, 0);
        guiNode.attachChild(help);

        statusText = new BitmapText(font);
        statusText.setSize(18);
        statusText.setLocalTranslation(20, 82, 0);
        guiNode.attachChild(statusText);

        preview = new Picture("Object image");
        preview.setWidth(280);
        preview.setHeight(210);
        preview.setPosition(settings.getWidth() - 300, settings.getHeight() - 250);
        preview.setCullHint(Spatial.CullHint.Always);
        guiNode.attachChild(preview);
    }

    private void registerInput() {
        inputManager.addMapping("Select", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Activate", new KeyTrigger(KeyInput.KEY_E), new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping("Save", new KeyTrigger(KeyInput.KEY_F5));
        inputManager.addMapping("ResetCamera", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener((ActionListener) (name, pressed, tpf) -> {
            if (!pressed) return;
            switch (name) {
                case "Select" -> selectObjectAtCursor();
                case "Activate" -> activateSelectedEvent();
                case "Save" -> saveScene();
                case "ResetCamera" -> resetCamera();
            }
        }, "Select", "Activate", "Save", "ResetCamera");
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (animationRuntime == null) return;
        animationRuntime.update(tpf);
        SelectionService.Hit hit = pick().orElse(null);
        hoveredId = hit == null ? null : hit.objectId();
        updateHighlight();
        if (activeEventId != null) {
            statusText.setText("Прогресс события: " + Math.round(animationRuntime.progress(activeEventId) * 100) + "%");
        }
    }

    private java.util.Optional<SelectionService.Hit> pick() {
        Vector2f cursor = inputManager.getCursorPosition();
        Vector3f origin = cam.getWorldCoordinates(cursor, 0f);
        Vector3f direction = cam.getWorldCoordinates(cursor, 0.3f).subtractLocal(origin).normalizeLocal();
        return selectionService.select(rootNode, new Ray(origin, direction));
    }

    private void selectObjectAtCursor() {
        String id = pick().map(SelectionService.Hit::objectId).orElse(null);
        selectObject(id);
    }

    private void selectObject(String id) {
        selectedId = id;
        activeEventId = null;
        SceneObjectData object = id == null ? null : objectsById.get(id);
        if (object == null) {
            infoText.setText("Объект не выбран");
            preview.setCullHint(Spatial.CullHint.Always);
            statusText.setText("");
            return;
        }
        String plain = object.getDescriptionMarkdown() == null ? "" : object.getDescriptionMarkdown()
                .replace("#", "").replace("**", "").replace("`", "").trim();
        if (plain.startsWith(object.getName())) plain = plain.substring(object.getName().length()).trim();
        infoText.setText(wrap(object.getName() + "\n" + plain, 58));
        String image = findImage(object);
        if (image == null) {
            preview.setCullHint(Spatial.CullHint.Always);
        } else {
            try {
                preview.setImage(assetManager, image, true);
                preview.setCullHint(Spatial.CullHint.Inherit);
            } catch (Exception exception) {
                preview.setCullHint(Spatial.CullHint.Always);
                statusText.setText("Изображение не загружено: " + image);
            }
        }
    }

    private String findImage(SceneObjectData object) {
        SceneObjectData current = object;
        while (current != null) {
            if (current.getImagePaths() != null && !current.getImagePaths().isEmpty()) return current.getImagePaths().get(0);
            current = current.getParentId() == null ? null : objectsById.get(current.getParentId());
        }
        return null;
    }

    private void activateSelectedEvent() {
        SceneObjectData current = selectedId == null ? null : objectsById.get(selectedId);
        while (current != null) {
            for (EventData event : scene.getEvents()) {
                if (event.getOwnerObjectId().equals(current.getId())) {
                    animationRuntime.toggle(event.getId());
                    activeEventId = event.getId();
                    statusText.setText("Событие: " + event.getName());
                    return;
                }
            }
            current = current.getParentId() == null ? null : objectsById.get(current.getParentId());
        }
        statusText.setText("Для выбранного объекта событие не задано");
    }

    private void updateHighlight() {
        String id = hoveredId != null ? hoveredId : selectedId;
        Spatial spatial = id == null ? null : spatialsById.get(id);
        if (spatial == null || spatial.getWorldBound() == null) {
            highlight.setCullHint(Spatial.CullHint.Always);
            return;
        }
        var bounds = (com.jme3.bounding.BoundingBox) spatial.getWorldBound();
        highlight.setLocalTranslation(bounds.getCenter());
        highlight.setLocalScale(bounds.getXExtent() * 1.04f, bounds.getYExtent() * 1.04f, bounds.getZExtent() * 1.04f);
        highlight.getMaterial().setColor("Color", hoveredId != null ? ColorRGBA.Yellow : ColorRGBA.Cyan);
        highlight.setCullHint(Spatial.CullHint.Inherit);
    }

    private void saveScene() {
        try {
            for (var entry : spatialsById.entrySet()) {
                SceneObjectData object = objectsById.get(entry.getKey());
                Spatial spatial = entry.getValue();
                object.setTransform(new TransformData(Vector3Data.fromJme(spatial.getLocalTranslation()),
                        QuaternionData.fromJme(spatial.getLocalRotation()), Vector3Data.fromJme(spatial.getLocalScale())));
            }
            repository.save(sceneFile, scene);
            statusText.setText("Сцена сохранена: " + sceneFile.toAbsolutePath());
        } catch (Exception exception) {
            statusText.setText("Ошибка сохранения: " + exception.getMessage());
        }
    }

    private void resetCamera() {
        cam.setLocation(new Vector3f(4.2f, 3.1f, 5.2f));
        cam.lookAt(new Vector3f(0.2f, 0.35f, 0), Vector3f.UNIT_Y);
    }

    private static ColorRGBA parseColor(String hex) {
        try {
            String value = hex.startsWith("#") ? hex.substring(1) : hex;
            int rgb = Integer.parseInt(value, 16);
            return new ColorRGBA(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, 1);
        } catch (Exception exception) {
            return ColorRGBA.DarkGray;
        }
    }

    private static String wrap(String text, int width) {
        StringBuilder result = new StringBuilder();
        for (String paragraph : text.split("\\R", -1)) {
            String[] words = paragraph.split(" ");
            int line = 0;
            for (String word : words) {
                if (line > 0 && line + word.length() + 1 > width) {
                    result.append('\n');
                    line = 0;
                }
                if (line > 0) { result.append(' '); line++; }
                result.append(word);
                line += word.length();
            }
            result.append('\n');
        }
        return result.toString().trim();
    }
}
