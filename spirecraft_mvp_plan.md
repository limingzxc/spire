---
name: SpireCraft MVP — 实施计划
overview: 杀戮尖塔风格 roguelike 爬塔模组，基于 RustedIronCore 1.5.5 API，兼容 Minecraft 1.6.4 MCP，融入 MITE 独有怪物体系与硬核特性。
todos:
  - id: phase0-build
    content: 创建最小 SpireMod.java + 验证 Gradle 编译通过
    status: completed
  - id: phase1-spiremod
    content: 完成 SpireMod.java（logger, 调用各模块 init）
    status: completed
  - id: phase1-events
    content: 完成 SpireEvents.java（注册所有 RIC Handler 空壳）
    status: completed
  - id: phase1-dungeon-mgr
    content: 完成 DungeonManager.java（状态转换 + WorldInfo 持久化 + 死亡重置）
    status: completed
  - id: phase1-floormap
    content: 完成 FloorMap.java（路径生成算法）
    status: completed
  - id: phase1-floormap-gui
    content: 完成 FloorMapScreen.java（渲染 + 交互）
    status: completed
  - id: phase1-registry
    content: 完成 SpireRegistryInit.java + SpireDimensionProvider.java + 传送门
    status: completed
  - id: phase1-network
    content: 完成 SpireNetwork.java（包注册 + 同步）
    status: completed
  - id: phase2-enemy
    content: 完成 EnemyTemplate.java + 12个MVP敌人配置（融入MITE独有怪物）
    status: completed
  - id: phase2-combat
    content: 完成 CombatManager.java + CombatRoom.java
    status: completed
  - id: phase2-relic-core
    content: 完成 Relic.java + RelicEffect.java + RelicRegistry.java
    status: completed
  - id: phase2-relic-mgr
    content: 完成 RelicManager.java + RIC Handler 集成
    status: completed
  - id: phase2-relic-mvp
    content: 完成 StarterRelics.java（3个遗物）
    status: completed
  - id: phase2-reward
    content: 完成 RewardScreen.java
    status: completed
  - id: phase3-dispatcher
    content: 完成 RoomDispatcher.java
    status: completed
  - id: phase3-rest
    content: 完成 RestRoom.java + RestScreen.java
    status: completed
  - id: phase3-event
    content: 完成 EventRoom.java + EventScreen.java（3个事件）
    status: completed
  - id: phase3-shop
    content: 完成 ShopRoom.java + ShopScreen.java
    status: completed
  - id: phase3-treasure
    content: 完成 TreasureRoom.java + TreasureScreen.java
    status: completed
  - id: phase3-affix
    content: 完成 AffixManager.java + AffixRegistry.java + ForgeScreen.java
    status: completed
  - id: phase3-boss
    content: 完成 BOSS战斗逻辑 + VictoryScreen.java
    status: completed
  - id: phase3-polish
    content: 全流程端到端测试 + 修复边缘情况 + 客户端/服务端架构修复
    status: completed
  - id: phase-compile-fix
    content: 修复 21 个编译错误（1.6.4 MCP API 差异适配）
    status: completed
  - id: phase-runtime-fix
    content: 修复客户端/服务端架构缺陷（同步包/房间操作/战斗反射/玩家查找）
    status: completed
  - id: phase-fix-dimension
    content: 接入尖塔维度系统（注册维度 + 传送玩家 + 生成战斗平台 + 返回主世界）
    status: completed
  - id: phase-fix-death-event
    content: 注册玩家死亡事件，调用 CombatManager.onPlayerDied + cleanupCombat
    status: completed
  - id: phase-fix-armor-relic
    content: 注册 onArmorProtectionModify listener，修复护甲碎片遗物无效
    status: completed
  - id: phase-fix-nbt-persistence
    content: 完善 NBT 持久化（floorMap + pending* 字段），重启后可继续
    status: completed
  - id: phase-fix-relic-cache
    content: readFromNBT 后调用 RelicManager.recalculateEffects 重建缓存
    status: completed
  - id: phase-fix-start-node
    content: 修复 FloorMap 起点节点类型（TREASURE → START 或设为不可选）
    status: completed
  - id: phase-fix-event-logic
    content: 修复事件文案与逻辑不符（元素祭坛/暗影契约）
    status: completed
  - id: phase-fix-dead-code
    content: 清理 RoomDispatcher 非战斗分支死代码 + 未使用的 Room.enter 方法
    status: completed
  - id: phase-audit-fix-mob-names
    content: 修正 4 个不存在的怪物类名（Wraith→Wight, IceElemental→Ghoul, Nightmare→Nightwing, Demon→DemonSpider）
    status: completed
  - id: phase-audit-fix-spawn-fallback
    content: CombatManager 敌人生成失败时 fallback 到 EntityZombie + setAttackTarget
    status: completed
  - id: phase-audit-fix-teleport-rollback
    content: teleportToSpire 返回 boolean，失败时 startNewRun 回滚 state
    status: completed
  - id: phase-audit-fix-starter-relic
    content: startNewRun 发放 1 个随机起始遗物
    status: completed
  - id: phase-audit-fix-affix-onkill
    content: onEnemyDied 链式调用 AffixManager.applyCombatModifiers（吸血词条生效）
    status: completed
  - id: phase-audit-fix-abandon
    content: 新增 abandon 房间动作 + FloorMapScreen 放弃按钮
    status: completed
  - id: phase-audit-fix-logic-bugs
    content: selectNode completed 顺序 + 元素祭坛护甲检查 + valueOf try-catch + ItemSpirePortal 维度检查
    status: completed
  - id: phase-flow-fix-double-spawn
    content: 修复 CombatManager fallback 敌人双重 spawnEntityInWorld + 回退僵尸误套模板倍率
    status: completed
  - id: phase-flow-fix-boss-hp
    content: 修复 BOSS healthMultiplier 无效（setHealth 被 maxHealth 截断）→ 反射修改 maxHealth 字段
    status: completed
  - id: phase-flow-fix-relic-pool
    content: 扩充遗物池（3→14 个），新增 SpireRelics.java 注册 11 个通用遗物
    status: completed
  - id: phase-flow-fix-gui-stuck
    content: 修复 ESC 关闭 GUI 后卡死（onClientTick 检测 phase 需GUI但 currentScreen=null 时重开）
    status: completed
  - id: phase-flow-fix-portal-recipe
    content: 添加 spire_portal 合成配方（4 黑曜石 + 1 钻石）通过 Handlers.Crafting 注册
    status: completed
  - id: phase-flow-fix-starter-gear
    content: startNewRun 发放开局装备（铁剑+锋利词条+铁套），最小验证方案
    status: completed
  - id: phase-fix-dim-teleport-npe
    content: 修复右键传送门崩溃（transferEntityToWorld 对自定义维度调 getEntrancePortalLocation 返回 null → NPE）+ 清理自动生成的下界传送门框架
    status: completed
  - id: phase-fix-nbttagstring-construct
    content: 修复 startNewRun 崩溃（NBTTagString 单参构造把参数当 name、data 为 null → ItemStack.copy 抛 Empty string not allowed）。5 处 new NBTTagString(x) 改为双参 new NBTTagString("", x)，getTagList 加 hasKey 守卫
    status: completed
  - id: phase-fix-dim-weather-npe
    content: 修复进入尖塔维度后客户端渲染崩溃（World.isStormingAt events null）。SpireDimensionProvider 覆写 calcSunriseSunsetColors 返回 null，参照 WorldProviderEnd 规避非主世界维度天气计算 NPE
    status: completed
  - id: phase-fix-player-manager-residual
    content:修复 travelToDimension 撞尖塔 PlayerManager 残留（client crash 后未清理）抛 "already is in chunk" 崩溃。抽 cleanPlayerManagerResidual helper，teleportToSpire 与 returnToOverworld 的 travelToDimension 前重置 managedPos 到目标坐标并主动 removePlayer 清理残留
    status: completed
