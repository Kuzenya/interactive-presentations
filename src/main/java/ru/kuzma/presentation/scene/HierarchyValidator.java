package ru.kuzma.presentation.scene;

import ru.kuzma.presentation.model.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Проверка ссылочной целостности и ацикличности за O(n + e). */
public final class HierarchyValidator {

    public void validate(SceneData scene) {
        if (scene == null) throw new SceneValidationException("Сцена не задана");
        if (scene.getSchemaVersion() != 1) {
            throw new SceneValidationException("Поддерживается только schemaVersion=1");
        }
        if (blank(scene.getId()) || blank(scene.getName())) {
            throw new SceneValidationException("У сцены должны быть id и name");
        }

        Map<String, SceneObjectData> objects = new HashMap<>();
        for (SceneObjectData object : scene.getObjects()) {
            if (object == null || blank(object.getId()) || blank(object.getName())) {
                throw new SceneValidationException("Каждый объект должен содержать id и name");
            }
            if (objects.putIfAbsent(object.getId(), object) != null) {
                throw new SceneValidationException("Повторяющийся id объекта: " + object.getId());
            }
            if (object.getTransform() == null || object.getObjectType() == null) {
                throw new SceneValidationException("Неполные данные объекта: " + object.getId());
            }
        }

        for (SceneObjectData object : scene.getObjects()) {
            String parentId = object.getParentId();
            if (parentId != null) {
                if (parentId.equals(object.getId())) {
                    throw new SceneValidationException("Объект не может быть родителем самого себя: " + parentId);
                }
                if (!objects.containsKey(parentId)) {
                    throw new SceneValidationException("Не найден родитель " + parentId + " объекта " + object.getId());
                }
            }
        }
        detectCycles(scene, objects);

        Set<String> modelIds = new HashSet<>();
        for (ModelResourceData model : scene.getModels()) {
            if (model == null || blank(model.getId()) || blank(model.getFilePath()) || !modelIds.add(model.getId())) {
                throw new SceneValidationException("Некорректный или повторяющийся ресурс модели");
            }
            if (!objects.containsKey(model.getRootObjectId())) {
                throw new SceneValidationException("Не найден корневой объект модели: " + model.getRootObjectId());
            }
        }

        Set<String> eventIds = new HashSet<>();
        for (EventData event : scene.getEvents()) {
            if (event == null || blank(event.getId()) || !eventIds.add(event.getId())) {
                throw new SceneValidationException("Некорректный или повторяющийся id события");
            }
            if (!objects.containsKey(event.getOwnerObjectId())) {
                throw new SceneValidationException("Не найден владелец события: " + event.getId());
            }
            if (event.getAnimations() == null || event.getAnimations().isEmpty()) {
                throw new SceneValidationException("Событие должно содержать хотя бы одну анимацию: " + event.getId());
            }
            for (AnimationSpec animation : event.getAnimations()) {
                if (!objects.containsKey(animation.getTargetObjectId()) || animation.getChannel() == null) {
                    throw new SceneValidationException("Некорректная цель или канал анимации: " + event.getId());
                }
                if (!Float.isFinite(animation.getDurationSec()) || animation.getDurationSec() <= 0
                        || !Float.isFinite(animation.getAmplitude())) {
                    throw new SceneValidationException("Длительность должна быть > 0, амплитуда — конечным числом");
                }
            }
        }
    }

    private void detectCycles(SceneData scene, Map<String, SceneObjectData> objects) {
        Map<String, Integer> state = new HashMap<>();
        for (SceneObjectData object : scene.getObjects()) {
            visit(object.getId(), objects, state);
        }
    }

    private void visit(String id, Map<String, SceneObjectData> objects, Map<String, Integer> state) {
        int current = state.getOrDefault(id, 0);
        if (current == 2) return;
        if (current == 1) throw new SceneValidationException("Обнаружен цикл иерархии у объекта " + id);
        state.put(id, 1);
        String parentId = objects.get(id).getParentId();
        if (parentId != null) visit(parentId, objects, state);
        state.put(id, 2);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
