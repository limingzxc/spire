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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 商店 GUI。
 * 从 clientState.pendingShopRelics 读取待售遗物（服务端同步）。
 * 购买/离开通过 PacketRoomAction 发送。
 */
public class ShopScreen extends GuiScreen {

    private final int cardWidth = 160;
    private final int cardHeight = 60;
    private List<Relic> shopItems = new ArrayList<>();
    private final Set<String> purchased = new HashSet<>();

    @Override
    public void initGui() {
        buttonList.clear();
        refreshShopItems();

        int startX = width / 2 - (shopItems.size() * (cardWidth + 10)) / 2 + 5;
        int y = height / 2 - cardHeight / 2;

        for (int i = 0; i < shopItems.size(); i++) {
            Relic relic = shopItems.get(i);
            int x = startX + i * (cardWidth + 10);
            String label = purchased.contains(relic.relicId)
                ? "[已售罄]"
                : relic.name + " (" + relic.getGoldValue() + "金)";
            buttonList.add(new GuiButton(100 + i, x, y, cardWidth, cardHeight, label));
        }

        buttonList.add(new GuiButton(10, width / 2 - 50, height - 40, 100, 20, "离开"));
    }

    private void refreshShopItems() {
        DungeonState state = DungeonManager.getInstance().getClientState();
        shopItems.clear();
        for (String id : state.pendingShopRelics) {
            Relic r = RelicRegistry.getById(id);
            if (r != null) shopItems.add(r);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        DungeonState state = DungeonManager.getInstance().getClientState();
        drawCenteredString(fontRenderer, "商店 — 金币: " + state.gold, width / 2, 15, 0xFFAA00);

        for (int i = 0; i < shopItems.size(); i++) {
            Relic relic = shopItems.get(i);
            if (!purchased.contains(relic.relicId) && i < buttonList.size()) {
                GuiButton btn = (GuiButton) buttonList.get(i);
                drawCenteredString(fontRenderer, relic.description,
                    btn.xPosition + cardWidth / 2, btn.yPosition + cardHeight - 25, 0xAAAAAA);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 10) {
            Network.sendToServer(new SpireNetwork.PacketRoomAction("leave_shop", ""));
            return;
        }

        int index = button.id - 100;
        if (index < 0 || index >= shopItems.size()) return;

        Relic relic = shopItems.get(index);
        if (purchased.contains(relic.relicId)) return;

        Network.sendToServer(new SpireNetwork.PacketRoomAction("buy", relic.relicId));
        purchased.add(relic.relicId);
        button.displayString = "[已售罄]";
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
