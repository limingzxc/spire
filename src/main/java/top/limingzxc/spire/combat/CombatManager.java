package top.limingzxc.spire.combat;

import top.limingzxc.spire.SpireMod;
import top.limingzxc.spire.dungeon.*;
import top.limingzxc.spire.relic.RelicManager;
import top.limingzxc.spire.weapon.AffixManager;
import moddedmite.rustedironcore.api.accessor.Accessor;
import net.minecraft.EntityLiving;
import net.minecraft.EntityLivingBase;
import net.minecraft.EntityPlayer;
import net.minecraft.ItemStack;
import net.minecraft.DamageSource;
import net.minecraft.World;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 战斗房间管理器。
 * 管理战斗的完整生命周期：生成 → 战斗 → 奖励。
 */
public class CombatManager {

    public enum State {
        PREPARING, FIGHTING, REWARDING, COMPLETED
    }

    private static final CombatManager INSTANCE = new CombatManager();

    /** playerUUID -> 当前战斗状态 */
    private final Map<UUID, State> combatStates = new HashMap<>();
    /** playerUUID -> 当前战斗中的敌人列表 */
    private final Map<UUID, List<EntityLiving>> activeEnemies = new HashMap<>();
    /** playerUUID -> 当前战斗的房间节点 */
    private final Map<UUID, MapNode> activeNodes = new HashMap<>();

    private CombatManager() {}

    public static CombatManager getInstance() {
        return INSTANCE;
    }

    /** 开始一次战斗 */
    public void startCombat(EntityPlayer player, MapNode node) {
        UUID playerId = player.getUniqueID();
        DungeonState state = DungeonManager.getInstance().getState(player);
        if (state == null) return;

        combatStates.put(playerId, State.PREPARING);
        activeNodes.put(playerId, node);

        int floor = state.currentFloor;
        RoomType roomType = node.type;

        // 从 EnemyTemplate 选取敌人
        int enemyCount = switch (roomType) {
            case COMBAT -> 2 + new Random().nextInt(2); // 2-3
            case ELITE -> 1 + new Random().nextInt(2);  // 1-2
            case BOSS -> 1;
            default -> 2;
        };

        List<EntityLiving> enemies = new ArrayList<>();
        World world = player.worldObj;

        for (int i = 0; i < enemyCount; i++) {
            EnemyTemplate template = EnemyTemplate.selectRandom(roomType, floor, new Random());
            if (template == null) continue;

            EntityLiving enemy = spawnEnemyFromTemplate(world, template, player);
            if (enemy != null) {
                enemies.add(enemy);
            }
        }

        if (enemies.isEmpty()) {
            SpireMod.LOGGER.warn("All enemy spawns failed for {}; falling back to 1 zombie.", player.getEntityName());
            EntityLiving fallback = spawnFallbackEnemy(world, player);
            if (fallback != null) enemies.add(fallback);
        }

        activeEnemies.put(playerId, enemies);
        combatStates.put(playerId, State.FIGHTING);

        SpireMod.LOGGER.info("Combat started for {}: {} enemies (type={})",
            player.getEntityName(), enemies.size(), roomType);
    }

    /** 敌人生成（反射构造；失败时回退到僵尸）。
     *  生成失败时回退到僵尸，但回退僵尸不应用模板的生命值倍率（避免给僵尸套上 BOSS 倍率）。 */
    private EntityLiving spawnEnemyFromTemplate(World world, EnemyTemplate template, EntityPlayer player) {
        EntityLiving entity = trySpawn(world, template.mobClassName, player);
        boolean isFallback = false;
        if (entity == null) {
            SpireMod.LOGGER.warn("Failed to spawn {}; falling back to zombie.", template.mobClassName);
            entity = spawnFallbackEnemy(world, player);
            isFallback = entity != null;
        }
        if (entity == null) return null;

        // 应用生命值倍率（回退僵尸跳过，使用默认数值）
        if (!isFallback && template.healthMultiplier != 1.0f) {
            applyHealthMultiplier(entity, template.healthMultiplier);
        }
        // 注：damageMultiplier/speedMultiplier 暂未应用（MITE 1.6.4 无 attribute 修改 API）
        // BOSS 通过提高生命值倍率实现难度

        entity.setAttackTarget(player);
        world.spawnEntityInWorld(entity);
        return entity;
    }

    /** 通过反射修改 maxHealth 字段后再 setHealth，使倍率 > 1 时实际生效（1.6.4 setHealth 会被 maxHealth 截断） */
    private void applyHealthMultiplier(EntityLiving entity, float multiplier) {
        float baseHealth = entity.getMaxHealth();
        float newMax = baseHealth * multiplier;
        try {
            Field f = EntityLivingBase.class.getDeclaredField("maxHealth");
            Accessor.modify(f, entity, newMax);
            entity.setHealth(newMax);
            SpireMod.LOGGER.info("Applied health multiplier {} -> {} on {}",
                baseHealth, newMax, entity.getClass().getSimpleName());
        } catch (NoSuchFieldException e) {
            // 字段名不匹配时回退：仅 setHealth（被 maxHealth 截断，BOSS 至少满血）
            SpireMod.LOGGER.warn("maxHealth field not found on {}; falling back to setHealth only.",
                entity.getClass().getSimpleName());
            entity.setHealth(baseHealth);
        } catch (Exception e) {
            SpireMod.LOGGER.warn("Failed to modify maxHealth: {}", e.getMessage());
            entity.setHealth(baseHealth);
        }
    }

