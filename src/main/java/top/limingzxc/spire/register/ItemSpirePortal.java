package top.limingzxc.spire.register;

import top.limingzxc.spire.SpireMod;
import top.limingzxc.spire.dimension.SpireDimensionProvider;
import top.limingzxc.spire.dungeon.DungeonManager;
import net.minecraft.CreativeTabs;
import net.minecraft.EntityPlayer;
import net.minecraft.Item;

/**
 * 尖塔传送门物品。
 * 右键使用：开始一次新的爬塔 run。
 */
public class ItemSpirePortal extends Item {

    public ItemSpirePortal(int id, String modId, String itemName) {
        super(id, modId + ":" + itemName);
        setUnlocalizedName(itemName);
        setCreativeTab(CreativeTabs.tabMisc);
    }

    // MITE 1.6.4 签名: onItemRightClick(EntityPlayer, float, boolean) -> boolean
    // 旧代码用了 1.7+ 的签名 (ItemStack, World, EntityPlayer) -> ItemStack，根本不是 override，引擎从没调到过。
    @Override
    public boolean onItemRightClick(EntityPlayer player, float partialTick, boolean isServer) {
        // 仅服务端执行传送；客户端直接返回 true 拦截其他右键行为（如吞食）。
        if (player.worldObj.isRemote) {
            return true;
        }

        DungeonManager dm = DungeonManager.getInstance();
        if (dm.isRunActive(player)) {
            SpireMod.LOGGER.info("Player {} already in a run.", player.getEntityName());
            return true;
        }

        if (SpireDimensionProvider.SPIRE_DIMENSION != null
            && player.dimension == SpireDimensionProvider.SPIRE_DIMENSION.id()) {
            SpireMod.LOGGER.warn("Player {} is already in spire dimension without active run; aborting.",
                player.getEntityName());
            return true;
        }

        dm.startNewRun(player);
        SpireMod.LOGGER.info("Player {} started a new SpireCraft run.", player.getEntityName());
        return true;
    }
}