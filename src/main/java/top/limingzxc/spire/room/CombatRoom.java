package top.limingzxc.spire.room;

import top.limingzxc.spire.combat.CombatManager;
import top.limingzxc.spire.dungeon.DungeonManager;
import top.limingzxc.spire.dungeon.MapNode;
import net.minecraft.EntityPlayer;

/**
 * 战斗房间入口。
 * 由 RoomDispatcher 调用，触发 CombatManager.startCombat。
 */
public class CombatRoom {

    public static void enter(EntityPlayer player, MapNode node) {
        CombatManager.getInstance().startCombat(player, node);
    }
}