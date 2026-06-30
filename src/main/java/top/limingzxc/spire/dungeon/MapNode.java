package top.limingzxc.spire.dungeon;

/**
 * 楼层地图上的一个节点。
 * 每个节点表示一个房间，节点之间有路径连接。
 */
public class MapNode {
    public final int id;
    public final RoomType type;
    public final int x;           // 地图列坐标 (0=起点列, 逐列递增)
    public final int y;           // 地图行坐标 (同列内排序)
    public boolean completed;      // 已完成
    public boolean accessible;     // 当前可达（玩家已解锁该节点路径）

    public MapNode(int id, RoomType type, int x, int y) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.completed = false;
        this.accessible = false;
    }
}