isProject: false
---

# SpireCraft MVP — 实施计划

> **RIC API 参考文档**: [rusted-iron-core-api-1.5.5.md](docs/rusted-iron-core-api-1.5.5.md)

## 当前状态

**全部完成**：52 个子任务全部完成，编译通过（`BUILD SUCCESSFUL`），可进入 MITE 实机验证。

---

## 已完成内容

### 核心循环

右键传送门 → `startNewRun`（传送至尖塔维度 + 生成战斗平台 + floor=1 + gold=99 + 开局装备[铁剑+锋利词条+铁套] + 随机起始遗物）→ FloorMap 生成 → 地图选点 → 房间处理 → 奖励/返回地图 → 3 层通关 VICTORY 返回主世界 / 死亡重生返回主世界 / 主动放弃返回主世界。

### 已实现系统

| 系统 | 文件 | 说明 |
|------|------|------|
| 状态机 | DungeonManager, DungeonState, DungeonPhase | 12 阶段状态转换，服务端权威 + 客户端镜像 |
| 地图生成 | FloorMap, MapNode, RoomType | 分支-汇聚 DAG，楼层越高列越多（7/9/11 列） |
| 战斗 | CombatManager, EnemyTemplate, CombatRoom | 12 个 MITE 敌人，反射生成，事件驱动 |
| 遗物 | Relic, RelicEffect, RelicManager, RelicRegistry, StarterRelics, SpireRelics | 14 个遗物（3 开局 + 11 通用），链式效果引擎 |
| 武器词条 | WeaponAffix, AffixManager, AffixRegistry | 3 个词条，NBT 持久化 |
| 房间 | RoomDispatcher + 6 个 Room | COMBAT/REST/EVENT/SHOP/TREASURE/BOSS |
| GUI | 8 个 Screen | 服务端同步驱动屏幕切换 |
| 网络 | SpireNetwork | 3 个包：SyncState / SelectNode / RoomAction |
| 维度 | SpireDimensionProvider, VoidChunkProvider | 虚空世界 + 25×25 石头战斗平台，传送/返回完整 |
| 持久化 | WorldInfo NBT | floorMap + pending* + pendingReturns 全字段持久化 |

