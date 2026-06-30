package top.limingzxc.spire.relic.relics;

import top.limingzxc.spire.relic.Relic;
import top.limingzxc.spire.relic.RelicEffect;
import top.limingzxc.spire.relic.RelicRegistry;

/**
 * 通用遗物池。
 * 不同于 StarterRelics（仅作为开局三选一），这里的遗物通过战斗奖励 / 宝箱 / 事件 / 商店进入玩家手中。
 * 覆盖 MAX_HP / DAMAGE_REDUCTION / ARMOR / MELEE_DAMAGE / HEAL_ON_KILL / HUNGER_LIMIT 等效果。
 */
public class SpireRelics {

    public static void registerAll() {
        // ========== COMMON ==========
        RelicRegistry.register(new Relic("warrior_heart", "战士之心", "生命上限 +4",
            Relic.Rarity.COMMON, 50)
            .addEffect(new RelicEffect(RelicEffect.Trigger.PASSIVE,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_MAX_HP, 4.0f)));

        RelicRegistry.register(new Relic("iron_will", "钢铁意志", "受到伤害 ×0.9",
            Relic.Rarity.COMMON, 50)
            .addEffect(new RelicEffect(RelicEffect.Trigger.ON_DAMAGE_TAKEN,
                RelicEffect.ModifierMode.MULTIPLY, RelicEffect.TARGET_DAMAGE_REDUCTION, 0.9f)));

        RelicRegistry.register(new Relic("sharp_blade", "锋利之刃", "近战伤害 +1",
            Relic.Rarity.COMMON, 50)
            .addEffect(new RelicEffect(RelicEffect.Trigger.ON_ATTACK,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_MELEE_DAMAGE, 1.0f)));

        RelicRegistry.register(new Relic("satiety_ring", "饱腹之戒", "饥饿上限 +6",
            Relic.Rarity.COMMON, 40)
            .addEffect(new RelicEffect(RelicEffect.Trigger.PASSIVE,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_HUNGER_LIMIT, 6.0f)));

        // ========== UNCOMMON ==========
        RelicRegistry.register(new Relic("thorn_armor", "荆棘护甲", "护甲值 +2",
            Relic.Rarity.UNCOMMON, 30)
            .addEffect(new RelicEffect(RelicEffect.Trigger.PASSIVE,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_ARMOR, 2.0f)));

        RelicRegistry.register(new Relic("life_steal", "生命窃取", "击杀回复 1 生命",
            Relic.Rarity.UNCOMMON, 30)
            .addEffect(new RelicEffect(RelicEffect.Trigger.ON_KILL,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_HEAL_ON_KILL, 1.0f)));

        RelicRegistry.register(new Relic("berserker", "狂战之怒", "近战伤害 +4",
            Relic.Rarity.UNCOMMON, 30)
            .addEffect(new RelicEffect(RelicEffect.Trigger.ON_ATTACK,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_MELEE_DAMAGE, 4.0f)));

        RelicRegistry.register(new Relic("vitality_crystal", "活力水晶", "生命上限 +8",
            Relic.Rarity.UNCOMMON, 30)
            .addEffect(new RelicEffect(RelicEffect.Trigger.PASSIVE,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_MAX_HP, 8.0f)));

        // ========== RARE ==========
        RelicRegistry.register(new Relic("damage_ward", "伤害护盾", "受到伤害 ×0.75",
            Relic.Rarity.RARE, 15)
            .addEffect(new RelicEffect(RelicEffect.Trigger.ON_DAMAGE_TAKEN,
                RelicEffect.ModifierMode.MULTIPLY, RelicEffect.TARGET_DAMAGE_REDUCTION, 0.75f)));

        RelicRegistry.register(new Relic("titan_belt", "泰坦腰带", "生命上限 +12，护甲 +1",
            Relic.Rarity.RARE, 15)
            .addEffect(new RelicEffect(RelicEffect.Trigger.PASSIVE,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_MAX_HP, 12.0f))
            .addEffect(new RelicEffect(RelicEffect.Trigger.PASSIVE,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_ARMOR, 1.0f)));

        RelicRegistry.register(new Relic("executioner", "处刑者", "近战伤害 +6，击杀回复 3 生命",
            Relic.Rarity.RARE, 15)
            .addEffect(new RelicEffect(RelicEffect.Trigger.ON_ATTACK,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_MELEE_DAMAGE, 6.0f))
            .addEffect(new RelicEffect(RelicEffect.Trigger.ON_KILL,
                RelicEffect.ModifierMode.ADD, RelicEffect.TARGET_HEAL_ON_KILL, 3.0f)));
    }
}
