package top.limingzxc.spire.relic;

import net.minecraft.EntityPlayer;

import java.util.*;

/**
 * 遗物效果引擎。
 * 收集玩家遗物，按触发时机分类，对 RIC 链式修改器应用效果。
 */
public class RelicManager {

    public static final RelicManager INSTANCE = new RelicManager();

    /** playerUUID -> 按触发时机分组的 effect 列表 */
    private final Map<UUID, Map<RelicEffect.Trigger, List<RelicEffect>>> effectCache = new HashMap<>();

    private RelicManager() {}

    /** 重新计算玩家所有遗物效果（收集遗物/失去遗物时调用） */
    public void recalculateEffects(EntityPlayer player, List<String> relicIds) {
        Map<RelicEffect.Trigger, List<RelicEffect>> grouped = new EnumMap<>(RelicEffect.Trigger.class);
        for (RelicEffect.Trigger trigger : RelicEffect.Trigger.values()) {
            grouped.put(trigger, new ArrayList<>());
        }
        for (String relicId : relicIds) {
            Relic relic = RelicRegistry.getById(relicId);
            if (relic != null) {
                for (RelicEffect effect : relic.effects) {
                    grouped.get(effect.trigger).add(effect);
                }
            }
        }
        effectCache.put(player.getUniqueID(), grouped);
    }

    /** 清除玩家缓存 */
    public void clearPlayer(EntityPlayer player) {
        effectCache.remove(player.getUniqueID());
    }

    /** 对战相关修改器应用（ON_ATTACK, ON_DAMAGE_TAKEN 等） */
    public float applyCombatModifiers(EntityPlayer player, RelicEffect.Trigger trigger,
                                       String target, float original) {
        Map<RelicEffect.Trigger, List<RelicEffect>> grouped = effectCache.get(player.getUniqueID());
        if (grouped == null) return original;

        List<RelicEffect> effects = grouped.get(trigger);
        if (effects == null || effects.isEmpty()) return original;

        float result = original;
        for (RelicEffect effect : effects) {
            if (effect.target.equals(target)) {
                if (effect.mode == RelicEffect.ModifierMode.ADD) {
                    result += effect.value;
                } else if (effect.mode == RelicEffect.ModifierMode.MULTIPLY) {
                    result *= effect.value;
                }
            }
        }
        return result;
    }

    /** 属性修改器应用（PASSIVE, ON_FLOOR_START） */
    public float applyAttributeModifiers(EntityPlayer player, RelicEffect.Trigger trigger,
                                          String target, float original) {
        return applyCombatModifiers(player, trigger, target, original);
    }

    /** 获取玩家的按触发分组的效果 */
    public Map<RelicEffect.Trigger, List<RelicEffect>> getPlayerEffects(EntityPlayer player) {
        return effectCache.get(player.getUniqueID());
    }
}