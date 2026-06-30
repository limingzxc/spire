package top.limingzxc.spire.weapon;

import top.limingzxc.spire.relic.RelicEffect;
import net.minecraft.Item;

/**
 * 武器词条数据类。
 * 复用 RelicEffect 结构定义效果。
 */
public class WeaponAffix {

    public final String affixId;
    public final String name;
    public final int level;
    public final RelicEffect effect;
    public final String materialName;
    public final int materialCount;

    public WeaponAffix(String affixId, String name, int level, RelicEffect effect,
                       String materialName, int materialCount) {
        this.affixId = affixId;
        this.name = name;
        this.level = level;
        this.effect = effect;
        this.materialName = materialName;
        this.materialCount = materialCount;
    }
}