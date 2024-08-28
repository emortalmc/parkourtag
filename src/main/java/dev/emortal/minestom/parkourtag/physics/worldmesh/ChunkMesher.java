package dev.emortal.minestom.parkourtag.physics.worldmesh;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.collision.shapes.infos.IndexedMesh;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.Shape;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChunkMesher {

    private static final BlockFace[] BLOCK_FACES = BlockFace.values();

    public static @Nullable PhysicsCollisionObject createChunk(Chunk chunk) {
        int minY = MinecraftServer.getDimensionTypeRegistry().get(chunk.getInstance().getDimensionType()).minY();
        int maxY = MinecraftServer.getDimensionTypeRegistry().get(chunk.getInstance().getDimensionType()).maxY();

        return generateChunkCollisionObject(chunk, minY, maxY);
    }

    private static @Nullable PhysicsCollisionObject generateChunkCollisionObject(Chunk chunk, int minY, int maxY) {
        List<Vector3f> vertices = new ArrayList<>();

        List<Quad> faces = getChunkFaces(chunk, minY, maxY);
        for (Quad face : faces) {
            for (Triangle triangle : face.triangles()) {
                vertices.add(triangle.get1());
                vertices.add(triangle.get2());
                vertices.add(triangle.get3());
            }
        }

        if (vertices.isEmpty()) return null;

        Vector3f[] array = vertices.toArray(new Vector3f[0]);

        int[] indicesArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            indicesArray[i] = i;
        }

        var indexedMesh = new IndexedMesh(array, indicesArray);
        var shape = new MeshCollisionShape(true, indexedMesh);

        return new PhysicsRigidBody(shape, PhysicsBody.massForStatic);
    }

    private static List<Quad> getChunkFaces(Chunk chunk, int minY, int maxY) {
        int bottomY = maxY;
        int topY = minY;

        // Get min and max of current chunk sections to avoid computing on air
        List<Section> sections = chunk.getSections();
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            if (isEmpty(section)) continue;
            int chunkBottom = minY + i * Chunk.CHUNK_SECTION_SIZE;
            int chunkTop = chunkBottom + Chunk.CHUNK_SECTION_SIZE;

            if (bottomY > chunkBottom) {
                bottomY = chunkBottom;
            }
            if (topY < chunkTop) {
                topY = chunkTop;
            }
        }


        List<Quad> finalFaces = new ArrayList<>();

        for (int y = bottomY; y < topY; y++) {
            for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {
                    List<Quad> faces = getQuads(chunk, x, y, z);
                    if (faces == null) continue;
                    finalFaces.addAll(faces);
                }
            }
        }

        return finalFaces;
    }

    private static @Nullable List<Quad> getQuads(Chunk chunk, int x, int y, int z){
        Block block = chunk.getBlock(x, y, z, Block.Getter.Condition.TYPE);

        if (block.isAir() || block.isLiquid()) return null;
        List<Quad> quads = new ArrayList<>();

        Shape shape = block.registry().collisionShape();
        Point relStart = shape.relativeStart();
        Point relEnd = shape.relativeEnd();

        var blockX = chunk.getChunkX() * Chunk.CHUNK_SIZE_X + x;
        var blockZ = chunk.getChunkZ() * Chunk.CHUNK_SIZE_Z + z;

        for (BlockFace blockFace : BLOCK_FACES) {
            Face face = new Face(
                    blockFace,
                    blockFace == BlockFace.EAST ? relEnd.x() : relStart.x(),
                    blockFace == BlockFace.TOP ? relEnd.y() : relStart.y(),
                    blockFace == BlockFace.SOUTH ? relEnd.z() : relStart.z(),
                    blockFace == BlockFace.WEST ? relStart.x() : relEnd.x(),
                    blockFace == BlockFace.BOTTOM ? relStart.y() : relEnd.y(),
                    blockFace == BlockFace.NORTH ? relStart.z() : relEnd.z(),
                    blockX,
                    y,
                    blockZ
            );

            if (!face.isEdge()) { // If face isn't an edge, we don't need to check neighbours
                quads.add(face.toQuad());
                continue;
            }

            var dir = blockFace.toDirection();
            var neighbourBlock = chunk.getBlock(x + dir.normalX(), y + dir.normalY(), z + dir.normalZ(), Block.Getter.Condition.TYPE);

            if (!isFull(neighbourBlock)) {
                quads.add(face.toQuad());
            }
        }

        return quads;
    }

    private static boolean isFull(Block block) {
        if (block.isAir() || block.isLiquid()) return false;

        Shape shape = block.registry().collisionShape();
        Point relStart = shape.relativeStart();
        Point relEnd = shape.relativeEnd();

        return relStart.x() == 0.0 && relStart.y() == 0.0 && relStart.z() == 0.0 &&
                relEnd.x() == 1.0 && relEnd.y() == 1.0 && relEnd.z() == 1.0;
    }

    private static boolean isEmpty(Section section) {
        return section.blockPalette().count() == 0;
    }

}
