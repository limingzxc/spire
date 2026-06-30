package top.limingzxc.spire.gui;

import top.limingzxc.spire.dungeon.DungeonManager;
import top.limingzxc.spire.dungeon.DungeonState;
import top.limingzxc.spire.network.SpireNetwork;
import moddedmite.rustedironcore.network.Network;
import net.minecraft.GuiButton;
import net.minecraft.GuiScreen;

/**
 * 事件房间 GUI。
 * 从 clientState.pendingEventId 读取事件（服务端同步）。
 * 选项通过 PacketRoomAction 发送。
 */
public class EventScreen extends GuiScreen {

    private int selectedEventId;
    private String eventTitle;
    private String eventDescription;
    private String optionAText;
    private String optionBText;

    @Override
    public void initGui() {
        buttonList.clear();

        DungeonState state = DungeonManager.getInstance().getClientState();
        selectedEventId = state.pendingEventId;
        if (selectedEventId < 0) selectedEventId = 0;
        setupEvent(selectedEventId);

        int centerX = width / 2;
        buttonList.add(new GuiButton(0, centerX - 100, height / 2 + 20, 200, 20, optionAText));
        buttonList.add(new GuiButton(1, centerX - 100, height / 2 + 50, 200, 20, optionBText));
    }

    private void setupEvent(int eventId) {
        switch (eventId) {
            case 0:
                eventTitle = "幽灵商贩";
                eventDescription = "一个幽灵商人愿意用遗物换取你的生命力...";
                optionAText = "支付 30% 生命值 — 获得随机罕见遗物";
                optionBText = "离开";
                break;
            case 1:
                eventTitle = "元素祭坛";
                eventDescription = "古老的元素祭坛，献上祭品获得赐福。";
                optionAText = "献祭一件护甲 — 获得随机遗物";
                optionBText = "离开";
                break;
            case 2:
                eventTitle = "暗影契约";
                eventDescription = "与暗影达成交易，力量与代价并存。";
                optionAText = "获得强力遗物 — 扣除 4 点当前生命";
                optionBText = "离开";
                break;
            default:
                eventTitle = "未知事件";
                eventDescription = "...";
                optionAText = "继续";
                optionBText = "离开";
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawCenteredString(fontRenderer, eventTitle, width / 2, 15, 0xFFAA00);
        drawCenteredString(fontRenderer, eventDescription, width / 2, height / 2 - 40, 0xCCCCCC);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            Network.sendToServer(new SpireNetwork.PacketRoomAction("event_a", ""));
        } else if (button.id == 1) {
            Network.sendToServer(new SpireNetwork.PacketRoomAction("event_b", ""));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
