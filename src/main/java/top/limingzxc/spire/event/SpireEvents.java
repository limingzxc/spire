package top.limingzxc.spire.event;

import top.limingzxc.spire.SpireMod;
import top.limingzxc.spire.combat.CombatManager;
import top.limingzxc.spire.dimension.SpireDimensionProvider;
import top.limingzxc.spire.dungeon.DungeonManager;
import top.limingzxc.spire.register.SpireRegistryInit;
import top.limingzxc.spire.relic.RelicEffect;
import top.limingzxc.spire.relic.RelicManager;
import top.limingzxc.spire.weapon.AffixManager;
import moddedmite.rustedironcore.api.event.Handlers;
import moddedmite.rustedironcore.api.event.events.CraftingRecipeRegisterEvent;
import moddedmite.rustedironcore.api.event.events.DimensionRegisterEvent;
import moddedmite.rustedironcore.api.event.events.PlayerLoggedInEvent;
import moddedmite.rustedironcore.api.event.events.PlayerRespawnEvent;
import moddedmite.rustedironcore.api.event.listener.*;
import moddedmite.rustedironcore.api.world.Dimension;
import moddedmite.rustedironcore.api.world.DimensionContext;
import net.minecraft.Block;
import net.minecraft.Damage;
import net.minecraft.Entity;
import net.minecraft.EntityLivingBase;
import net.minecraft.EntityPlayer;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.DamageSource;
import net.minecraft.ServerPlayer;

/**
 * 集中注册所有 RIC Handler。
 * 每个 Handler 对应一类游戏事件，由对应的模块处理。
 */
public class SpireEvents {

    public static void register() {
        SpireMod.LOGGER.info("Registering SpireCraft events...");

        // === Dimension: 注册尖塔维度 ===
        Handlers.Dimension.register((DimensionRegisterEvent event) -> {
            int id = event.getNextDimensionID();
            Dimension spireDim = new Dimension("spire", id);
            event.register(spireDim,
                new DimensionContext(
                    () -> new SpireDimensionProvider(id, "spire"),
                    true,
                    false
                )
            );
            SpireDimensionProvider.SPIRE_DIMENSION = spireDim;
            SpireMod.LOGGER.info("Spire dimension registered: id={}", id);
        });

        // === Crafting: 尖塔传送门合成配方（4 黑曜石 + 1 钻石） ===
        Handlers.Crafting.register((CraftingRecipeRegisterEvent event) -> {
            if (SpireRegistryInit.SPIRE_PORTAL == null) {
                SpireMod.LOGGER.warn("SPIRE_PORTAL not initialized; skipping crafting recipe.");
                return;
            }
            event.registerShapedRecipe(
                new ItemStack(SpireRegistryInit.SPIRE_PORTAL, 1),
                true,
                " O ",
                "ODO",
                " O ",
                'O', Block.obsidian,
                'D', Item.diamond
            );
            SpireMod.LOGGER.info("Spire portal crafting recipe registered.");
        });

        // === WorldInfo: 持久化 run 状态到 level.dat ===
        Handlers.WorldInfo.register(new IWorldInfoListener() {
            @Override
            public void onNBTWrite(NBTTagCompound nbt) {
                DungeonManager.getInstance().writeToNBT(nbt);
            }

            @Override
            public void onNBTRead(NBTTagCompound nbt) {
                DungeonManager.getInstance().readFromNBT(nbt);
            }
        });

        // === PlayerEvent: 玩家登录重建遗物缓存 + 重生返回主世界 ===
        Handlers.PlayerEvent.register(new IPlayerEventListener() {
            @Override
            public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
                DungeonManager.getInstance().onPlayerLoggedIn(event.player());
            }

            @Override
            public void onPlayerRespawn(PlayerRespawnEvent event) {
                DungeonManager.getInstance().onPlayerRespawn(event.player());
            }
        });

        // === EntityEvent: 敌人死亡回调 + 玩家死亡清理 ===
        Handlers.EntityEvent.register(new IEntityEventListener() {
            @Override
            public void onDeath(EntityLivingBase entity, DamageSource cause) {
                CombatManager.getInstance().onEnemyDied(entity, cause);
            }

            @Override
            public void onServerPlayerDeath(ServerPlayer player, DamageSource cause) {
                SpireMod.LOGGER.info("Player {} died in SpireCraft run, cleaning up.", player.getEntityName());
                CombatManager.getInstance().onPlayerDied(player);
            }
        });

        // === Combat: 遗物链式修改（先 Relic 后 Affix） ===
        Handlers.Combat.register(new ICombatListener() {
            @Override
            public float onPlayerRawMeleeDamageModify(EntityPlayer player, Entity target,
                                                       boolean critical, boolean suspended_in_liquid, float damage) {
                damage = RelicManager.INSTANCE.applyCombatModifiers(player,
                    RelicEffect.Trigger.ON_ATTACK, RelicEffect.TARGET_MELEE_DAMAGE, damage);
                ItemStack weapon = player.getHeldItemStack();
                damage = AffixManager.applyCombatModifiers(weapon,
                    RelicEffect.Trigger.ON_ATTACK, RelicEffect.TARGET_MELEE_DAMAGE, damage);
                return damage;
            }

            @Override
            public void onPlayerReceiveDamageModify(EntityPlayer player, Damage damage) {
                float dmg = damage.getAmount();
                dmg = RelicManager.INSTANCE.applyCombatModifiers(player,
                    RelicEffect.Trigger.ON_DAMAGE_TAKEN, RelicEffect.TARGET_DAMAGE_REDUCTION, dmg);
                ItemStack weapon = player.getHeldItemStack();
                dmg = AffixManager.applyCombatModifiers(weapon,
                    RelicEffect.Trigger.ON_DAMAGE_TAKEN, RelicEffect.TARGET_DAMAGE_REDUCTION, dmg);
                damage.setAmount(dmg);
            }

            @Override
            public float onArmorProtectionModify(ItemStack item_stack, EntityLivingBase owner, float original) {
                if (!(owner instanceof EntityPlayer)) return original;
                return RelicManager.INSTANCE.applyAttributeModifiers((EntityPlayer) owner,
                    RelicEffect.Trigger.PASSIVE, RelicEffect.TARGET_ARMOR, original);
            }
        });

        // === PlayerAttribute: 遗物常驻属性 ===
        Handlers.PlayerAttribute.register(new IPlayerAttributeListener() {
            @Override
            public float onHealthLimitModify(EntityPlayer player, float original) {
                return RelicManager.INSTANCE.applyAttributeModifiers(player,
                    RelicEffect.Trigger.PASSIVE, RelicEffect.TARGET_MAX_HP, original);
            }

            @Override
            public int onHungerLimitModify(EntityPlayer player, int original) {
                return (int) RelicManager.INSTANCE.applyAttributeModifiers(player,
                    RelicEffect.Trigger.PASSIVE, RelicEffect.TARGET_HUNGER_LIMIT, original);
            }
        });

        // === Tick: dungeon 状态机驱动 ===
        Handlers.Tick.register(new ITickListener() {
            @Override
            public void onEntityPlayerTick(EntityPlayer player) {
                DungeonManager.getInstance().onPlayerTick(player);
            }

            @Override
            public void onClientTick(net.minecraft.Minecraft client) {
                DungeonManager.getInstance().onClientTick(client);
            }
        });

        SpireMod.LOGGER.info("SpireCraft events registered.");
    }
}