package ru.kuzma.presentation.animation;

import ru.kuzma.presentation.model.AnimationSpec;
import ru.kuzma.presentation.model.EventData;

/**
 * Нормированный прогресс p ∈ [0;1]. Смена направления не меняет p, поэтому скачка нет.
 * Подготовка O(k + t), обновление кадра O(c), где c — число каналов активного события.
 */
public final class ReversibleEvent {
    private final EventData definition;
    private final float totalDurationSec;
    private float progress;
    private int direction = 1;

    public ReversibleEvent(EventData definition) {
        if (definition == null || definition.getAnimations() == null || definition.getAnimations().isEmpty()) {
            throw new IllegalArgumentException("Событие должно содержать анимации");
        }
        this.definition = definition;
        float maximum = 0;
        for (AnimationSpec animation : definition.getAnimations()) {
            if (animation.getDurationSec() <= 0 || !Float.isFinite(animation.getDurationSec())) {
                throw new IllegalArgumentException("Длительность анимации должна быть конечной и > 0");
            }
            maximum = Math.max(maximum, animation.getDurationSec());
        }
        this.totalDurationSec = maximum;
    }

    public void toggle() {
        if (progress <= 0) direction = 1;
        else if (progress >= 1) direction = -1;
        else direction = -direction;
    }

    public float advance(float deltaTimeSec) {
        if (!Float.isFinite(deltaTimeSec) || deltaTimeSec < 0) {
            throw new IllegalArgumentException("deltaTimeSec должен быть конечным и >= 0");
        }
        progress = Math.max(0f, Math.min(1f, progress + direction * deltaTimeSec / totalDurationSec));
        return progress;
    }

    public float clipTime(AnimationSpec animation) { return progress * animation.getDurationSec(); }
    public float offset(AnimationSpec animation) { return animation.getAmplitude() * progress; }
    public boolean isRunning() { return direction > 0 ? progress < 1 : progress > 0; }
    public float getProgress() { return progress; }
    public int getDirection() { return direction; }
    public float getTotalDurationSec() { return totalDurationSec; }
    public EventData getDefinition() { return definition; }
}
