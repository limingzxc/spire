package top.limingzxc.spire.dungeon;

import java.util.*;

/**
 * 一次 run 的完整状态数据。
 * 通过 WorldInfo 持久化到 level.dat。
 */
public class DungeonState {
    /** 当前阶段 */
    public DungeonPhase phase = DungeonPhase.IDLE;
    /** 当前楼层 (1-3)，0 表示未开始 */
    public int currentFloor;
    /** 当前所在节点 ID，-1 表示不在任何房间中 */
    public int currentNodeId;
    /** 当前楼层地图 */
    public FloorMap floorMap;
    /** 玩家选择的下一节点 ID（地图中选中但尚未进入） */
    public int selectedNodeId = -1;
    /** 已收集遗物 ID 列表 */
    public List<String> relicIds = new ArrayList<>();
    /** 金币 */
    public int gold;
    /** run 是否活跃中 */
    public boolean isRunActive;
    /** 已完成节点数 */
    public int completedNodeCount;

    /** 商店待售遗物 ID 列表（进入 SHOP 阶段时生成） */
    public List<String> pendingShopRelics = new ArrayList<>();
    /** 奖励选择遗物 ID 列表（进入 REWARD 阶段时生成） */
    public List<String> pendingRewardRelics = new ArrayList<>();
    /** 宝箱已发放的遗物 ID（进入 TREASURE 阶段时服务端自动发放） */
    public String pendingTreasureRelic = null;
    /** 宝箱已发放的金币 */
    public int pendingTreasureGold;
    /** 当前事件 ID（进入 EVENT 阶段时随机选取） */
    public int pendingEventId = -1;

    public DungeonState() {
        reset();
    }

    /** 完全重置 */
    public void reset() {
        this.phase = DungeonPhase.IDLE;
        this.currentFloor = 0;
        this.currentNodeId = -1;
        this.selectedNodeId = -1;
        this.relicIds.clear();
        this.gold = 0;
        this.isRunActive = false;
        this.completedNodeCount = 0;
        this.floorMap = null;
        this.pendingShopRelics.clear();
        this.pendingRewardRelics.clear();
        this.pendingTreasureRelic = null;
        this.pendingTreasureGold = 0;
        this.pendingEventId = -1;
    }

    /** 开始新一轮 run */
    public void startNewRun() {
        reset();
        this.currentFloor = 1;
        this.gold = 99;
        this.isRunActive = true;
        this.phase = DungeonPhase.FLOOR_START;
    }

    /** 进入下一楼层 */
    public void advanceFloor() {
        this.currentFloor++;
        this.currentNodeId = -1;
        this.selectedNodeId = -1;
        this.floorMap = null;
        this.phase = DungeonPhase.FLOOR_START;
    }

    /** 判断是否已通关 */
    public boolean isVictory() {
        return currentFloor > 3 && phase == DungeonPhase.VICTORY;
    }
}