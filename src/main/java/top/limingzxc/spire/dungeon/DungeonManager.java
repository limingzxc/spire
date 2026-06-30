package top.limingzxc.spire.dungeon;

import top.limingzxc.spire.SpireMod;
import top.limingzxc.spire.dimension.SpireDimensionProvider;
import top.limingzxc.spire.gui.*;
import top.limingzxc.spire.network.SpireNetwork;
import top.limingzxc.spire.relic.Relic;
import top.limingzxc.spire.relic.RelicManager;
import top.limingzxc.spire.relic.RelicRegistry;
import top.limingzxc.spire.weapon.AffixManager;
import top.limingzxc.spire.weapon.AffixRegistry;
import top.limingzxc.spire.weapon.WeaponAffix;
import moddedmite.rustedironcore.network.Network;
import net.minecraft.Block;
import net.minecraft.EntityPlayer;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import net.minecraft.ServerPlayer;
import net.minecraft.Minecraft;
import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagList;
import net.minecraft.NBTTagString;
import net.minecraft.World;
import net.minecraft.WorldServer;

import java.util.*;

/**
 * 一次爬塔 run 的全生命周期状态机。
 *
 * 服务端持有权威状态，客户端持有镜像。
 * 状态通过 PacketSyncState 同步，GUI 屏幕由 onSyncApplied 驱动打开。
 */
public class DungeonManager {

    private static final DungeonManager INSTANCE = new DungeonManager();

    /** playerUUID -> DungeonState，仅服务端持有 */
    private final Map<UUID, DungeonState> serverStates = new HashMap<>();

    /** playerUUID -> 返回主世界的坐标（run 开始前记录，endRun/respawn 时使用） */
    private final Map<UUID, ReturnLocation> pendingReturns = new HashMap<>();

    /** 客户端镜像状态 */
    private DungeonState clientState = new DungeonState();

    /** 客户端: 同步到达后需打开的 phase（由 onClientTick 消费） */
    private DungeonPhase pendingScreenPhase = null;

    /** 战斗平台参数 */
    private static final int PLATFORM_Y = 64;
    private static final int PLATFORM_RADIUS = 12;
    private static final double SPAWN_X = 0.5;
    private static final double SPAWN_Y = 65.0;
    private static final double SPAWN_Z = 0.5;

    private DungeonManager() {}

    /** 玩家返回主世界的坐标 */
    private static class ReturnLocation {
        final int dimensionId;
        final double x, y, z;
        ReturnLocation(int dim, double x, double y, double z) {
            this.dimensionId = dim; this.x = x; this.y = y; this.z = z;
        }
    }

    public static DungeonManager getInstance() {
        return INSTANCE;
    }

    // ======================== 服务端状态操作 ========================

    public DungeonState getState(EntityPlayer player) {
        return serverStates.get(player.getUniqueID());
    }

    public boolean isRunActive(EntityPlayer player) {
        DungeonState state = serverStates.get(player.getUniqueID());
        return state != null && state.isRunActive;
    }

    // ======================== 状态转换 API（服务端调用） ========================

    /** 开始新 run: IDLE -> MAP，传送玩家到尖塔维度并生成战斗平台。
     *  传送失败时回滚 state，避免玩家卡在 isRunActive=true 但未传送的状态。 */
    public void startNewRun(EntityPlayer player) {
        if (!teleportToSpire(player)) {
            SpireMod.LOGGER.error("Teleport to spire failed for {}; run not started.", player.getEntityName());
            return;
        }

        DungeonState state = new DungeonState();
        state.startNewRun();
        serverStates.put(player.getUniqueID(), state);

        state.floorMap = new FloorMap(state.currentFloor);
        state.phase = DungeonPhase.MAP;

        grantStarterGear(player);
        grantStarterRelic(player, state);
        RelicManager.INSTANCE.recalculateEffects(player, state.relicIds);

        syncToClient(player);
        SpireMod.LOGGER.info("New run started for {}: floor 1, {} nodes",
            player.getEntityName(), state.floorMap.nodes.size());
    }

