package com.legostudio.model;

import org.joml.Vector3i;

/**
 * Represents a single Lego brick placed in the world.
 * Position is in grid coordinates (studs for X/Z, plates for Y).
 */
public final class Brick {
    private final BrickType type;
    private final BrickColor color;
    private final Vector3i position; // Grid position
    private int rotation; // 0, 90, 180, 270 degrees

    public Brick(BrickType type, BrickColor color, int x, int y, int z) {
        this.type = type;
        this.color = color;
        this.position = new Vector3i(x, y, z);
        this.rotation = 0;
    }

    public Brick(BrickType type, BrickColor color, Vector3i position) {
        this(type, color, position.x, position.y, position.z);
    }

    public BrickType getType() { return type; }
    public BrickColor getColor() { return color; }
    public Vector3i getPosition() { return position; }
    public int getRotation() { return rotation; }

    public void setRotation(int degrees) {
        this.rotation = ((degrees % 360) + 360) % 360;
    }

    public void rotate90() {
        this.rotation = (this.rotation + 90) % 360;
    }

    /**
     * Get actual dimensions considering rotation.
     */
    public int getActualWidth() {
        return (rotation == 90 || rotation == 270) ? type.getLength() : type.getWidth();
    }

    public int getActualLength() {
        return (rotation == 90 || rotation == 270) ? type.getWidth() : type.getLength();
    }

    /**
     * Check if this brick occupies the given grid cell.
     */
    public boolean occupies(int x, int y, int z) {
        int w = getActualWidth();
        int l = getActualLength();
        int h = type.getHeight();

        return x >= position.x && x < position.x + w &&
               y >= position.y && y < position.y + h &&
               z >= position.z && z < position.z + l;
    }

    /**
     * Check if this brick collides with another brick.
     */
    public boolean collidesWith(Brick other) {
        int x1 = position.x, y1 = position.y, z1 = position.z;
        int w1 = getActualWidth(), h1 = type.getHeight(), l1 = getActualLength();

        int x2 = other.position.x, y2 = other.position.y, z2 = other.position.z;
        int w2 = other.getActualWidth(), h2 = other.type.getHeight(), l2 = other.getActualLength();

        return x1 < x2 + w2 && x1 + w1 > x2 &&
               y1 < y2 + h2 && y1 + h1 > y2 &&
               z1 < z2 + l2 && z1 + l1 > z2;
    }

    @Override
    public String toString() {
        return String.format("Brick[%s, %s, pos=(%d,%d,%d), rot=%d]",
                type, color, position.x, position.y, position.z, rotation);
    }
}
