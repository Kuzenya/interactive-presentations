package ru.kuzma.presentation.animation;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import ru.kuzma.presentation.model.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Применяет нормированный прогресс событий к локальным трансформациям Spatial. */
public final class AnimationRuntime {
    private final Map<String, ReversibleEvent> events = new LinkedHashMap<>();
    private final Map<String, Spatial> spatials;
    private final Map<String, Vector3f> baseLocations = new LinkedHashMap<>();
    private final Map<String, Quaternion> baseRotations = new LinkedHashMap<>();

    public AnimationRuntime(SceneData scene, Map<String, Spatial> spatials) {
        this.spatials = spatials;
        for (EventData event : scene.getEvents()) events.put(event.getId(), new ReversibleEvent(event));
        for (SceneObjectData object : scene.getObjects()) {
            Spatial spatial = spatials.get(object.getId());
            if (spatial != null) {
                baseLocations.put(object.getId(), spatial.getLocalTranslation().clone());
                baseRotations.put(object.getId(), spatial.getLocalRotation().clone());
            }
        }
    }

    public void toggle(String eventId) {
        ReversibleEvent event = events.get(eventId);
        if (event == null) throw new IllegalArgumentException("Неизвестное событие: " + eventId);
        event.toggle();
    }

    public void update(float deltaTimeSec) {
        for (ReversibleEvent event : events.values()) {
            if (!event.isRunning()) continue;
            event.advance(deltaTimeSec);
            for (AnimationSpec animation : event.getDefinition().getAnimations()) apply(animation, event.offset(animation));
        }
    }

    private void apply(AnimationSpec animation, float offset) {
        String id = animation.getTargetObjectId();
        Spatial spatial = spatials.get(id);
        if (spatial == null) return;
        switch (animation.getChannel()) {
            case ROTATE_X -> spatial.setLocalRotation(baseRotations.get(id).clone()
                    .mult(new Quaternion().fromAngleAxis(offset, Vector3f.UNIT_X)));
            case ROTATE_Y -> spatial.setLocalRotation(baseRotations.get(id).clone()
                    .mult(new Quaternion().fromAngleAxis(offset, Vector3f.UNIT_Y)));
            case TRANSLATE_Y -> spatial.setLocalTranslation(baseLocations.get(id).clone().add(0, offset, 0));
        }
    }

    public float progress(String eventId) { return events.get(eventId).getProgress(); }
}
