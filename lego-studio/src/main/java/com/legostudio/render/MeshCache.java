package com.legostudio.render;

import com.legostudio.model.BrickType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Caches brick meshes to avoid regenerating geometry.
 * Each brick type only needs one mesh regardless of how many instances exist.
 */
public class MeshCache {
    private final Map<BrickType, BrickMesh> meshes = new EnumMap<>(BrickType.class);

    public BrickMesh getMesh(BrickType type) {
        return meshes.computeIfAbsent(type, t ->
                new BrickMesh(t.getWidth(), t.getLength(), t.getHeight()));
    }

    public void cleanup() {
        meshes.values().forEach(BrickMesh::cleanup);
        meshes.clear();
    }
}
