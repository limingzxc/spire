package top.limingzxc.spire.gui;

import top.limingzxc.spire.dungeon.DungeonManager;
import top.limingzxc.spire.dungeon.DungeonState;
import top.limingzxc.spire.network.SpireNetwork;
import top.limingzxc.spire.relic.Relic;
import top.limingzxc.spire.relic.RelicRegistry;
import moddedmite.rustedironcore.network.Network;
import net.minecraft.GuiButton;
import net.minecraft.GuiScreen;

/**
 * 宝箱房间 GUI。
 * 从 clientState.pendingTreasureRelic/Gold 读取服务端已发放的奖励。
 * 点击"继续"通过 PacketRoomAction 通知服务端完成房间。
 */
public class TreasureScreen extends GuiScreen {

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 50, height / 2 + 60, 100, 20, "继续"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        DungeonState state = DungeonManager.getInstance().getClientState();
        drawCenteredString(fontRenderer, "宝箱", width / 2, 15, 0xFFAA00);

        int y = height / 2 - 20;
        if (state.pendingTreasureRelic != null) {
            Relic relic = RelicRegistry.getById(state.pendingTreasureRelic);
            if (relic != null) {
                drawCenteredString(fontRenderer, "获得遗物: " + relic.name, width / 2, y, 0xFFFFFF);
                drawCenteredString(fontRenderer, relic.description, width / 2, y + 20, 0xAAAAAA);
            }
        }
        if (state.pendingTreasureGold > 0) {
            drawCenteredString(fontRenderer, "金币 +" + state.pendingTreasureGold,
                width / 2, y + 40, 0xFFAA00);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            Network.sendToServer(new SpireNetwork.PacketRoomAction("treasure_continue", ""));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
