package top.limingzxc.spire.combat;

import top.limingzxc.spire.dungeon.RoomType;

import java.util.*;

/**
 * 敌人模板配置。
 * 定义每个敌人类别、属性倍率和出现条件。
 * 融入 MITE 独有怪物体系。
 */
public class EnemyTemplate {

    public final String enemyId;
    public final String mobClassName;
    public final int weight;
    public final int minFloor;
    public final int maxFloor;
    public final RoomType roomType;
    public final float healthMultiplier;
    public final float damageMultiplier;
    public final float speedMultiplier;

    private static final List<EnemyTemplate> ALL_TEMPLATES = new ArrayList<>();

    public EnemyTemplate(String enemyId, String mobClassName, int weight, int minFloor, int maxFloor,
                         RoomType roomType, float healthMultiplier, float damageMultiplier, float speedMultiplier) {
        this.enemyId = enemyId;
        this.mobClassName = mobClassName;
        this.weight = weight;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.roomType = roomType;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.speedMultiplier = speedMultiplier;
    }

    public static void register(EnemyTemplate template) {
        ALL_TEMPLATES.add(template);
    }

    /** 获取指定房间类型和楼层的候选敌人列表（按权重） */
    public static List<EnemyTemplate> getForRoom(RoomType type, int floor) {
        List<EnemyTemplate> candidates = new ArrayList<>();
        for (EnemyTemplate t : ALL_TEMPLATES) {
            if (t.roomType == type && floor >= t.minFloor && floor <= t.maxFloor) {
                candidates.add(t);
            }
        }
        return candidates;
    }

    /** 按权重随机选择敌人 */
    public static EnemyTemplate selectRandom(RoomType type, int floor, Random random) {
        List<EnemyTemplate> candidates = getForRoom(type, floor);
        if (candidates.isEmpty()) return null;

        int totalWeight = 0;
        for (EnemyTemplate t : candidates) totalWeight += t.weight;

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (EnemyTemplate t : candidates) {
            cumulative += t.weight;
            if (roll < cumulative) return t;
        }
        return candidates.get(candidates.size() - 1);
    }

    // ======================== MVP 敌人注册 ========================

    static {
        // COMBAT 敌人
        register(new EnemyTemplate("wight", "EntityWight", 20, 1, 3,
            RoomType.COMBAT, 1.0f, 1.0f, 1.0f));
        register(new EnemyTemplate("wood_spider", "EntityWoodSpider", 20, 1, 2,
            RoomType.COMBAT, 0.8f, 0.9f, 1.3f));
        register(new EnemyTemplate("shadow", "EntityShadow", 20, 1, 3,
            RoomType.COMBAT, 1.0f, 1.2f, 1.0f));
        register(new EnemyTemplate("earth_elemental", "EntityEarthElemental", 15, 1, 2,
            RoomType.COMBAT, 1.5f, 0.7f, 0.6f));
        register(new EnemyTemplate("fire_elemental", "EntityFireElemental", 15, 2, 3,
            RoomType.COMBAT, 1.0f, 1.3f, 1.0f));
        register(new EnemyTemplate("ghoul", "EntityGhoul", 15, 2, 3,
            RoomType.COMBAT, 1.0f, 1.0f, 0.9f));

        // ELITE 敌人
        register(new EnemyTemplate("hellhound", "EntityHellhound", 30, 1, 2,
            RoomType.ELITE, 1.3f, 1.3f, 1.5f));
        register(new EnemyTemplate("invisible_stalker", "EntityInvisibleStalker", 25, 2, 3,
            RoomType.ELITE, 1.2f, 1.5f, 1.1f));
        register(new EnemyTemplate("phase_spider", "EntityPhaseSpider", 25, 2, 3,
            RoomType.ELITE, 1.1f, 1.2f, 1.4f));

        // BOSS 敌人
        register(new EnemyTemplate("ancient_bone_lord", "EntityAncientBoneLord", 100, 1, 1,
            RoomType.BOSS, 3.0f, 1.5f, 0.8f));
        register(new EnemyTemplate("nightwing", "EntityNightwing", 100, 2, 2,
            RoomType.BOSS, 3.5f, 1.8f, 1.2f));
        register(new EnemyTemplate("demon_spider", "EntityDemonSpider", 100, 3, 3,
            RoomType.BOSS, 4.0f, 2.0f, 1.0f));
    }
}