package top.limingzxc.spire.dungeon;

/**
 * 房间类型枚举。
 * 表示杀戮尖塔地图上的节点类型。
 */
public enum RoomType {
    /** 起点节点（不可进入，仅作为路径起点） */
    START,
    /** 普通战斗 - 小怪 */
    COMBAT,
    /** 精英战斗 - 掉落遗物 */
    ELITE,
    /** 楼层Boss */
    BOSS,
    /** 休息 - 回血或锻造 */
    REST,
    /** 随机事件（问号房间） */
    EVENT,
    /** 商店 - 购买遗物/药水 */
    SHOP,
    /** 宝箱 - 随机遗物 */
    TREASURE;

    public boolean isCombat() {
        return this == COMBAT || this == ELITE || this == BOSS;
    }
}