### 客户端/服务端架构

所有 GUI 采用"服务端权威 + `PacketRoomAction` 提交"模式：客户端从 `clientState.pending*` 读取状态，操作通过 `PacketRoomAction` 发送服务端处理，`syncToClient` 驱动屏幕切换。

### 完整文件结构

```
src/main/java/top/limingzxc/spire/
├── SpireMod.java                     # 入口
├── event/SpireEvents.java            # RIC Handler 集中注册
├── dungeon/
│   ├── DungeonManager.java           # 状态机（服务端权威）
│   ├── DungeonState.java             # 数据类
│   ├── DungeonPhase.java             # 阶段枚举
│   ├── FloorMap.java                 # 路径图生成
│   ├── MapNode.java                  # 节点数据
│   └── RoomType.java                 # 房间类型枚举
├── room/
│   ├── RoomDispatcher.java           # 路由
│   ├── CombatRoom.java
│   ├── RestRoom.java
│   ├── EventRoom.java
│   ├── ShopRoom.java
│   └── TreasureRoom.java
├── combat/
│   ├── CombatManager.java            # 战斗生命周期
│   └── EnemyTemplate.java            # 12 个 MITE 敌人配置
├── relic/
│   ├── Relic.java
│   ├── RelicEffect.java
│   ├── RelicManager.java             # 效果引擎
│   ├── RelicRegistry.java
│   └── relics/
│       ├── StarterRelics.java         # 3 个开局遗物
│       └── SpireRelics.java          # 11 个通用遗物（奖励/商店/宝箱/事件池）
├── weapon/
│   ├── WeaponAffix.java
│   ├── AffixManager.java             # NBT 读写 + 链式计算
│   └── AffixRegistry.java            # 3 个词条
├── gui/
│   ├── FloorMapScreen.java
│   ├── RewardScreen.java
│   ├── RestScreen.java
│   ├── ForgeScreen.java
│   ├── ShopScreen.java
│   ├── EventScreen.java
│   ├── TreasureScreen.java
│   └── VictoryScreen.java
├── dimension/
│   ├── SpireDimensionProvider.java    # 尖塔维度 WorldProvider
│   └── VoidChunkProvider.java         # 虚空 ChunkProvider（空 chunk + 天光）
├── network/SpireNetwork.java
└── register/
    ├── SpireRegistryInit.java
    └── ItemSpirePortal.java
```

