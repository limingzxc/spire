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

    /** 生成人类可读的效果描述（用于 tooltip） */
    public String getDescription() {
        String targetName;
        String suffix = "";
        if (TARGET_MELEE_DAMAGE.equals(target)) {
            targetName = "近战伤害";
        } else if (TARGET_DAMAGE_REDUCTION.equals(target)) {
            targetName = "受到伤害";
        } else if (TARGET_HEAL_ON_KILL.equals(target)) {
            targetName = "击杀回复";
            suffix = " 生命";
        } else if (TARGET_ARMOR.equals(target)) {
            targetName = "护甲";
        } else if (TARGET_MAX_HP.equals(target)) {
            targetName = "最大生命";
        } else if (TARGET_HUNGER_LIMIT.equals(target)) {
            targetName = "饥饿上限";
        } else if (TARGET_NUTRITION_LIMIT.equals(target)) {
            targetName = "营养上限";
        } else {
            targetName = target;
        }

        if (mode == ModifierMode.ADD) {
            return targetName + " +" + (value == (int) value ? String.valueOf((int) value) : String.valueOf(value)) + suffix;
        } else {
            return targetName + " ×" + (value == (int) value ? String.valueOf((int) value) : String.valueOf(value)) + suffix;
        }
    }
}