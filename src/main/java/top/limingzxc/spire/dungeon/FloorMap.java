package top.limingzxc.spire.dungeon;

import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagList;
import top.limingzxc.spire.SpireMod;

import java.util.*;

/**
 * 楼层路径图。
 * 生成随机分支-汇聚型有向无环图。
 * 结构：起点列 → N 个中间列 → Boss 列
 */
public class FloorMap {

    public final int floorNumber;
    public final List<MapNode> nodes;
    public final Map<Integer, List<Integer>> connections;
    public final Map<Integer, List<Integer>> columnNodes;
    public int totalColumns;
    public int startNodeId;
    public int bossNodeId;

    private int nextId = 0;

    public FloorMap(int floorNumber) {
        this(floorNumber, new Random());
    }

    public FloorMap(int floorNumber, Random random) {
        this.floorNumber = floorNumber;
        this.nodes = new ArrayList<>();
        this.connections = new HashMap<>();
        this.columnNodes = new LinkedHashMap<>();

        int middleColumns = 5 + (floorNumber - 1) * 2;
        this.totalColumns = middleColumns + 2;

        MapNode start = addNode(RoomType.START, 0, 0);
        start.accessible = false;
        this.startNodeId = start.id;
        this.connections.put(start.id, new ArrayList<>());

        for (int col = 1; col <= middleColumns; col++) {
            int nodeCount = 2 + random.nextInt(3);
            for (int row = 0; row < nodeCount; row++) {
                RoomType type = randomRoomType(random, col, middleColumns);
                addNode(type, col, row);
            }
        }

        MapNode boss = addNode(RoomType.BOSS, middleColumns + 1, 0);
        this.bossNodeId = boss.id;
        this.connections.put(boss.id, new ArrayList<>());

        buildConnections(random);

        // Unlock nodes connected to start (start itself is not enterable)
        List<Integer> startTargets = connections.get(start.id);
        if (startTargets != null) {
            for (int nextId : startTargets) {
                MapNode next = getNode(nextId);
                if (next != null) next.accessible = true;
            }
        }

        SpireMod.LOGGER.info(
            "Generated FloorMap: floor={}, columns={}, nodes={}",
            floorNumber, totalColumns, nodes.size()
        );
    }

    private MapNode addNode(RoomType type, int col, int row) {
        MapNode node = new MapNode(nextId, type, col, row);
        nodes.add(node);
        connections.put(node.id, new ArrayList<>());
        columnNodes.computeIfAbsent(col, k -> new ArrayList<>()).add(node.id);
        nextId++;
        return node;
    }

    private RoomType randomRoomType(Random random, int col, int maxMiddleCol) {
        int roll = random.nextInt(100);
        if (roll < 60) return RoomType.COMBAT;
        if (roll < 75) return RoomType.ELITE;
        if (roll < 83) return RoomType.REST;
        if (roll < 91) return RoomType.EVENT;
        if (roll < 96) return RoomType.SHOP;
        return RoomType.TREASURE;
    }

    private void buildConnections(Random random) {
        List<Integer> sortedCols = new ArrayList<>(columnNodes.keySet());
        Collections.sort(sortedCols);

        for (int i = 0; i < sortedCols.size() - 1; i++) {
            int fromCol = sortedCols.get(i);
            int toCol = sortedCols.get(i + 1);
            List<Integer> fromNodes = columnNodes.get(fromCol);
            List<Integer> toNodes = columnNodes.get(toCol);

            for (int fromId : fromNodes) {
                int connectCount = 1 + random.nextInt(Math.min(3, toNodes.size()));
                Set<Integer> selected = new HashSet<>();
                while (selected.size() < connectCount) {
                    selected.add(toNodes.get(random.nextInt(toNodes.size())));
                }
                connections.get(fromId).addAll(selected);
            }

            for (int toId : toNodes) {
                boolean hasIncoming = false;
                for (int fromId : fromNodes) {
                    if (connections.get(fromId).contains(toId)) {
                        hasIncoming = true;
                        break;
                    }
                }
                if (!hasIncoming) {
                    int fromId = fromNodes.get(random.nextInt(fromNodes.size()));
                    connections.get(fromId).add(toId);
                }
            }
        }
    }

    public void unlockNextNodes(int nodeId) {
        MapNode completed = getNode(nodeId);
        if (completed != null) {
            completed.completed = true;
        }
        List<Integer> nextIds = connections.get(nodeId);
        if (nextIds != null) {
            for (int nextId : nextIds) {
                MapNode next = getNode(nextId);
                if (next != null) {
                    next.accessible = true;
                }
            }
        }
    }

    public MapNode getNode(int nodeId) {
        for (MapNode node : nodes) {
            if (node.id == nodeId) return node;
        }
        return null;
    }

    public List<MapNode> getAccessibleNodes() {
        List<MapNode> result = new ArrayList<>();
        for (MapNode node : nodes) {
            if (node.accessible && !node.completed) {
                result.add(node);
            }
        }
        return result;
    }

    // ======================== 网络序列化 ========================

    public void writeToBuf(PacketByteBuf buf) {
        buf.writeInt(floorNumber);
        buf.writeInt(totalColumns);
        buf.writeInt(startNodeId);
        buf.writeInt(bossNodeId);

        buf.writeInt(nodes.size());
        for (MapNode node : nodes) {
            buf.writeInt(node.id);
            buf.writeInt(node.type.ordinal());
            buf.writeInt(node.x);
            buf.writeInt(node.y);
            buf.writeBoolean(node.completed);
            buf.writeBoolean(node.accessible);
        }

        buf.writeInt(connections.size());
        for (Map.Entry<Integer, List<Integer>> entry : connections.entrySet()) {
            buf.writeInt(entry.getKey());
            buf.writeInt(entry.getValue().size());
            for (int toId : entry.getValue()) {
                buf.writeInt(toId);
            }
        }

        buf.writeInt(columnNodes.size());
        for (Map.Entry<Integer, List<Integer>> entry : columnNodes.entrySet()) {
            buf.writeInt(entry.getKey());
            buf.writeInt(entry.getValue().size());
            for (int id : entry.getValue()) {
                buf.writeInt(id);
            }
        }
    }

