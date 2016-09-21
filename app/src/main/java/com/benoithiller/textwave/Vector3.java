package com.benoithiller.textwave;

import android.support.annotation.NonNull;

/**
 * Simple 3D Vector math implementation.
 */
public class Vector3 {
    public static Vector3 i = new Vector3(1, 0, 0);
    public static Vector3 j = new Vector3(0, 1, 0);
    public static Vector3 k = new Vector3(0, 0, 1);

    public final float x;
    public final float y;
    public final float z;

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    public Vector3 sub(Vector3 other) {
        return add(other.mult(-1));
    }

    public Vector3 mult(float scalar) {
        return new Vector3(scalar * x, scalar * y, scalar * z);
    }

    public float dot(Vector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public double magnitude() {
        return Math.sqrt(dot(this));
    }

    /**
     * project this vector onto another vector
     *
     * @param other the vector to project onto
     */
    public Vector3 project(Vector3 other) {
        return other.mult(dot(other) / other.dot(other));
    }

    public double angle(Vector3 other) {
        return Math.acos(dot(other) / (magnitude() * other.magnitude()));
    }

    public Vector2 flatten() {
        return new Vector2(x, y);
    }
}
