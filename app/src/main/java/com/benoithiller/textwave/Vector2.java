package com.benoithiller.textwave;

/**
 * Simple 2D Vector math implementation.
 */
public class Vector2 {
    public static final Vector2 i = new Vector2(1, 0);
    public static final Vector2 j = new Vector2(0, 1);

    public final float x;
    public final float y;

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2 add(Vector2 other) {
        return new Vector2(x + other.x, y + other.y);
    }

    public Vector2 sub(Vector2 other) {
        return add(other.mult(-1));
    }

    public Vector2 mult(float scalar) {
        return new Vector2(scalar * x, scalar * y);
    }

    public float dot(Vector2 other) {
        return x * other.x + y * other.y;
    }

    public double magnitude() {
        return Math.sqrt(dot(this));
    }

    /**
     * project this vector onto another vector
     *
     * @param other the vector to project onto
     */
    public Vector2 project(Vector2 other) {
        return other.mult(dot(other) / other.dot(other));
    }

    /**
     * The angle with respect to the positive x axis
     *
     * @return angle value in the range (-π,π]
     */
    public double angle() {
        return Math.atan2(y, x);
    }

    /**
     * The number of degrees clockwise the other angle is from this one
     *
     * @param other the angle to compare ours to
     * @return angle value in the range [-π,π]
     */
    public double angle(Vector2 other) {
        double angle = angle() - other.angle();
        return ((angle + Math.PI) % (Math.PI * 2)) - Math.PI;
    }
}
