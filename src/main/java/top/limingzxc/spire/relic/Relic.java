package top.limingzxc.spire.relic;

import java.util.*;

/**
 * 遗物数据类。
 * 每个遗物有一个或多个 RelicEffect。
 */
public class Relic {

    public enum Rarity {
        COMMON, UNCOMMON, RARE
    }

    public final String relicId;
    public final String name;
    public final String description;
    public final List<RelicEffect> effects;
    public final Rarity rarity;
    public final int weight;

    public Relic(String relicId, String name, String description, Rarity rarity, int weight) {
        this.relicId = relicId;
        this.name = name;
        this.description = description;
        this.effects = new ArrayList<>();
        this.rarity = rarity;
        this.weight = weight;
    }

    public Relic addEffect(RelicEffect effect) {
        this.effects.add(effect);
        return this;
    }

    public int getGoldValue() {
        return switch (rarity) {
            case COMMON -> 50;
            case UNCOMMON -> 100;
            case RARE -> 150;
        };
    }
}