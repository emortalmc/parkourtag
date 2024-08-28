package dev.emortal.minestom.parkourtag.physics.worldmesh;

import com.jme3.math.Vector3f;
import net.minestom.server.instance.block.BlockFace;

public record Face(BlockFace blockFace, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int blockX, int blockY, int blockZ) {

    public Face(BlockFace blockFace, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int blockX, int blockY, int blockZ) {
        this(blockFace, (float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, blockX, blockY, blockZ);
    }

    public boolean isEdge() {
        return switch (blockFace) {
            case BOTTOM -> minY == 0.0;
            case TOP -> maxY == 1.0;
            case NORTH -> minZ == 0.0;
            case SOUTH -> maxZ == 1.0;
            case WEST -> minX == 0.0;
            case EAST -> maxX == 1.0;
        };
    }

    public Quad toQuad() {
        return switch (blockFace) {
            case TOP, BOTTOM -> new Quad(
                    new Vector3f(minX + blockX, maxY + blockY, minZ + blockZ),
                    new Vector3f(maxX + blockX, maxY + blockY, minZ + blockZ),
                    new Vector3f(maxX + blockX, maxY + blockY, maxZ + blockZ),
                    new Vector3f(minX + blockX, maxY + blockY, maxZ + blockZ)
            );
            case EAST, WEST -> new Quad(
                    new Vector3f(maxX + blockX, minY + blockY, minZ + blockZ),
                    new Vector3f(maxX + blockX, maxY + blockY, minZ + blockZ),
                    new Vector3f(maxX + blockX, maxY + blockY, maxZ + blockZ),
                    new Vector3f(maxX + blockX, minY + blockY, maxZ + blockZ)
            );
            case NORTH, SOUTH -> new Quad(
                    new Vector3f(minX + blockX, minY + blockY, minZ + blockZ),
                    new Vector3f(maxX + blockX, minY + blockY, minZ + blockZ),
                    new Vector3f(maxX + blockX, maxY + blockY, minZ + blockZ),
                    new Vector3f(minX + blockX, maxY + blockY, minZ + blockZ)
            );
        };
    }
}