package top.limingzxc.spire.gui;

import top.limingzxc.spire.network.SpireNetwork;
import moddedmite.rustedironcore.network.Network;
import net.minecraft.GuiButton;
import net.minecraft.GuiScreen;

/**
 * 休息房间 GUI。
 * 选项：休息（恢复生命）或 锻造（打开 ForgeScreen）。
 * 状态变更通过 PacketRoomAction 发送到服务端。
 */
public class RestScreen extends GuiScreen {

    @Override
    public void initGui() {
        buttonList.clear();
        int centerX = width / 2;
        int y = height / 2 - 30;

        buttonList.add(new GuiButton(0, centerX - 100, y, 200, 20, "休息 — 恢复 30% 生命值"));
        buttonList.add(new GuiButton(1, centerX - 100, y + 30, 200, 20, "锻造 — 为武器添加词条"));
        buttonList.add(new GuiButton(2, centerX - 100, y + 70, 200, 20, "离开"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawCenteredString(fontRenderer, "休息营地", width / 2, 15, 0xFFFFFF);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                Network.sendToServer(new SpireNetwork.PacketRoomAction("rest_heal", ""));
                break;
            case 1:
                mc.displayGuiScreen(new ForgeScreen());
                break;
            case 2:
                Network.sendToServer(new SpireNetwork.PacketRoomAction("leave_rest", ""));
                break;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
