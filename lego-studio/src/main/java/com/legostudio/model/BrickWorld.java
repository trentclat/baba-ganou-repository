package com.legostudio.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages all bricks in the world.
 * Uses spatial indexing for efficient collision detection.
 */
public class BrickWorld {
    private final List<Brick> bricks;
    private final int gridSize; // Size of the building area in studs

    public BrickWorld(int gridSize) {
        this.gridSize = gridSize;
        this.bricks = new ArrayList<>();
    }

    public int getGridSize() {
        return gridSize;
    }

    public List<Brick> getBricks() {
        return Collections.unmodifiableList(bricks);
    }

    /**
     * Attempt to add a brick to the world.
     * Returns true if successful, false if placement is invalid.
     */
    public boolean addBrick(Brick brick) {
        if (!isValidPlacement(brick)) {
            return false;
        }
        bricks.add(brick);
        return true;
    }

    /**
     * Remove a brick from the world.
     */
    public boolean removeBrick(Brick brick) {
        return bricks.remove(brick);
    }

    /**
     * Remove the brick at the specified position.
     */
    public Brick removeBrickAt(int x, int y, int z) {
        Brick found = getBrickAt(x, y, z);
        if (found != null) {
            bricks.remove(found);
        }
        return found;
    }

    /**
     * Get the brick at the specified grid position.
     */
    public Brick getBrickAt(int x, int y, int z) {
        for (Brick brick : bricks) {
            if (brick.occupies(x, y, z)) {
                return brick;
            }
        }
        return null;
    }

    /**
     * Check if a brick placement is valid (within bounds and no collisions).
     */
    public boolean isValidPlacement(Brick brick) {
        // Check bounds
        int x = brick.getPosition().x;
        int y = brick.getPosition().y;
        int z = brick.getPosition().z;
        int w = brick.getActualWidth();
        int l = brick.getActualLength();

        if (x < 0 || x + w > gridSize ||
            y < 0 ||
            z < 0 || z + l > gridSize) {
            return false;
        }

        // Check collisions with existing bricks
        for (Brick existing : bricks) {
            if (brick.collidesWith(existing)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Clear all bricks from the world.
     */
    public void clear() {
        bricks.clear();
    }

    /**
     * Get total brick count.
     */
    public int getBrickCount() {
        return bricks.size();
    }
}