    /** 记录返回坐标 + 传送到尖塔维度 + 生成战斗平台 + 放置玩家。
     *  返回 true 表示成功；false 表示传送失败（dim 未注册或玩家非 ServerPlayer）。 */
    private boolean teleportToSpire(EntityPlayer player) {
        if (SpireDimensionProvider.SPIRE_DIMENSION == null) {
            SpireMod.LOGGER.error("Spire dimension not registered; cannot start run. " +
                "Enable RIC config \"UseCustomDimension\" in RustedIronCore.json and restart the game.");
            return false;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            SpireMod.LOGGER.error("Player {} not ServerPlayer; cannot teleport.", player.getEntityName());
            return false;
        }

        pendingReturns.put(player.getUniqueID(), new ReturnLocation(
            player.dimension,
            player.posX, player.posY, player.posZ));

        int spireDimId = SpireDimensionProvider.SPIRE_DIMENSION.id();
        cleanPlayerManagerResidual(serverPlayer, spireDimId, SPAWN_X, SPAWN_Z);
        serverPlayer.travelToDimension(spireDimId);
        clearPortalRubble(player.worldObj,
            (int) player.posX, (int) player.posY, (int) player.posZ, 6);
        generatePlatform(player.worldObj);
        serverPlayer.travelInsideDimension(SPAWN_X, SPAWN_Y, SPAWN_Z);
        SpireMod.LOGGER.info("Player {} teleported to spire dimension {}",
            player.getEntityName(), spireDimId);
        return true;
    }

