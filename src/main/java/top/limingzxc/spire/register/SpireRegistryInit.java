package top.limingzxc.spire.register;

import top.limingzxc.spire.SpireMod;
import huix.glacier.api.entrypoint.IGameRegistry;
import huix.glacier.api.registry.MinecraftRegistry;
import net.minecraft.Item;
import net.xiaoyu233.fml.reload.utils.IdUtil;

/**
 * 注册物品/方块/维度。
 * Phase 1: 注册传送门物品 (spire_portal)。
 */
public class SpireRegistryInit implements IGameRegistry {

    public static final MinecraftRegistry registry =
            new MinecraftRegistry(SpireMod.MOD_ID).initAutoItemRegister();

    /** 尖塔传送门物品 — 右键使用开始新 run */
    public static Item SPIRE_PORTAL;

    @Override
    public void onGameRegistry() {
        SpireMod.LOGGER.info("Registering SpireCraft items...");

        // Item 构造函数把 id 当作偏移量，实际 itemID = id + 256；用 itemsList.length 会越界。
        int portalId = IdUtil.getNextItemID();
        SPIRE_PORTAL = new ItemSpirePortal(portalId, SpireMod.MOD_ID, "spire_portal");
        registry.registerItem("spire_portal", "spirePortal", SPIRE_PORTAL);

        SpireMod.LOGGER.info("SpireCraft items registered.");
    }
}