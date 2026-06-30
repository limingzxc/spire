package top.limingzxc.spire.relic.relics;

import top.limingzxc.spire.relic.Relic;
import top.limingzxc.spire.relic.RelicEffect;
import top.limingzxc.spire.relic.RelicRegistry;

/**
 * MVP 遗物注册。
 * 3 个初始遗物：力量之戒、护甲碎片、吸血之牙。
 */
public class StarterRelics {

    public static final Relic RING_OF_POWER;
    public static final Relic ARMOR_FRAGMENT;
    public static final Relic VAMPIRE_FANG;

    static {
        RING_OF_POWER = new Relic("ring_of_power", "力量之戒", "近战伤害+2",
            Relic.Rarity.COMMON, 50)
            .addEffect(new RelicEffect(RelicEffect.Trigger.ON_ATTACK,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_MELEE_DAMAGE, 2.0f));

        ARMOR_FRAGMENT = new Relic("armor_fragment", "护甲碎片", "护甲值+1",
            Relic.Rarity.COMMON, 50)
            .addEffect(new RelicEffect(RelicEffect.Trigger.PASSIVE,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_ARMOR, 1.0f));

        VAMPIRE_FANG = new Relic("vampire_fang", "吸血之牙", "击杀敌人回复2生命值",
            Relic.Rarity.UNCOMMON, 30)
            .addEffect(new RelicEffect(RelicEffect.Trigger.ON_KILL,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_HEAL_ON_KILL, 2.0f));
    }

    public static void registerAll() {
        RelicRegistry.register(RING_OF_POWER);
        RelicRegistry.register(ARMOR_FRAGMENT);
        RelicRegistry.register(VAMPIRE_FANG);
    }
}