    /** 反射生成指定类名的敌人 */
    private EntityLiving trySpawn(World world, String className, EntityPlayer player) {
        try {
            Class<?> entityClass = Class.forName("net.minecraft." + className);
            EntityLiving entity = (EntityLiving) entityClass.getConstructor(World.class).newInstance(world);

            double angle = Math.random() * Math.PI * 2;
            double dist = 4 + Math.random() * 3;
            entity.setPosition(player.posX + Math.cos(angle) * dist, player.posY, player.posZ + Math.sin(angle) * dist);
            return entity;
        } catch (Exception e) {
            SpireMod.LOGGER.warn("Spawn failed for {}: {}", className, e.getMessage());
            return null;
        }
    }

    /** 兜底生成僵尸，避免玩家卡在空战斗。
     *  仅创建并定位实体，不调用 spawnEntityInWorld（由调用方统一处理，避免双重生成）。 */
    private EntityLiving spawnFallbackEnemy(World world, EntityPlayer player) {
        return trySpawn(world, "EntityZombie", player);
    }

    /** 敌人死亡回调 */
    public void onEnemyDied(EntityLivingBase entity, DamageSource cause) {
        if (!(entity instanceof EntityLiving)) return;

        // 找到这个敌人所属的战斗
        for (Map.Entry<UUID, List<EntityLiving>> entry : activeEnemies.entrySet()) {
            List<EntityLiving> enemies = entry.getValue();
            if (enemies.remove(entity)) {
                UUID playerId = entry.getKey();
                EntityPlayer player = findPlayerByUUID(entity.worldObj, playerId);
                if (player == null) continue;

                // 触发 ON_KILL 效果（遗物 + 武器词条吸血等）
                float healAmount = RelicManager.INSTANCE.applyCombatModifiers(player,
                    top.limingzxc.spire.relic.RelicEffect.Trigger.ON_KILL,
                    top.limingzxc.spire.relic.RelicEffect.TARGET_HEAL_ON_KILL, 0);
                ItemStack weapon = player.getHeldItemStack();
                healAmount = AffixManager.applyCombatModifiers(weapon,
                    top.limingzxc.spire.relic.RelicEffect.Trigger.ON_KILL,
                    top.limingzxc.spire.relic.RelicEffect.TARGET_HEAL_ON_KILL, healAmount);
                if (healAmount > 0) {
                    float maxHP = player.getMaxHealth();
                    player.setHealth(Math.min(player.getHealth() + healAmount, maxHP));
                }

                if (enemies.isEmpty()) {
                    onAllEnemiesDefeated(player);
                }
                break;
            }
        }
    }

    /** 通过 UUID 在世界中查找玩家（1.6.4 World 无按 UUID 查找方法） */
    private EntityPlayer findPlayerByUUID(World world, UUID uuid) {
        for (Object o : world.playerEntities) {
            if (o instanceof EntityPlayer) {
                EntityPlayer p = (EntityPlayer) o;
                if (p.getUniqueID().equals(uuid)) return p;
            }
        }
        return null;
    }

    /** 所有敌人被击败 */
    private void onAllEnemiesDefeated(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        combatStates.put(playerId, State.REWARDING);
        activeEnemies.remove(playerId);

        MapNode node = activeNodes.remove(playerId);
        if (node == null) return;

        // 触发奖励阶段
        DungeonManager.getInstance().completeRoom(player);

        SpireMod.LOGGER.info("All enemies defeated for {} at node {}", player.getEntityName(), node.id);
    }

    /** 玩家死亡 */
    public void onPlayerDied(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        combatStates.remove(playerId);

        List<EntityLiving> enemies = activeEnemies.remove(playerId);
        if (enemies != null) {
            for (EntityLiving enemy : enemies) {
                if (!enemy.isDead) {
                    enemy.setDead();
                }
            }
        }
        activeNodes.remove(playerId);
        DungeonManager.getInstance().endRun(player);
    }

    /** 清理战斗（强制结束） */
    public void cleanupCombat(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        combatStates.remove(playerId);
        activeEnemies.remove(playerId);
        activeNodes.remove(playerId);
    }

    public State getCombatState(EntityPlayer player) {
        return combatStates.getOrDefault(player.getUniqueID(), State.COMPLETED);
    }

    public boolean isInCombat(EntityPlayer player) {
        State s = combatStates.get(player.getUniqueID());
        return s == State.PREPARING || s == State.FIGHTING;
    }
}