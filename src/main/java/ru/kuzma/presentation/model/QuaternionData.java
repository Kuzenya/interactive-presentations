package ru.kuzma.presentation.model;

import com.jme3.math.Quaternion;

/** Поворот в формате кватерниона glTF (x, y, z, w). */
public record QuaternionData(float x, float y, float z, float w) {
    public static QuaternionData identity() { return new QuaternionData(0, 0, 0, 1); }

    public QuaternionData {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z) || !Float.isFinite(w)) {
            throw new IllegalArgumentException("Компоненты поворота должны быть конечными числами");
        }
        if (x * x + y * y + z * z + w * w < 1e-12f) {
            throw new IllegalArgumentException("Нулевой кватернион недопустим");
        }
    }

    public Quaternion toJme() { return new Quaternion(x, y, z, w).normalizeLocal(); }
    public static QuaternionData fromJme(Quaternion value) {
        return new QuaternionData(value.getX(), value.getY(), value.getZ(), value.getW());
    }
}
