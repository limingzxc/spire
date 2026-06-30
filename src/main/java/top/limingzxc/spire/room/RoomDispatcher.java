package top.limingzxc.spire.room;

import top.limingzxc.spire.dungeon.MapNode;
import net.minecraft.EntityPlayer;

/**
 * 房间路由器。
 * 仅处理战斗类房间（COMBAT/ELITE/BOSS）的敌人生成。
 * 非战斗房间（REST/EVENT/SHOP/TREASURE）由 syncToClient 驱动 GUI，无需 enter。
 */
public class RoomDispatcher {

    public static void dispatch(EntityPlayer player, MapNode node) {
        switch (node.type) {
            case COMBAT:
            case ELITE:
            case BOSS:
                CombatRoom.enter(player, node);
                break;
            default:
                break;
        }
    }
}
