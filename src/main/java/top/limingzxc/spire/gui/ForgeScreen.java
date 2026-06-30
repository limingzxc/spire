package top.limingzxc.spire.gui;

import top.limingzxc.spire.network.SpireNetwork;
import top.limingzxc.spire.relic.RelicEffect;
import top.limingzxc.spire.weapon.AffixRegistry;
import top.limingzxc.spire.weapon.WeaponAffix;
import moddedmite.rustedironcore.network.Network;
import net.minecraft.GuiButton;
import net.minecraft.GuiScreen;

import java.util.List;

/**
 * 锻造 GUI。
 * 展示所有可用词条，玩家选择其一后通过 PacketRoomAction 发送到服务端。
 * 服务端验证材料并应用词条。取消则返回 RestScreen。
 */
public class ForgeScreen extends GuiScreen {

    private final int cardWidth = 200;
    private final int cardHeight = 50;
    private List<WeaponAffix> affixOptions;

    @Override
    public void initGui() {
        buttonList.clear();
        affixOptions = AffixRegistry.getAllAffixes();

        int startY = height / 2 - 80;
        for (int i = 0; i < affixOptions.size(); i++) {
            WeaponAffix affix = affixOptions.get(i);
            int y = startY + i * (cardHeight + 10);
            String label = affix.name + " — 需要: " + affix.materialName + "×" + affix.materialCount;
            buttonList.add(new GuiButton(100 + i, width / 2 - cardWidth / 2, y, cardWidth, cardHeight, label));
        }

        buttonList.add(new GuiButton(10, width / 2 - 50, height - 40, 100, 20, "取消"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawCenteredString(fontRenderer, "武器锻造", width / 2, 15, 0xFFAA00);
        drawCenteredString(fontRenderer, "将武器拿在手中，选择词条进行锻造",
            width / 2, 30, 0xAAAAAA);

        for (int i = 0; i < affixOptions.size(); i++) {
            WeaponAffix affix = affixOptions.get(i);
            if (i < buttonList.size()) {
                GuiButton btn = (GuiButton) buttonList.get(i);
                String effectDesc = affix.effect.target + " " +
                    (affix.effect.mode == RelicEffect.ModifierMode.ADD ? "+" : "×") +
                    affix.effect.value;
                drawCenteredString(fontRenderer, effectDesc,
                    btn.xPosition + cardWidth / 2, btn.yPosition + cardHeight - 15, 0x888888);
            }
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