    /** 清理 1.6.4 transferEntityToWorld 在目标世界自动生成的下界传送门框架
     *  （传送门方块 + 黑曜石框）。仅清除这两种方块，不破坏周围地形。
     *  玩家在传送后已被 placeInExistingPortal 放进该框架，随后由 travelInsideDimension
     *  移走，因此同 tick 清除不会触发再次传送。 */
    private void clearPortalRubble(World world, int cx, int cy, int cz, int radius) {
        int portalId = Block.portal.blockID;
        int obsidianId = Block.obsidian.blockID;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int id = world.getBlockId(cx + dx, cy + dy, cz + dz);
                    if (id == portalId || id == obsidianId) {
                        world.setBlock(cx + dx, cy + dy, cz + dz, 0);
                    }
                }
            }
        }
    }

    /** 清理目标维度 PlayerManager 中可能残留的玩家注册（防御 client crash 后状态不一致）。
     *  transferPlayerToDimension 的 func_72375_a 只从源维度 removePlayer，不清目标维度残留，
     *  导致 addPlayer 撞上 "already in chunk" 崩溃。这里主动在目标维度 removePlayer，
     *  重置 managedPos 到目标坐标使扫描范围覆盖残留；removePlayer 对不含玩家的 chunk 是 no-op。 */
    private void cleanPlayerManagerResidual(ServerPlayer player, int targetDimId, double targetX, double targetZ) {
        WorldServer targetWorld = player.mcServer.worldServerForDimension(targetDimId);
        if (targetWorld == null) return;
        double savedMx = player.managedPosX;
        double savedMz = player.managedPosZ;
        player.managedPosX = targetX;
        player.managedPosZ = targetZ;
        try {
            targetWorld.getPlayerManager().removePlayer(player);
        } catch (Exception e) {
            SpireMod.LOGGER.warn("Pre-cleanup of dim {} PlayerManager residual failed: {}",
                targetDimId, e.getMessage());
        }
        player.managedPosX = savedMx;
        player.managedPosZ = savedMz;
    }

    /** 发放开局装备：银剑（带锋利词条）+ 铁套四件。 */
    private void grantStarterGear(EntityPlayer player) {
        ItemStack sword = new ItemStack(Item.swordSilver, 1);
        WeaponAffix sharpEdge = AffixRegistry.getById("sharp_edge");
        if (sharpEdge != null) {
            AffixManager.applyAffix(sword, sharpEdge);
        }
        if (!player.inventory.addItemStackToInventory(sword)) {
            SpireMod.LOGGER.warn("Failed to add starter sword to {}'s inventory.", player.getEntityName());
        }

        // armorInventory index: 0=boots, 1=leggings, 2=chestplate, 3=helmet
        player.inventory.armorInventory[3] = new ItemStack(Item.helmetIron, 1);
        player.inventory.armorInventory[2] = new ItemStack(Item.plateIron, 1);
        player.inventory.armorInventory[1] = new ItemStack(Item.legsIron, 1);
        player.inventory.armorInventory[0] = new ItemStack(Item.bootsIron, 1);

        SpireMod.LOGGER.info("Granted starter gear (silver sword + iron armor) to {}", player.getEntityName());
    }

    /** 发放 1 个起始遗物（从 StarterRelics 三选一） */
    private void grantStarterRelic(EntityPlayer player, DungeonState state) {
        String[] starterIds = {"ring_of_power", "armor_fragment", "vampire_fang"};
        String picked = starterIds[new Random().nextInt(starterIds.length)];
        state.relicIds.add(picked);
        SpireMod.LOGGER.info("Granted starter relic {} to {}", picked, player.getEntityName());
    }

    /** 在虚空世界生成 (2R+1)×(2R+1) 石头战斗平台 */
    private void generatePlatform(World world) {
        int blockId = Block.stone.blockID;
        for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
            for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                world.setBlock(dx, PLATFORM_Y, dz, blockId);
            }
        }
        SpireMod.LOGGER.info("Combat platform generated: {}x{} at y={}",
            (PLATFORM_RADIUS * 2 + 1), (PLATFORM_RADIUS * 2 + 1), PLATFORM_Y);
    }

    /** 传送玩家返回 run 开始前的原坐标；如无记录则返回主世界出生点 */
    public void returnToOverworld(EntityPlayer player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ReturnLocation loc = pendingReturns.remove(player.getUniqueID());
        if (loc == null) {
            SpireMod.LOGGER.warn("No return location for {}; defaulting to overworld.", player.getEntityName());
            if (player.dimension != 0) {
                cleanPlayerManagerResidual(serverPlayer, 0, player.posX, player.posZ);
                serverPlayer.travelToDimension(0);
                clearPortalRubble(player.worldObj,
                    (int) player.posX, (int) player.posY, (int) player.posZ, 6);
            }
            return;
        }
        if (player.dimension != loc.dimensionId) {
            cleanPlayerManagerResidual(serverPlayer, loc.dimensionId, loc.x, loc.z);
            serverPlayer.travelToDimension(loc.dimensionId);
            clearPortalRubble(player.worldObj,
                (int) player.posX, (int) player.posY, (int) player.posZ, 6);
        }
        serverPlayer.travelInsideDimension(loc.x, loc.y, loc.z);
        SpireMod.LOGGER.info("Player {} returned to dim {} ({}, {}, {})",
            player.getEntityName(), loc.dimensionId, loc.x, loc.y, loc.z);
    }

    /** 玩家登录: 若有未结束的 run，重建遗物效果缓存 */
    public void onPlayerLoggedIn(ServerPlayer player) {
        DungeonState state = serverStates.get(player.getUniqueID());
        if (state != null && state.isRunActive && !state.relicIds.isEmpty()) {
            RelicManager.INSTANCE.recalculateEffects(player, state.relicIds);
            SpireMod.LOGGER.info("Restored relic effects for {} on login ({} relics)",
                player.getEntityName(), state.relicIds.size());
        }
    }

    /** 玩家重生: 清理 run 状态并传送回原坐标 */
    public void onPlayerRespawn(ServerPlayer player) {
        if (serverStates.containsKey(player.getUniqueID()) || pendingReturns.containsKey(player.getUniqueID())) {
            endRun(player);
            returnToOverworld(player);
            SpireMod.LOGGER.info("Player {} respawned after SpireCraft run; returned to overworld.",
                player.getEntityName());
        }
    }

    /** 玩家在地图上选择节点: MAP -> COMBAT|REST|EVENT|SHOP|TREASURE|BOSS */
    public void selectNode(EntityPlayer player, int nodeId) {
        DungeonState state = getState(player);
        if (state == null || state.phase != DungeonPhase.MAP) return;

        MapNode node = state.floorMap.getNode(nodeId);
        if (node == null || !node.accessible || node.completed) return;

        state.selectedNodeId = nodeId;
        state.currentNodeId = nodeId;

        DungeonPhase targetPhase = mapRoomTypeToPhase(node.type);
        state.phase = targetPhase;

        prepareRoomContent(player, state, node);

        if (targetPhase.isCombat()) {
            top.limingzxc.spire.room.RoomDispatcher.dispatch(player, node);
        }

        node.completed = true;
        syncToClient(player);
    }

    /** 完成当前房间: COMBAT->REWARD, BOSS->REWARD, 其他->MAP */
    public void completeRoom(EntityPlayer player) {
        DungeonState state = getState(player);
        if (state == null) return;

        DungeonPhase prevPhase = state.phase;

        if (prevPhase == DungeonPhase.BOSS || prevPhase == DungeonPhase.COMBAT) {
            state.phase = DungeonPhase.REWARD;
            generateRewardOptions(state);
        } else {
            state.floorMap.unlockNextNodes(state.currentNodeId);
            state.currentNodeId = -1;
            state.selectedNodeId = -1;
            state.completedNodeCount++;
            state.phase = DungeonPhase.MAP;
        }

        syncToClient(player);
    }

    /** 从 REWARD 返回地图: REWARD -> MAP（普通战斗）或 VICTORY/FLOOR_COMPLETE（BOSS） */
    public void returnToMap(EntityPlayer player) {
        DungeonState state = getState(player);
        if (state == null || state.phase != DungeonPhase.REWARD) return;

        MapNode currentNode = state.floorMap != null ? state.floorMap.getNode(state.currentNodeId) : null;
        boolean isBoss = currentNode != null && currentNode.type == RoomType.BOSS;

        state.floorMap.unlockNextNodes(state.currentNodeId);
        state.currentNodeId = -1;
        state.selectedNodeId = -1;
        state.completedNodeCount++;
        state.pendingRewardRelics.clear();

        if (isBoss) {
            if (state.currentFloor >= 3) {
                state.phase = DungeonPhase.VICTORY;
                state.floorMap = null;
                SpireMod.LOGGER.info("Player {} VICTORY!", player.getEntityName());
            } else {
                state.phase = DungeonPhase.FLOOR_COMPLETE;
                advanceFloor(player);
                return;
            }
        } else {
            state.phase = DungeonPhase.MAP;
        }

        syncToClient(player);
    }

    /** 推进到下一层: FLOOR_COMPLETE -> MAP */
    public void advanceFloor(EntityPlayer player) {
        DungeonState state = getState(player);
        if (state == null) return;

        state.advanceFloor();
        state.floorMap = new FloorMap(state.currentFloor);
        state.phase = DungeonPhase.MAP;

        syncToClient(player);
        SpireMod.LOGGER.info("Player {} advanced to floor {}",
            player.getEntityName(), state.currentFloor);
    }

    /** 结束 run（死亡/放弃）: 任意阶段 -> RUN_END */
    public void endRun(EntityPlayer player) {
        DungeonState state = serverStates.remove(player.getUniqueID());
        if (state != null) {
            state.phase = DungeonPhase.RUN_END;
            state.isRunActive = false;
            SpireMod.LOGGER.info("Run ended for {}", player.getEntityName());
        }
        RelicManager.INSTANCE.clearPlayer(player);
        top.limingzxc.spire.combat.CombatManager.getInstance().cleanupCombat(player);
        syncToClient(player);
    }

    // ======================== 房间内容生成（服务端） ========================

    private void prepareRoomContent(EntityPlayer player, DungeonState state, MapNode node) {
        state.pendingRewardRelics.clear();
        state.pendingShopRelics.clear();
        state.pendingTreasureRelic = null;
        state.pendingTreasureGold = 0;
        state.pendingEventId = -1;

        switch (node.type) {
            case SHOP: {
                Set<String> owned = new HashSet<>(state.relicIds);
                List<Relic> batch = RelicRegistry.getRandomBatch(state.currentFloor, owned, 3, new Random());
                for (Relic r : batch) state.pendingShopRelics.add(r.relicId);
                break;
            }
            case EVENT: {
                state.pendingEventId = new Random().nextInt(3);
                break;
            }
            case TREASURE: {
                Set<String> owned = new HashSet<>(state.relicIds);
                Relic relic = RelicRegistry.getRandom(state.currentFloor, owned, new Random());
                int goldReward = 30 + new Random().nextInt(50);
                state.pendingTreasureGold = goldReward;
                state.gold += goldReward;
                if (relic != null) {
                    state.pendingTreasureRelic = relic.relicId;
                    state.relicIds.add(relic.relicId);
                    RelicManager.INSTANCE.recalculateEffects(player, state.relicIds);
                }
                break;
            }
            default:
                break;
        }
    }

    private void generateRewardOptions(DungeonState state) {
        state.pendingRewardRelics.clear();
        Set<String> owned = new HashSet<>(state.relicIds);
        List<Relic> batch = RelicRegistry.getRandomBatch(state.currentFloor, owned, 3, new Random());
        for (Relic r : batch) state.pendingRewardRelics.add(r.relicId);
    }

    // ======================== 房间操作处理（服务端） ========================

    public void handleRoomAction(EntityPlayer player, String action, String payload) {
        DungeonState state = getState(player);
        if (state == null) return;

        switch (action) {
            case "rest_heal":
                if (state.phase != DungeonPhase.REST) return;
                doRestHeal(player);
                completeRoom(player);
                break;
            case "leave_rest":
                if (state.phase != DungeonPhase.REST) return;
                completeRoom(player);
                break;
            case "forge":
                if (state.phase != DungeonPhase.REST) return;
                doForge(player, payload);
                syncToClient(player);
                break;
            case "buy":
                if (state.phase != DungeonPhase.SHOP) return;
                doBuy(player, state, payload);
                syncToClient(player);
                break;
            case "leave_shop":
                if (state.phase != DungeonPhase.SHOP) return;
                completeRoom(player);
                break;
            case "event_a":
                if (state.phase != DungeonPhase.EVENT) return;
                doEventChoice(player, state, state.pendingEventId, true);
                completeRoom(player);
                break;
            case "event_b":
                if (state.phase != DungeonPhase.EVENT) return;
                completeRoom(player);
                break;
            case "treasure_continue":
                if (state.phase != DungeonPhase.TREASURE) return;
                completeRoom(player);
                break;
            case "reward":
                if (state.phase != DungeonPhase.REWARD) return;
                doSelectReward(player, state, payload);
                break;
            case "victory_return":
                endRun(player);
                returnToOverworld(player);
                break;
            case "abandon":
                SpireMod.LOGGER.info("Player {} abandoned run.", player.getEntityName());
                endRun(player);
                returnToOverworld(player);
                break;
            default:
                SpireMod.LOGGER.warn("Unknown room action: {}", action);
                break;
        }
    }

    private void doRestHeal(EntityPlayer player) {
        float maxHealth = player.getMaxHealth();
        float healAmount = maxHealth * 0.3f;
        player.setHealth(Math.min(player.getHealth() + healAmount, maxHealth));
        SpireMod.LOGGER.info("Player {} rested: healed {} HP", player.getEntityName(), healAmount);
    }

    private void doForge(EntityPlayer player, String affixId) {
        WeaponAffix affix = AffixRegistry.getById(affixId);
        if (affix == null) return;

        ItemStack weapon = player.getHeldItemStack();
        if (weapon == null) {
            SpireMod.LOGGER.info("No weapon in hand for forging.");
            return;
        }

        int materialCount = countMaterial(player, affix.materialName);
        if (materialCount < affix.materialCount) {
            SpireMod.LOGGER.info("Not enough materials for {} (need {} {})",
                affix.name, affix.materialCount, affix.materialName);
            return;
        }

        consumeMaterial(player, affix.materialName, affix.materialCount);
        AffixManager.applyAffix(weapon, affix);
        SpireMod.LOGGER.info("Player {} forged {} on weapon", player.getEntityName(), affix.name);
    }

    private void doBuy(EntityPlayer player, DungeonState state, String relicId) {
        if (!state.pendingShopRelics.contains(relicId)) return;
        Relic relic = RelicRegistry.getById(relicId);
        if (relic == null) return;

        int price = relic.getGoldValue();
        if (state.gold < price) return;

        state.gold -= price;
        state.relicIds.add(relicId);
        state.pendingShopRelics.remove(relicId);
        RelicManager.INSTANCE.recalculateEffects(player, state.relicIds);
        SpireMod.LOGGER.info("Player {} purchased {} for {} gold", player.getEntityName(), relic.name, price);
    }

    private void doEventChoice(EntityPlayer player, DungeonState state, int eventId, boolean optionA) {
        if (!optionA) return;

        Set<String> owned = new HashSet<>(state.relicIds);
        Random random = new Random();

        switch (eventId) {
            case 0: // 幽灵商贩: 支付30%生命 → 罕见遗物
                float cost = player.getMaxHealth() * 0.3f;
                player.setHealth(Math.max(1, player.getHealth() - cost));
                grantRandomRelic(player, state, owned, random);
                break;
            case 1: // 元素祭坛: 献祭护甲 → 抗性遗物（无护甲则不发放）
                boolean hasArmor = false;
                for (int i = 0; i < 4; i++) {
                    if (player.getCurrentArmor(i) != null) {
                        player.setCurrentItemOrArmor(i, null);
                        hasArmor = true;
                        break;
                    }
                }
                if (hasArmor) {
                    grantRandomRelic(player, state, owned, random);
                } else {
                    SpireMod.LOGGER.info("Player {} has no armor; element altar grants nothing.",
                        player.getEntityName());
                }
                break;
            case 2: // 暗影契约: 扣除4点生命 → 强力遗物
                player.setHealth(Math.max(1, player.getHealth() - 4));
                grantRandomRelic(player, state, owned, random);
                break;
            default:
                break;
        }
    }

    private void grantRandomRelic(EntityPlayer player, DungeonState state, Set<String> owned, Random random) {
        Relic relic = RelicRegistry.getRandom(state.currentFloor, owned, random);
        if (relic != null) {
            state.relicIds.add(relic.relicId);
            RelicManager.INSTANCE.recalculateEffects(player, state.relicIds);
        }
    }

    private void doSelectReward(EntityPlayer player, DungeonState state, String relicId) {
        if (!state.pendingRewardRelics.contains(relicId)) return;
        state.relicIds.add(relicId);
        RelicManager.INSTANCE.recalculateEffects(player, state.relicIds);
        SpireMod.LOGGER.info("Player {} selected reward relic: {}", player.getEntityName(), relicId);
        returnToMap(player);
    }

    private int countMaterial(EntityPlayer player, String materialName) {
        int count = 0;
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (stack != null && stack.getDisplayName() != null &&
                stack.getDisplayName().contains(materialName)) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    private void consumeMaterial(EntityPlayer player, String materialName, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.inventory.mainInventory.length && remaining > 0; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (stack != null && stack.getDisplayName() != null &&
                stack.getDisplayName().contains(materialName)) {
                int toRemove = Math.min(remaining, stack.stackSize);
                stack.stackSize -= toRemove;
                remaining -= toRemove;
                if (stack.stackSize <= 0) {
                    player.inventory.mainInventory[i] = null;
                }
            }
        }
    }

    // ======================== Tick 驱动 ========================

    public void onPlayerTick(EntityPlayer player) {
        // 服务端 tick: 无额外逻辑（战斗由 CombatManager 事件驱动）
    }

    public void onClientTick(Minecraft client) {
        if (client.thePlayer == null) return;

        if (pendingScreenPhase != null) {
            DungeonPhase phase = pendingScreenPhase;
            pendingScreenPhase = null;
            openScreenForPhase(client, phase);
            return;
        }

        // 防卡死：若当前 phase 需要GUI但玩家按 ESC 关闭了屏幕，自动重新打开
        DungeonPhase currentPhase = clientState.phase;
        if (currentPhase.isGuiRoom() || currentPhase == DungeonPhase.MAP || currentPhase == DungeonPhase.VICTORY) {
            if (client.currentScreen == null) {
                openScreenForPhase(client, currentPhase);
            }
        }
    }

    private void openScreenForPhase(Minecraft client, DungeonPhase phase) {
        switch (phase) {
            case MAP:
                if (!(client.currentScreen instanceof FloorMapScreen)) {
                    client.displayGuiScreen(new FloorMapScreen());
                }
                break;
            case REST:
                client.displayGuiScreen(new RestScreen());
                break;
            case EVENT:
                client.displayGuiScreen(new EventScreen());
                break;
            case SHOP:
                client.displayGuiScreen(new ShopScreen());
                break;
            case TREASURE:
                client.displayGuiScreen(new TreasureScreen());
                break;
            case REWARD:
                client.displayGuiScreen(new RewardScreen());
                break;
            case VICTORY:
                if (!(client.currentScreen instanceof VictoryScreen)) {
                    client.displayGuiScreen(new VictoryScreen());
                }
                break;
            case COMBAT:
            case BOSS:
            case IDLE:
            case RUN_END:
                client.displayGuiScreen(null);
                break;
            default:
                break;
        }
    }

    // ======================== 客户端同步回调 ========================

    /** PacketSyncState.apply 在客户端调用：更新 clientState 后通知 GUI 切换 */
    public void onSyncApplied(DungeonPhase oldPhase, DungeonPhase newPhase) {
        if (oldPhase != newPhase) {
            pendingScreenPhase = newPhase;
        } else if (newPhase.isGuiRoom()) {
            // 同阶段 GUI 房间刷新（如商店购买后刷新）
            pendingScreenPhase = newPhase;
        }
    }

    public DungeonState getClientState() {
        return clientState;
    }

    // ======================== NBT 持久化 ========================

    private static final String NBT_KEY = "SpireCraft";
    private static final String NBT_PLAYERS = "Players";
    private static final String NBT_RETURNS = "PendingReturns";
    private static final String NBT_UUID = "UUID";
    private static final String NBT_PHASE = "Phase";
    private static final String NBT_FLOOR = "Floor";
    private static final String NBT_CURRENT_NODE = "CurrentNode";
    private static final String NBT_SELECTED_NODE = "SelectedNode";
    private static final String NBT_GOLD = "Gold";
    private static final String NBT_RELICS = "Relics";
    private static final String NBT_COMPLETED = "CompletedCount";
    private static final String NBT_RUN_ACTIVE = "RunActive";
    private static final String NBT_FLOOR_MAP = "FloorMap";
    private static final String NBT_SHOP_RELICS = "ShopRelics";
    private static final String NBT_REWARD_RELICS = "RewardRelics";
    private static final String NBT_TREASURE_RELIC = "TreasureRelic";
    private static final String NBT_TREASURE_GOLD = "TreasureGold";
    private static final String NBT_EVENT_ID = "EventId";
    private static final String NBT_RETURN_DIM = "ReturnDim";
    private static final String NBT_RETURN_X = "ReturnX";
    private static final String NBT_RETURN_Y = "ReturnY";
    private static final String NBT_RETURN_Z = "ReturnZ";

    public void writeToNBT(NBTTagCompound rootNbt) {
        NBTTagCompound spireNbt = new NBTTagCompound();
        NBTTagList playerList = new NBTTagList();

        for (Map.Entry<UUID, DungeonState> entry : serverStates.entrySet()) {
            DungeonState s = entry.getValue();
            NBTTagCompound playerNbt = new NBTTagCompound();
            playerNbt.setString(NBT_UUID, entry.getKey().toString());
            playerNbt.setString(NBT_PHASE, s.phase.name());
            playerNbt.setInteger(NBT_FLOOR, s.currentFloor);
            playerNbt.setInteger(NBT_CURRENT_NODE, s.currentNodeId);
            playerNbt.setInteger(NBT_SELECTED_NODE, s.selectedNodeId);
            playerNbt.setInteger(NBT_GOLD, s.gold);
            playerNbt.setInteger(NBT_COMPLETED, s.completedNodeCount);
            playerNbt.setBoolean(NBT_RUN_ACTIVE, s.isRunActive);

            NBTTagList relicList = new NBTTagList();
            for (String relicId : s.relicIds) {
                relicList.appendTag(new NBTTagString("", relicId));
            }
            playerNbt.setTag(NBT_RELICS, relicList);

            if (s.floorMap != null) {
                playerNbt.setTag(NBT_FLOOR_MAP, s.floorMap.writeToNBT());
            }

            NBTTagList shopList = new NBTTagList();
            for (String id : s.pendingShopRelics) shopList.appendTag(new NBTTagString("", id));
            playerNbt.setTag(NBT_SHOP_RELICS, shopList);

            NBTTagList rewardList = new NBTTagList();
            for (String id : s.pendingRewardRelics) rewardList.appendTag(new NBTTagString("", id));
            playerNbt.setTag(NBT_REWARD_RELICS, rewardList);

            if (s.pendingTreasureRelic != null) {
                playerNbt.setString(NBT_TREASURE_RELIC, s.pendingTreasureRelic);
            }
            playerNbt.setInteger(NBT_TREASURE_GOLD, s.pendingTreasureGold);
            playerNbt.setInteger(NBT_EVENT_ID, s.pendingEventId);

            playerList.appendTag(playerNbt);
        }

        spireNbt.setTag(NBT_PLAYERS, playerList);

        NBTTagList returnList = new NBTTagList();
        for (Map.Entry<UUID, ReturnLocation> entry : pendingReturns.entrySet()) {
            NBTTagCompound r = new NBTTagCompound();
            r.setString(NBT_UUID, entry.getKey().toString());
            r.setInteger(NBT_RETURN_DIM, entry.getValue().dimensionId);
            r.setDouble(NBT_RETURN_X, entry.getValue().x);
            r.setDouble(NBT_RETURN_Y, entry.getValue().y);
            r.setDouble(NBT_RETURN_Z, entry.getValue().z);
            returnList.appendTag(r);
        }
        spireNbt.setTag(NBT_RETURNS, returnList);

        rootNbt.setTag(NBT_KEY, spireNbt);
    }

    public void readFromNBT(NBTTagCompound rootNbt) {
        serverStates.clear();
        pendingReturns.clear();

        if (!rootNbt.hasKey(NBT_KEY)) return;
        NBTTagCompound spireNbt = rootNbt.getCompoundTag(NBT_KEY);

        NBTTagList playerList = spireNbt.getTagList(NBT_PLAYERS);
        for (int i = 0; i < playerList.tagCount(); i++) {
            NBTTagCompound playerNbt = (NBTTagCompound) playerList.tagAt(i);

            UUID uuid;
            try {
                uuid = UUID.fromString(playerNbt.getString(NBT_UUID));
            } catch (IllegalArgumentException e) {
                continue;
            }

            DungeonState s = new DungeonState();
            try {
                s.phase = DungeonPhase.valueOf(playerNbt.getString(NBT_PHASE));
            } catch (IllegalArgumentException e) {
                SpireMod.LOGGER.warn("Unknown phase {} in NBT; defaulting to IDLE.", playerNbt.getString(NBT_PHASE));
                s.phase = DungeonPhase.IDLE;
            }
            s.currentFloor = playerNbt.getInteger(NBT_FLOOR);
            s.currentNodeId = playerNbt.getInteger(NBT_CURRENT_NODE);
            s.selectedNodeId = playerNbt.getInteger(NBT_SELECTED_NODE);
            s.gold = playerNbt.getInteger(NBT_GOLD);
            s.completedNodeCount = playerNbt.getInteger(NBT_COMPLETED);
            s.isRunActive = playerNbt.getBoolean(NBT_RUN_ACTIVE);

            NBTTagList relicList = playerNbt.getTagList(NBT_RELICS);
            for (int j = 0; j < relicList.tagCount(); j++) {
                s.relicIds.add(((NBTTagString) relicList.tagAt(j)).data);
            }

            if (playerNbt.hasKey(NBT_FLOOR_MAP)) {
                s.floorMap = FloorMap.readFromNBT(playerNbt.getCompoundTag(NBT_FLOOR_MAP));
            }

            NBTTagList shopList = playerNbt.getTagList(NBT_SHOP_RELICS);
            for (int j = 0; j < shopList.tagCount(); j++) {
                s.pendingShopRelics.add(((NBTTagString) shopList.tagAt(j)).data);
            }

            NBTTagList rewardList = playerNbt.getTagList(NBT_REWARD_RELICS);
            for (int j = 0; j < rewardList.tagCount(); j++) {
                s.pendingRewardRelics.add(((NBTTagString) rewardList.tagAt(j)).data);
            }

            if (playerNbt.hasKey(NBT_TREASURE_RELIC)) {
                s.pendingTreasureRelic = playerNbt.getString(NBT_TREASURE_RELIC);
            }
            s.pendingTreasureGold = playerNbt.getInteger(NBT_TREASURE_GOLD);
            s.pendingEventId = playerNbt.getInteger(NBT_EVENT_ID);

            serverStates.put(uuid, s);
        }

        NBTTagList returnList = spireNbt.getTagList(NBT_RETURNS);
        for (int i = 0; i < returnList.tagCount(); i++) {
            NBTTagCompound r = (NBTTagCompound) returnList.tagAt(i);
            try {
                UUID uuid = UUID.fromString(r.getString(NBT_UUID));
                pendingReturns.put(uuid, new ReturnLocation(
                    r.getInteger(NBT_RETURN_DIM),
                    r.getDouble(NBT_RETURN_X),
                    r.getDouble(NBT_RETURN_Y),
                    r.getDouble(NBT_RETURN_Z)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        SpireMod.LOGGER.info("Restored {} dungeon states and {} pending returns from NBT.",
            serverStates.size(), pendingReturns.size());
    }

    // ======================== 网络同步 ========================

    private void syncToClient(EntityPlayer player) {
        DungeonState state = getState(player);
        ServerPlayer serverPlayer = (ServerPlayer) player;
        if (state == null) {
            Network.sendToClient(serverPlayer, new SpireNetwork.PacketSyncState(new DungeonState()));
        } else {
            Network.sendToClient(serverPlayer, new SpireNetwork.PacketSyncState(state));
        }
    }

    // ======================== 工具方法 ========================

    private DungeonPhase mapRoomTypeToPhase(RoomType type) {
        switch (type) {
            case COMBAT: case ELITE:  return DungeonPhase.COMBAT;
            case BOSS:                return DungeonPhase.BOSS;
            case REST:                return DungeonPhase.REST;
            case EVENT:               return DungeonPhase.EVENT;
            case SHOP:                return DungeonPhase.SHOP;
            case TREASURE:             return DungeonPhase.TREASURE;
            default:                  return DungeonPhase.COMBAT;
        }
    }

    public boolean isBossNode(int nodeId) {
        if (clientState.floorMap == null) return false;
        MapNode node = clientState.floorMap.getNode(nodeId);
        return node != null && node.type == RoomType.BOSS;
    }
}
