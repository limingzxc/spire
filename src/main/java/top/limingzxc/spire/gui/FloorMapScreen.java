package top.limingzxc.spire.gui;

import top.limingzxc.spire.SpireMod;
import top.limingzxc.spire.dungeon.DungeonManager;
import top.limingzxc.spire.dungeon.DungeonState;
import top.limingzxc.spire.dungeon.FloorMap;
import top.limingzxc.spire.dungeon.MapNode;
import top.limingzxc.spire.dungeon.RoomType;
import top.limingzxc.spire.network.SpireNetwork;
import moddedmite.rustedironcore.network.Network;
import net.minecraft.Minecraft;
import net.minecraft.GuiButton;
import net.minecraft.GuiScreen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 楼层地图 GUI。
 * 从 clientState.floorMap（服务端同步）读取节点，渲染路径图。
 * 点击可达节点发送 PacketSelectNode，后续屏幕由同步驱动。
 */
public class FloorMapScreen extends GuiScreen {

    private static final int NODE_SIZE = 24;
    private static final int NODE_PADDING = 8;
    private static final int MAP_TOP = 40;
    private static final int MAP_LEFT = 60;
    private static final int BTN_ABANDON_ID = 9001;

    private final DungeonState clientState;
    private final FloorMap floorMap;

    private final Map<Integer, int[]> nodeRects = new HashMap<>();

    public FloorMapScreen() {
        this.clientState = DungeonManager.getInstance().getClientState();
        this.floorMap = clientState.floorMap;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        nodeRects.clear();

        buttonList.add(new GuiButton(BTN_ABANDON_ID, width - 110, 8, 100, 20, "放弃 Run"));

        if (floorMap == null) return;

        List<Integer> sortedCols = floorMap.columnNodes.keySet().stream().sorted().toList();
        int colCount = sortedCols.size();

        for (int colIdx = 0; colIdx < colCount; colIdx++) {
            int col = sortedCols.get(colIdx);
            List<Integer> nodeIds = floorMap.columnNodes.get(col);
            int colX = MAP_LEFT + colIdx * (NODE_SIZE + NODE_PADDING * 4);

            for (int rowIdx = 0; rowIdx < nodeIds.size(); rowIdx++) {
                int nodeId = nodeIds.get(rowIdx);
                int rowY = MAP_TOP + rowIdx * (NODE_SIZE + NODE_PADDING * 2)
                    - (nodeIds.size() - 1) * (NODE_SIZE + NODE_PADDING * 2) / 2
                    + height / 4;

                nodeRects.put(nodeId, new int[]{colX, rowY, NODE_SIZE, NODE_SIZE});
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawCenteredString(fontRenderer,
            "Floor " + clientState.currentFloor + " — SpireCraft  (金币: " + clientState.gold + ")",
            width / 2, 10, 0xFFFFFF);

        if (floorMap == null) return;

        drawConnections();
        for (MapNode node : floorMap.nodes) {
            int[] rect = nodeRects.get(node.id);
            if (rect == null) continue;
            drawNode(node, rect[0], rect[1]);
        }
        drawNodeTooltip(mouseX, mouseY);
    }

    private void drawConnections() {
        for (Map.Entry<Integer, List<Integer>> entry : floorMap.connections.entrySet()) {
            int fromId = entry.getKey();
            int[] fromRect = nodeRects.get(fromId);
            if (fromRect == null) continue;

            int fromX = fromRect[0] + NODE_SIZE / 2;
            int fromY = fromRect[1] + NODE_SIZE / 2;

            for (int toId : entry.getValue()) {
                int[] toRect = nodeRects.get(toId);
                if (toRect == null) continue;

                int toX = toRect[0] + NODE_SIZE / 2;
                int toY = toRect[1] + NODE_SIZE / 2;

                MapNode fromNode = floorMap.getNode(fromId);
                MapNode toNode = floorMap.getNode(toId);
                int color = (fromNode != null && fromNode.completed && toNode != null && toNode.accessible)
                    ? 0x88AAFF88 : 0x66444444;

                drawHorizontalLine(fromX, toX, fromY, color);
                if (fromY != toY) {
                    drawVerticalLine(toX, fromY, toY, color);
                }
            }
        }
    }

    private void drawNode(MapNode node, int x, int y) {
        int color = getNodeColor(node);
        drawRect(x, y, x + NODE_SIZE, y + NODE_SIZE, color);

        if (node.accessible && !node.completed) {
            drawRect(x - 1, y - 1, x + NODE_SIZE + 1, y + NODE_SIZE + 1, 0xFFFFFF00);
        }
        if (node.completed) {
            drawString(fontRenderer, "√", x + 7, y + 5, 0xFFFFFF);
        }

        String icon = getNodeIcon(node.type);
        if (!node.completed) {
            drawString(fontRenderer, icon, x + 6, y + 4, 0xFFFFFF);
        }

        if (node.type == RoomType.BOSS) {
            drawString(fontRenderer, "B", x + 8, y + 5, 0xFF4444);
        }
    }

    private void drawNodeTooltip(int mouseX, int mouseY) {
        for (Map.Entry<Integer, int[]> entry : nodeRects.entrySet()) {
            int[] rect = entry.getValue();
            if (mouseX >= rect[0] && mouseX <= rect[0] + rect[2]
                && mouseY >= rect[1] && mouseY <= rect[1] + rect[3]) {
                MapNode node = floorMap.getNode(entry.getKey());
                if (node != null) {
                    String tip = node.type.name() + (node.accessible ? " [可进入]" : node.completed ? " [已完成]" : " [未解锁]");
                    drawString(fontRenderer, tip, mouseX + 12, mouseY - 12, 0xFFFFFFAA);
                }
                break;
            }
        }
    }

    private int getNodeColor(MapNode node) {
        if (node.completed) return 0x88333333;
        if (!node.accessible) return 0x88111111;

        return switch (node.type) {
            case START  -> 0x88666666;
            case COMBAT -> 0xDD884444;
            case ELITE  -> 0xDDAA4400;
            case BOSS   -> 0xDDBB0000;
            case REST   -> 0xDD448844;
            case EVENT  -> 0xDD4444BB;
            case SHOP   -> 0xDDAA8800;
            case TREASURE -> 0xDDAAAA44;
        };
    }

    private String getNodeIcon(RoomType type) {
        return switch (type) {
            case START  -> "S";
            case COMBAT -> "F";
            case ELITE  -> "E";
            case BOSS   -> "B";
            case REST   -> "R";
            case EVENT  -> "?";
            case SHOP   -> "$";
            case TREASURE -> "C";
        };
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BTN_ABANDON_ID) {
            Network.sendToServer(new SpireNetwork.PacketRoomAction("abandon", ""));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (button != 0 || floorMap == null) return;

        for (Map.Entry<Integer, int[]> entry : nodeRects.entrySet()) {
            int[] rect = entry.getValue();
            if (mouseX >= rect[0] && mouseX <= rect[0] + rect[2]
                && mouseY >= rect[1] && mouseY <= rect[1] + rect[3]) {
                int nodeId = entry.getKey();
                MapNode node = floorMap.getNode(nodeId);
                if (node != null && node.accessible && !node.completed) {
                    SpireMod.LOGGER.info("Player selected node {} (type={})", nodeId, node.type);
                    Network.sendToServer(new SpireNetwork.PacketSelectNode(nodeId));
                }
                return;
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
