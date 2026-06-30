package top.limingzxc.spire.gui;

import top.limingzxc.spire.dungeon.DungeonManager;
import top.limingzxc.spire.dungeon.DungeonState;
import top.limingzxc.spire.network.SpireNetwork;
import top.limingzxc.spire.relic.Relic;
import top.limingzxc.spire.relic.RelicRegistry;
import moddedmite.rustedironcore.network.Network;
import net.minecraft.GuiButton;
import net.minecraft.GuiScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗/精英/BOSS 胜利后的奖励选择 GUI。
 * 从 clientState.pendingRewardRelics 读取服务端生成的遗物选项。
 * 选择通过 PacketRoomAction 发送，服务端处理 returnToMap 流程。
 */
public class RewardScreen extends GuiScreen {

    private final int cardWidth = 160;
    private final int cardHeight = 60;
    private List<Relic> options = new ArrayList<>();

    public RewardScreen() {}

    @Override
    public void initGui() {
        buttonList.clear();
        refreshOptions();

        int startX = width / 2 - (options.size() * (cardWidth + 10)) / 2 + 5;
        int y = height / 2 - cardHeight / 2;

        for (int i = 0; i < options.size(); i++) {
            Relic relic = options.get(i);
            int x = startX + i * (cardWidth + 10);
            buttonList.add(new GuiButton(100 + i, x, y, cardWidth, cardHeight,
                relic.name + " (" + relic.rarity.name() + ")"));
        }
    }

    private void refreshOptions() {
        DungeonState state = DungeonManager.getInstance().getClientState();
        options.clear();
        for (String id : state.pendingRewardRelics) {
            Relic r = RelicRegistry.getById(id);
            if (r != null) options.add(r);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawCenteredString(fontRenderer, "选择奖励", width / 2, 15, 0xFFFFFF);

        for (int i = 0; i < options.size(); i++) {
            Relic relic = options.get(i);
            if (i < buttonList.size()) {
                GuiButton btn = (GuiButton) buttonList.get(i);
                drawCenteredString(fontRenderer, relic.description,
                    btn.xPosition + cardWidth / 2, btn.yPosition + cardHeight - 25, 0xAAAAAA);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        int index = button.id - 100;
        if (index < 0 || index >= options.size()) return;

        Relic chosen = options.get(index);
        Network.sendToServer(new SpireNetwork.PacketRoomAction("reward", chosen.relicId));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
