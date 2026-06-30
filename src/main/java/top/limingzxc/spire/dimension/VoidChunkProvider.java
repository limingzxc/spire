package top.limingzxc.spire.dimension;

import net.minecraft.Chunk;
import net.minecraft.ChunkPosition;
import net.minecraft.EnumCreatureType;
import net.minecraft.IChunkProvider;
import net.minecraft.IProgressUpdate;
import net.minecraft.World;

import java.util.ArrayList;
import java.util.List;

/**
 * 虚空区块生成器。
 * 生成完全为空的区块（全空气），用于尖塔维度的战斗平台。
 */
public class VoidChunkProvider implements IChunkProvider {

    private final World world;

    public VoidChunkProvider(World world) {
        this.world = world;
    }

    @Override
    public boolean chunkExists(int x, int z) {
        return true;
    }

    @Override
    public Chunk getChunkIfItExists(int x, int z) {
        return null;
    }

    @Override
    public Chunk provideChunk(int x, int z) {
        Chunk chunk = new Chunk(this.world, x, z);
        chunk.isTerrainPopulated = true;
        chunk.generateSkylightMap(true);
        return chunk;
    }

    @Override
    public Chunk loadChunk(int x, int z) {
        return provideChunk(x, z);
    }

    @Override
    public void populate(IChunkProvider provider, int x, int z) {
    }

    @Override
    public boolean saveChunks(boolean flag, IProgressUpdate progress) {
        return true;
    }

    @Override
    public boolean unloadQueuedChunks() {
        return false;
    }

    @Override
    public boolean canSave() {
        return true;
    }

    @Override
    public String makeString() {
        return "VoidSource";
    }

    @Override
    public List getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
        return new ArrayList();
    }

    @Override
    public ChunkPosition findClosestStructure(World world, String name, int x, int y, int z) {
        return null;
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }

    @Override
    public void recreateStructures(int x, int z) {
    }

    @Override
    public void saveExtraData() {
    }
}
