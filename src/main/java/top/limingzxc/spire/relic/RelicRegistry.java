package top.limingzxc.spire.relic;

import java.util.*;

/**
 * 遗物注册表。
 * 管理所有遗物的注册和随机抽取。
 */
public class RelicRegistry {

    private static final List<Relic> ALL_RELICS = new ArrayList<>();

    public static void register(Relic relic) {
        ALL_RELICS.add(relic);
    }

    public static Relic getById(String relicId) {
        for (Relic r : ALL_RELICS) {
            if (r.relicId.equals(relicId)) return r;
        }
        return null;
    }

    /** 按权重随机获取遗物，排除已拥有的 */
    public static Relic getRandom(int floor, Set<String> excludeIds, Random random) {
        List<Relic> candidates = new ArrayList<>();
        for (Relic r : ALL_RELICS) {
            if (!excludeIds.contains(r.relicId)) {
                candidates.add(r);
            }
        }
        if (candidates.isEmpty()) return null;

        int totalWeight = 0;
        for (Relic r : candidates) totalWeight += r.weight;

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (Relic r : candidates) {
            cumulative += r.weight;
            if (roll < cumulative) return r;
        }
        return candidates.get(candidates.size() - 1);
    }

    /** 获取 N 个不重复的随机遗物 */
    public static List<Relic> getRandomBatch(int floor, Set<String> excludeIds, int count, Random random) {
        List<Relic> result = new ArrayList<>();
        Set<String> tempExclude = new HashSet<>(excludeIds);
        for (int i = 0; i < count; i++) {
            Relic relic = getRandom(floor, tempExclude, random);
            if (relic == null) break;
            result.add(relic);
            tempExclude.add(relic.relicId);
        }
        return result;
    }

    public static List<Relic> getAllRelics() {
        return Collections.unmodifiableList(ALL_RELICS);
    }
}