---

## 技术参考

### 1.6.4 MCP API 适配要点

以下差异已在编译修复阶段解决，编写新代码时务必遵循：

1. **包结构扁平**：所有 `net.minecraft` 类在根包下，无子包层级。用 `import net.minecraft.EntityPlayer;` 而非 `net.minecraft.entity.player.EntityPlayer`。
2. **Item 构造函数**：无 `Item(int)` 构造。用 `super(id, modId + ":" + itemName)`，ID 通过 `Item.itemsList.length` 获取。
3. **NBTTagList**：无 `getStringTagAt(int)`，用 `((NBTTagString) tagAt(i)).data`。
4. **GuiButton 字段**：`width`/`height` 为 `protected`，子类不可直接访问。在 GUI 类中存储 `cardWidth`/`cardHeight` 实例字段替代。
5. **GUI 方法**：无 `drawHoveringText`，用 `drawString` 实现 tooltip。
6. **EntityPlayer**：用 `getHeldItemStack()` 而非 `getHeldItem()`；无 `setMaxHealth`，用 `setHealth()` 操作当前生命值。
7. **WorldProvider**：`getDimensionName()`/`isSurfaceWorld()` 为 final 不可覆写；需 `(int, String)` 构造函数调用 `super`。
8. **RIC Record 模式**：事件对象如 `PlayerRespawnEvent` 用 `event.player()` 而非 `event.getPlayer()`。
9. **Damage vs DamageSource**：`ICombatListener` 用 `Damage` 对象，通过 `getAmount()`/`setAmount()` 修改伤害。
10. **ServerPlayer**：`Network.sendToClient` 要求 `ServerPlayer` 类型，服务端 player 需 `(ServerPlayer) player` 强制转换。

### RIC API 速查

| 功能 | Handler | 关键方法 |
|------|---------|---------|
| 战斗伤害修改 | `Handlers.Combat` → `ICombatListener` | `onPlayerRawMeleeDamageModify`, `onPlayerReceiveDamageModify`, `onMobReceiveDamageModify`, `onArmorProtectionModify` |
| 怪物生成/死亡 | `Handlers.EntityEvent` → `IEntityEventListener` | `onSpawn`, `onDeath`, `onAttackEntityFrom` |
| 玩家事件 | `Handlers.PlayerEvent` → `IPlayerEventListener` | `onPlayerRespawn` |
| 常驻属性 | `Handlers.PlayerAttribute` → `IPlayerAttributeListener` | `onHealthLimitModify`, `onHungerLimitModify`, `onArmorProtectionModify` |
| Run 持久化 | `Handlers.WorldInfo` → `IWorldInfoListener` | `onNBTWrite`, `onNBTRead` |
| Tick 驱动 | `Handlers.Tick` → `ITickListener` | `onEntityPlayerTick`, `onClientTick` |
| 定时任务 | `Handlers.TimedTask` | 注册 Runnable + 延迟 tick |
| 物品提示 | `Handlers.Tooltip` → `ITooltipListener` | `onTooltipBody` |
| GUI 互动 | `Handlers.Screen` → `IScreenListener` | `onInit`, `onDraw`, `onButtonAction`, `onClose` |
| 维度注册 | `Handlers.Dimension` | 注册自定义 WorldProvider |
| 物品注册 | `MinecraftRegistry` + `IGameRegistry` | `registry.registerItem(...)` |
| 网络同步 | RIC Network | `Network.sendToClient`, `Network.sendToServer` |
| 启动回调 | `Handlers.Initialization` → `IInitializationListener` | `onClientStarted`, `onServerStarted` |

### 工程约定

- 基础包名：`top.limingzxc.spire`（与 `fml.mod.json` entrypoint 对齐）
- access widener：`spire.accesswidener`
- NBT 键名前缀：`SpireCraft` / `Spire`
- `Handlers.Combat` 链式调用顺序：RelicManager 先于 AffixManager 注册
- 客户端/服务端分离：GUI 类全在客户端，状态变更必须经 `PacketRoomAction`
