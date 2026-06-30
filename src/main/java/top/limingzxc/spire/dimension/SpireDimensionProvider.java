package top.limingzxc.spire.dimension;

import moddedmite.rustedironcore.api.world.Dimension;
import net.minecraft.ChunkCoordinates;
import net.minecraft.IChunkProvider;
import net.minecraft.WorldProvider;

/**
 * 尖塔竞技场维度。
 * 纯虚空 + 战斗平台，无日夜循环、无天气、无自然生物生成。
 */
public class SpireDimensionProvider extends WorldProvider {

    /** 注册后填充，用于传送查询 */
    public static Dimension SPIRE_DIMENSION = null;

    /** 尖塔出生点（与 DungeonManager.SPAWN_* 对齐） */
    public static final int SPAWN_X = 0;
    public static final int SPAWN_Y = 65;
    public static final int SPAWN_Z = 0;

    public SpireDimensionProvider(int dimensionId, String dimensionName) {
        super(dimensionId, dimensionName);
    }

    @Override
    public IChunkProvider createChunkGenerator() {
        return new VoidChunkProvider(this.worldObj);
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    /** 1.6.4 transferEntityToWorld 对自定义维度会调用此方法获取落点；
     *  父类默认返回 null 导致 NPE 崩溃。返回固定出生点修复崩溃。 */
    @Override
    public ChunkCoordinates getEntrancePortalLocation() {
        return new ChunkCoordinates(SPAWN_X, SPAWN_Y, SPAWN_Z);
    }
}