    public static FloorMap readFromBuf(PacketByteBuf buf) {
        int floorNumber = buf.readInt();
        FloorMap map = new FloorMap(floorNumber, new Random(0));
        map.totalColumns = buf.readInt();
        map.startNodeId = buf.readInt();
        map.bossNodeId = buf.readInt();

        map.nodes.clear();
        int nodeCount = buf.readInt();
        for (int i = 0; i < nodeCount; i++) {
            int id = buf.readInt();
            RoomType type = RoomType.values()[buf.readInt()];
            int x = buf.readInt();
            int y = buf.readInt();
            boolean completed = buf.readBoolean();
            boolean accessible = buf.readBoolean();
            MapNode node = new MapNode(id, type, x, y);
            node.completed = completed;
            node.accessible = accessible;
            map.nodes.add(node);
        }

        map.connections.clear();
        int connCount = buf.readInt();
        for (int i = 0; i < connCount; i++) {
            int fromId = buf.readInt();
            int toCount = buf.readInt();
            List<Integer> toIds = new ArrayList<>();
            for (int j = 0; j < toCount; j++) {
                toIds.add(buf.readInt());
            }
            map.connections.put(fromId, toIds);
        }

        map.columnNodes.clear();
        int colCount = buf.readInt();
        for (int i = 0; i < colCount; i++) {
            int col = buf.readInt();
            int idCount = buf.readInt();
            List<Integer> ids = new ArrayList<>();
            for (int j = 0; j < idCount; j++) {
                ids.add(buf.readInt());
            }
            map.columnNodes.put(col, ids);
        }

        return map;
    }

    // ======================== NBT 持久化（level.dat） ========================

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("floor", floorNumber);
        tag.setInteger("totalCols", totalColumns);
        tag.setInteger("startId", startNodeId);
        tag.setInteger("bossId", bossNodeId);

        NBTTagList nodeList = new NBTTagList();
        for (MapNode node : nodes) {
            NBTTagCompound n = new NBTTagCompound();
            n.setInteger("id", node.id);
            n.setInteger("type", node.type.ordinal());
            n.setInteger("x", node.x);
            n.setInteger("y", node.y);
            n.setBoolean("completed", node.completed);
            n.setBoolean("accessible", node.accessible);
            nodeList.appendTag(n);
        }
        tag.setTag("nodes", nodeList);

        NBTTagList connList = new NBTTagList();
        for (Map.Entry<Integer, List<Integer>> entry : connections.entrySet()) {
            NBTTagCompound c = new NBTTagCompound();
            c.setInteger("from", entry.getKey());
            int[] toIds = entry.getValue().stream().mapToInt(Integer::intValue).toArray();
            c.setIntArray("to", toIds);
            connList.appendTag(c);
        }
        tag.setTag("connections", connList);

        NBTTagList colList = new NBTTagList();
        for (Map.Entry<Integer, List<Integer>> entry : columnNodes.entrySet()) {
            NBTTagCompound col = new NBTTagCompound();
            col.setInteger("col", entry.getKey());
            int[] ids = entry.getValue().stream().mapToInt(Integer::intValue).toArray();
            col.setIntArray("ids", ids);
            colList.appendTag(col);
        }
        tag.setTag("columns", colList);

        return tag;
    }

    public static FloorMap readFromNBT(NBTTagCompound tag) {
        int floorNumber = tag.getInteger("floor");
        FloorMap map = new FloorMap(floorNumber, new Random(0));
        map.totalColumns = tag.getInteger("totalCols");
        map.startNodeId = tag.getInteger("startId");
        map.bossNodeId = tag.getInteger("bossId");

        map.nodes.clear();
        NBTTagList nodeList = tag.getTagList("nodes");
        for (int i = 0; i < nodeList.tagCount(); i++) {
            NBTTagCompound n = (NBTTagCompound) nodeList.tagAt(i);
            int id = n.getInteger("id");
            RoomType type = RoomType.values()[n.getInteger("type")];
            int x = n.getInteger("x");
            int y = n.getInteger("y");
            MapNode node = new MapNode(id, type, x, y);
            node.completed = n.getBoolean("completed");
            node.accessible = n.getBoolean("accessible");
            map.nodes.add(node);
        }

        map.connections.clear();
        NBTTagList connList = tag.getTagList("connections");
        for (int i = 0; i < connList.tagCount(); i++) {
            NBTTagCompound c = (NBTTagCompound) connList.tagAt(i);
            int fromId = c.getInteger("from");
            int[] toIds = c.getIntArray("to");
            List<Integer> toList = new ArrayList<>();
            for (int toId : toIds) toList.add(toId);
            map.connections.put(fromId, toList);
        }

        map.columnNodes.clear();
        NBTTagList colList = tag.getTagList("columns");
        for (int i = 0; i < colList.tagCount(); i++) {
            NBTTagCompound col = (NBTTagCompound) colList.tagAt(i);
            int colKey = col.getInteger("col");
            int[] ids = col.getIntArray("ids");
            List<Integer> idList = new ArrayList<>();
            for (int id : ids) idList.add(id);
            map.columnNodes.put(colKey, idList);
        }

        return map;
    }
}