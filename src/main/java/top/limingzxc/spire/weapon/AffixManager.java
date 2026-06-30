package top.limingzxc.spire.weapon;

import top.limingzxc.spire.SpireMod;
import top.limingzxc.spire.relic.RelicEffect;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagList;
import net.minecraft.NBTTagString;

import java.util.*;

/**
 * 武器词条管理器。
 * 负责武器 NBT 读写和效果链式计算。
 */
public class AffixManager {

    private static final String NBT_AFFIX_LIST = "SpireAffixes";

    /** 读取武器上的词条列表 */
    public static List<WeaponAffix> getAffixes(ItemStack weapon) {
        List<WeaponAffix> result = new ArrayList<>();
        if (weapon == null || weapon.stackTagCompound == null) return result;
        if (!weapon.stackTagCompound.hasKey(NBT_AFFIX_LIST)) return result;

        NBTTagList affixList = weapon.stackTagCompound.getTagList(NBT_AFFIX_LIST);
        for (int i = 0; i < affixList.tagCount(); i++) {
            String affixId = ((NBTTagString) affixList.tagAt(i)).data;
            if (affixId == null || affixId.isEmpty()) continue;
            WeaponAffix affix = AffixRegistry.getById(affixId);
            if (affix != null) {
                result.add(affix);
            }
        }
        return result;
    }

    /** 为武器添加词条 */
    public static void applyAffix(ItemStack weapon, WeaponAffix affix) {
        if (weapon == null || affix == null) return;

        if (weapon.stackTagCompound == null) {
            weapon.stackTagCompound = new NBTTagCompound();
        }

        NBTTagList newList = new NBTTagList();
        if (weapon.stackTagCompound.hasKey(NBT_AFFIX_LIST)) {
            NBTTagList affixList = weapon.stackTagCompound.getTagList(NBT_AFFIX_LIST);
            for (int i = 0; i < affixList.tagCount(); i++) {
                String existing = ((NBTTagString) affixList.tagAt(i)).data;
                if (existing == null || existing.isEmpty()) continue;
                newList.appendTag(new NBTTagString("", existing));
            }
        }
        newList.appendTag(new NBTTagString("", affix.affixId));
        weapon.stackTagCompound.setTag(NBT_AFFIX_LIST, newList);

        SpireMod.LOGGER.info("Applied affix {} to weapon", affix.affixId);
    }

    /** 链式计算武器词条效果（在 RelicManager 之后调用） */
    public static float applyCombatModifiers(ItemStack weapon, RelicEffect.Trigger trigger,
                                              String target, float original) {
        if (weapon == null) return original;

        List<WeaponAffix> affixes = getAffixes(weapon);
        float result = original;

        for (WeaponAffix affix : affixes) {
            RelicEffect effect = affix.effect;
            if (effect.trigger == trigger && effect.target.equals(target)) {
                if (effect.mode == RelicEffect.ModifierMode.ADD) {
                    result += effect.value;
                } else if (effect.mode == RelicEffect.ModifierMode.MULTIPLY) {
                    result *= effect.value;
                }
            }
        }
        return result;
    }
}