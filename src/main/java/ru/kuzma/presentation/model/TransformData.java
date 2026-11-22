package ru.kuzma.presentation.model;

/** Локальные трансформации узла относительно родителя. */
public record TransformData(Vector3Data location, QuaternionData rotation, Vector3Data scale) {
    public TransformData {
        location = location == null ? Vector3Data.zero() : location;
        rotation = rotation == null ? QuaternionData.identity() : rotation;
        scale = scale == null ? Vector3Data.one() : scale;
        if (Math.abs(scale.x()) < 1e-8f || Math.abs(scale.y()) < 1e-8f || Math.abs(scale.z()) < 1e-8f) {
            throw new IllegalArgumentException("Нулевой масштаб недопустим");
        }
    }

    public static TransformData identity() {
        return new TransformData(Vector3Data.zero(), QuaternionData.identity(), Vector3Data.one());
    }
}
