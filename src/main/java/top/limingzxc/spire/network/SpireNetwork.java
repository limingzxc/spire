package top.limingzxc.spire.network;

import top.limingzxc.spire.SpireMod;
import top.limingzxc.spire.dungeon.DungeonManager;
import top.limingzxc.spire.dungeon.DungeonPhase;
import top.limingzxc.spire.dungeon.DungeonState;
import top.limingzxc.spire.dungeon.FloorMap;
import moddedmite.rustedironcore.network.Network;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import moddedmite.rustedironcore.network.PacketReader;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * SpireCraft 网络包框架。
 *
 * 信道定义:
 *   spire:sync_state  — 服务端→客户端，同步完整 run 状态
 *   spire:select_node — 客户端→服务端，选择地图节点
 *   spire:room_action — 客户端→服务端，房间内操作（休息/购买/事件/奖励/锻造等）
 */
public class SpireNetwork {

    private static final String CHANNEL_SYNC_STATE = "spire:sync_state";
    private static final String CHANNEL_SELECT_NODE = "spire:select_node";
    private static final String CHANNEL_ROOM_ACTION = "spire:room_action";

    public static void register() {
        SpireMod.LOGGER.info("Registering SpireCraft network packets...");

        PacketReader.registerClientPacketReader(
            new ResourceLocation(CHANNEL_SYNC_STATE),
            PacketSyncState::new
        );

        PacketReader.registerServerPacketReader(
            new ResourceLocation(CHANNEL_SELECT_NODE),
            PacketSelectNode::new
        );

        PacketReader.registerServerPacketReader(
            new ResourceLocation(CHANNEL_ROOM_ACTION),
            PacketRoomAction::new
        );

        SpireMod.LOGGER.info("SpireCraft network packets registered.");
    }

    // ======================== PacketSyncState ========================

    /**
     * 服务端 -> 客户端：同步完整 DungeonState。
     * 客户端接收后更新 clientState 镜像，并根据 phase 切换 GUI 屏幕。
     */
    public static class PacketSyncState implements Packet {

        private int phaseOrdinal;
        private int currentFloor;
        private int currentNodeId;
        private int selectedNodeId;
        private int gold;
        private boolean isRunActive;
        private int completedNodeCount;
        private List<String> relicIds;
        private FloorMap floorMap;
        private List<String> pendingRewardRelics;
        private List<String> pendingShopRelics;
        private String pendingTreasureRelic;
        private int pendingTreasureGold;
        private int pendingEventId;

        public PacketSyncState() {}

        public PacketSyncState(DungeonState state) {
            this.phaseOrdinal = state.phase.ordinal();
            this.currentFloor = state.currentFloor;
            this.currentNodeId = state.currentNodeId;
            this.selectedNodeId = state.selectedNodeId;
            this.gold = state.gold;
            this.isRunActive = state.isRunActive;
            this.completedNodeCount = state.completedNodeCount;
            this.relicIds = new ArrayList<>(state.relicIds);
            this.floorMap = state.floorMap;
            this.pendingRewardRelics = new ArrayList<>(state.pendingRewardRelics);
            this.pendingShopRelics = new ArrayList<>(state.pendingShopRelics);
            this.pendingTreasureRelic = state.pendingTreasureRelic;
            this.pendingTreasureGold = state.pendingTreasureGold;
            this.pendingEventId = state.pendingEventId;
        }

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeInt(phaseOrdinal);
            buf.writeInt(currentFloor);
            buf.writeInt(currentNodeId);
            buf.writeInt(selectedNodeId);
            buf.writeInt(gold);
            buf.writeBoolean(isRunActive);
            buf.writeInt(completedNodeCount);

            writeStringList(buf, relicIds);

            buf.writeBoolean(floorMap != null);
            if (floorMap != null) {
                floorMap.writeToBuf(buf);
            }

            writeStringList(buf, pendingRewardRelics);
            writeStringList(buf, pendingShopRelics);

            buf.writeBoolean(pendingTreasureRelic != null);
            if (pendingTreasureRelic != null) {
                buf.writeString(pendingTreasureRelic);
            }
            buf.writeInt(pendingTreasureGold);
            buf.writeInt(pendingEventId);
        }

