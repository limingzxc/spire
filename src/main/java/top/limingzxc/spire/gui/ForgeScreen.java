package top.limingzxc.spire.gui;

import top.limingzxc.spire.network.SpireNetwork;
import top.limingzxc.spire.weapon.AffixRegistry;
import top.limingzxc.spire.weapon.WeaponAffix;
import moddedmite.rustedironcore.network.Network;
import net.minecraft.GuiButton;
import net.minecraft.GuiScreen;

import java.util.List;

/**
 * 锻造 GUI。
 * 展示所有可用词条，玩家选择其一后通过 PacketRoomAction 发送到服务端。
 */
public class ForgeScreen extends GuiScreen {

    private static final int CARD_WIDTH = 180;
    private static final int CARD_HEIGHT = 20;
    private static final int CARD_GAP = 36;
    private static final int COLOR_EFFECT = 0xFFFFAA;
    private static final int COLOR_MATERIAL = 0xAAAAAA;

    private List<WeaponAffix> affixOptions;

    @Override
    public void initGui() {
        buttonList.clear();
        affixOptions = AffixRegistry.getAllAffixes();

        int totalHeight = affixOptions.size() * CARD_GAP;
        int startY = (height - totalHeight) / 2 - 10;

        for (int i = 0; i < affixOptions.size(); i++) {
            int y = startY + i * CARD_GAP;
            buttonList.add(new GuiButton(100 + i,
                width / 2 - CARD_WIDTH / 2, y, CARD_WIDTH, CARD_HEIGHT,
                affixOptions.get(i).name));
        }

        buttonList.add(new GuiButton(10, width / 2 - 50, startY + affixOptions.size() * CARD_GAP + 8, 100, 20, "取消"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawCenteredString(fontRenderer, "§6武器锻造", width / 2, 15, 0xFFAA00);
        drawCenteredString(fontRenderer, "§7将武器拿在手中，选择词条进行锻造",
            width / 2, 30, 0xAAAAAA);

        for (int i = 0; i < affixOptions.size() && i < buttonList.size(); i++) {
            GuiButton btn = (GuiButton) buttonList.get(i);
            WeaponAffix affix = affixOptions.get(i);

            int infoY = btn.yPosition + CARD_HEIGHT + 4;
            String desc = affix.effect.getDescription();
            String cost = "需要: " + affix.materialName + "×" + affix.materialCount;

            // 效果描述在左，材料需求在右
            drawString(fontRenderer, desc, btn.xPosition + 4, infoY, COLOR_EFFECT);
            drawString(fontRenderer, cost, btn.xPosition + CARD_WIDTH - fontRenderer.getStringWidth(cost) - 4, infoY, COLOR_MATERIAL);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 10) {
            mc.displayGuiScreen(new RestScreen());
            return;
        }

        int index = button.id - 100;
        if (index < 0 || index >= affixOptions.size()) return;

        WeaponAffix chosen = affixOptions.get(index);
        Network.sendToServer(new SpireNetwork.PacketRoomAction("forge", chosen.affixId));
        mc.displayGuiScreen(new RestScreen());
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}