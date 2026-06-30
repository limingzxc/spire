package top.limingzxc.spire.relic;

/**
 * 遗物效果定义。
 * 包含触发时机和修改器。
 */
public class RelicEffect {

    public enum Trigger {
        ON_COMBAT_START,
        ON_ATTACK,
        ON_DAMAGE_TAKEN,
        ON_KILL,
        ON_FLOOR_START,
        PASSIVE
    }

    public enum ModifierMode {
        ADD, MULTIPLY
    }

    public final Trigger trigger;
    public final ModifierMode mode;
    public final String target;
    public final float value;

    public RelicEffect(Trigger trigger, ModifierMode mode, String target, float value) {
        this.trigger = trigger;
        this.mode = mode;
        this.target = target;
        this.value = value;
    }

    public static final String TARGET_MELEE_DAMAGE = "MELEE_DAMAGE";
    public static final String TARGET_ARMOR = "ARMOR";
    public static final String TARGET_MAX_HP = "MAX_HP";
    public static final String TARGET_DAMAGE_REDUCTION = "DAMAGE_REDUCTION";
    public static final String TARGET_HEAL_ON_KILL = "HEAL_ON_KILL";
    public static final String TARGET_HUNGER_LIMIT = "HUNGER_LIMIT";
    public static final String TARGET_NUTRITION_LIMIT = "NUTRITION_LIMIT";
}