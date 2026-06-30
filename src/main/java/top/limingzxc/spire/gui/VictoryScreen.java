package top.limingzxc.spire.gui;

import top.limingzxc.spire.dungeon.DungeonManager;
import top.limingzxc.spire.dungeon.DungeonState;
import top.limingzxc.spire.network.SpireNetwork;
import moddedmite.rustedironcore.network.Network;
import net.minecraft.GuiButton;
import net.minecraft.GuiScreen;

/**
 * 通关画面 GUI。
 * 展示 run 统计数据：遗物数、金币数、房间完成数。
 * 点击"返回"通过 PacketRoomAction 通知服务端结束 run。
 */
public class VictoryScreen extends GuiScreen {

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 50, height / 2 + 60, 100, 20, "返回"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        DungeonState state = DungeonManager.getInstance().getClientState();

        drawCenteredString(fontRenderer, "尖塔征服！", width / 2, 30, 0xFFAA00);
        drawCenteredString(fontRenderer, "恭喜通关 SpireCraft", width / 2, 50, 0xFFFFFF);

        drawCenteredString(fontRenderer, "遗物收集: " + state.relicIds.size() + " 个",
            width / 2, height / 2 - 20, 0xCCCCCC);
        drawCenteredString(fontRenderer, "金币总数: " + state.gold,
            width / 2, height / 2, 0xCCCCCC);
        drawCenteredString(fontRenderer, "完成房间: " + state.completedNodeCount + " 个",
            width / 2, height / 2 + 20, 0xCCCCCC);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            Network.sendToServer(new SpireNetwork.PacketRoomAction("victory_return", ""));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
