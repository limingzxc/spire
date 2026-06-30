package top.limingzxc.spire.weapon;

import top.limingzxc.spire.relic.RelicEffect;

import java.util.*;

/**
 * 词条注册表。
 * 注册 MVP 词条并提供随机抽取。
 */
public class AffixRegistry {

    private static final List<WeaponAffix> ALL_AFFIXES = new ArrayList<>();

    static {
        // 锋利: 近战伤害+3，材料: 铁锭×3
        ALL_AFFIXES.add(new WeaponAffix("sharp_edge", "锋利", 1,
            new RelicEffect(RelicEffect.Trigger.ON_ATTACK,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_MELEE_DAMAGE, 3.0f),
            "铁锭", 3));

        // 护佑: 受到伤害×0.85，材料: 皮革×5
        ALL_AFFIXES.add(new WeaponAffix("warding", "护佑", 1,
            new RelicEffect(RelicEffect.Trigger.ON_DAMAGE_TAKEN,
                RelicEffect.ModifierMode.MULTIPLY, RelicEffect.TARGET_DAMAGE_REDUCTION, 0.85f),
            "皮革", 5));

        // 吸血: 击杀回复1生命，材料: 金锭×2
        ALL_AFFIXES.add(new WeaponAffix("vampiric", "吸血", 1,
            new RelicEffect(RelicEffect.Trigger.ON_KILL,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_HEAL_ON_KILL, 1.0f),
            "金锭", 2));
    }

    public static WeaponAffix getById(String affixId) {
        for (WeaponAffix a : ALL_AFFIXES) {
            if (a.affixId.equals(affixId)) return a;
        }
        return null;
    }

    /** 获取随机 N 个词条 */
    public static List<WeaponAffix> getRandomBatch(int count, Random random) {
        List<WeaponAffix> copy = new ArrayList<>(ALL_AFFIXES);
        Collections.shuffle(copy, random);
        return copy.subList(0, Math.min(count, copy.size()));
    }

    public static List<WeaponAffix> getAllAffixes() {
        return Collections.unmodifiableList(ALL_AFFIXES);
    }
}