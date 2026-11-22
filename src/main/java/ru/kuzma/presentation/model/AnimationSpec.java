package ru.kuzma.presentation.model;

/** Одна анимация внутри составного события. */
public class AnimationSpec {
    private String targetObjectId;
    private AnimationChannel channel = AnimationChannel.ROTATE_Y;
    private float amplitude;
    private float durationSec = 1;

    public AnimationSpec() { }

    public AnimationSpec(String targetObjectId, AnimationChannel channel, float amplitude, float durationSec) {
        this.targetObjectId = targetObjectId;
        this.channel = channel;
        this.amplitude = amplitude;
        this.durationSec = durationSec;
    }

    public String getTargetObjectId() { return targetObjectId; }
    public void setTargetObjectId(String targetObjectId) { this.targetObjectId = targetObjectId; }
    public AnimationChannel getChannel() { return channel; }
    public void setChannel(AnimationChannel channel) { this.channel = channel; }
    public float getAmplitude() { return amplitude; }
    public void setAmplitude(float amplitude) { this.amplitude = amplitude; }
    public float getDurationSec() { return durationSec; }
    public void setDurationSec(float durationSec) { this.durationSec = durationSec; }
}