        public PacketSyncState(PacketByteBuf buf) {
            this.phaseOrdinal = buf.readInt();
            this.currentFloor = buf.readInt();
            this.currentNodeId = buf.readInt();
            this.selectedNodeId = buf.readInt();
            this.gold = buf.readInt();
            this.isRunActive = buf.readBoolean();
            this.completedNodeCount = buf.readInt();

            this.relicIds = readStringList(buf);

            if (buf.readBoolean()) {
                this.floorMap = FloorMap.readFromBuf(buf);
            }

            this.pendingRewardRelics = readStringList(buf);
            this.pendingShopRelics = readStringList(buf);

            if (buf.readBoolean()) {
                this.pendingTreasureRelic = buf.readString();
            }
            this.pendingTreasureGold = buf.readInt();
            this.pendingEventId = buf.readInt();
        }

        @Override
        public void apply(EntityPlayer player) {
            DungeonManager dm = DungeonManager.getInstance();
            DungeonState cs = dm.getClientState();

            DungeonPhase oldPhase = cs.phase;

            cs.phase = DungeonPhase.values()[phaseOrdinal];
            cs.currentFloor = currentFloor;
            cs.currentNodeId = currentNodeId;
            cs.selectedNodeId = selectedNodeId;
            cs.gold = gold;
            cs.isRunActive = isRunActive;
            cs.completedNodeCount = completedNodeCount;

            cs.relicIds.clear();
            cs.relicIds.addAll(relicIds);

            cs.floorMap = floorMap;

            cs.pendingRewardRelics.clear();
            cs.pendingRewardRelics.addAll(pendingRewardRelics);

            cs.pendingShopRelics.clear();
            cs.pendingShopRelics.addAll(pendingShopRelics);

            cs.pendingTreasureRelic = pendingTreasureRelic;
            cs.pendingTreasureGold = pendingTreasureGold;
            cs.pendingEventId = pendingEventId;

            dm.onSyncApplied(oldPhase, cs.phase);
        }

        @Override
        public ResourceLocation getChannel() {
            return new ResourceLocation(CHANNEL_SYNC_STATE);
        }
    }

    // ======================== PacketSelectNode ========================

    /**
     * 客户端 -> 服务端：玩家在地图上选择了某个节点。
     */
    public static class PacketSelectNode implements Packet {

        private int nodeId;

        public PacketSelectNode() {}

        public PacketSelectNode(int nodeId) {
            this.nodeId = nodeId;
        }

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeInt(nodeId);
        }

        @Override
        public void apply(EntityPlayer player) {
            DungeonManager.getInstance().selectNode(player, nodeId);
        }

        @Override
        public ResourceLocation getChannel() {
            return new ResourceLocation(CHANNEL_SELECT_NODE);
        }

        public PacketSelectNode(PacketByteBuf buf) {
            this.nodeId = buf.readInt();
        }
    }

    // ======================== PacketRoomAction ========================

    /**
     * 客户端 -> 服务端：房间内操作。
     * action 取值: rest_heal, leave_rest, forge, buy, leave_shop,
     *              event_a, event_b, treasure_continue, reward, victory_return
     * payload: relicId / affixId 等，无则空字符串
     */
    public static class PacketRoomAction implements Packet {

        private String action;
        private String payload;

        public PacketRoomAction() {}

        public PacketRoomAction(String action, String payload) {
            this.action = action;
            this.payload = payload == null ? "" : payload;
        }

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeString(action);
            buf.writeString(payload);
        }

        @Override
        public void apply(EntityPlayer player) {
            DungeonManager.getInstance().handleRoomAction(player, action, payload);
        }

        @Override
        public ResourceLocation getChannel() {
            return new ResourceLocation(CHANNEL_ROOM_ACTION);
        }

        public PacketRoomAction(PacketByteBuf buf) {
            this.action = buf.readString();
            this.payload = buf.readString();
        }
    }

    // ======================== 工具方法 ========================

    private static void writeStringList(PacketByteBuf buf, List<String> list) {
        buf.writeInt(list.size());
        for (String s : list) {
            buf.writeString(s);
        }
    }

    private static List<String> readStringList(PacketByteBuf buf) {
        int count = buf.readInt();
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(buf.readString());
        }
        return list;
    }
}
