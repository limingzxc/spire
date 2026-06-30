package top.limingzxc.spire.dungeon;

/**
 * Dungeon 状态机阶段枚举。
 * 定义一次 run 中的所有阶段。
 */
public enum DungeonPhase {
    /** 没有运行中的 run */
    IDLE,
    /** 开始新楼层，正在生成地图 */
    FLOOR_START,
    /** 在地图界面，等待玩家选择路径节点 */
    MAP,
    /** 玩家在战斗房间中 */
    COMBAT,
    /** 玩家在休息房间（GUI） */
    REST,
    /** 玩家在事件房间（GUI） */
    EVENT,
    /** 玩家在商店房间（GUI） */
    SHOP,
    /** 玩家在宝箱房间（GUI） */
    TREASURE,
    /** 战斗胜利，显示奖励选择 GUI */
    REWARD,
    /** 楼层 Boss 战斗 */
    BOSS,
    /** 楼层通过，即将进入下一层 */
    FLOOR_COMPLETE,
    /** 3 层全部通关 */
    VICTORY,
    /** run 结束（死亡或放弃） */
    RUN_END;

    /** 是否为 GUI 模式房间（非真实战斗） */
    public boolean isGuiRoom() {
        return this == REST || this == EVENT || this == SHOP || this == TREASURE || this == REWARD;
    }

    /** 是否为战斗类房间 */
    public boolean isCombat() {
        return this == COMBAT || this == BOSS;
    }
}