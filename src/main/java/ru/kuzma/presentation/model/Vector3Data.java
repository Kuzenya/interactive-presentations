package ru.kuzma.presentation.model;

import com.jme3.math.Vector3f;

/** Сериализуемый аналог Vector3f: в JSON не попадают служебные поля jMonkeyEngine. */
public record Vector3Data(float x, float y, float z) {
    public static Vector3Data zero() { return new Vector3Data(0, 0, 0); }
    public static Vector3Data one() { return new Vector3Data(1, 1, 1); }

    public Vector3Data {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("Координаты должны быть конечными числами");
        }
    }

    public Vector3f toJme() { return new Vector3f(x, y, z); }
    public static Vector3Data fromJme(Vector3f value) {
        return new Vector3Data(value.x, value.y, value.z);
    }
}
