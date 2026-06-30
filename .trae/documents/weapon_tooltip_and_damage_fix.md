# Plan: 武器词条 Tooltip + 怪物伤害抵抗修复

## Context

用户反馈两个问题：
1. 鼠标移到武器上时没有显示词条信息，按住 Shift 也没有显示词条的具体作用
2. 铁剑打不了某些怪物（MITE 的伤害类型抵抗机制：亡灵类怪物免疫非银质武器）

## 实现方案

### Part 1: 武器词条 Tooltip 显示

**原理**：RIC 提供 `Handlers.Tooltip` + `ITooltipListener`（CLIENT only），其中 `detailed` 参数在玩家按住 Shift 时自动为 true。

**改动文件**：
- `RelicEffect.java` — 新增 `getDescription()` 方法，将 effect 数据转为人类可读的中文描述
- `SpireEvents.java` — 注册 `ITooltipListener`，在 `onTooltipBody` 中读取武器 NBT 词条并追加到 tooltip

**描述文本生成规则**（在 `RelicEffect.getDescription()` 中实现）：

| 词条 | trigger+target | mode+value | 描述 |
|------|---------------|-----------|------|
| sharp_edge | ON_ATTACK / MELEE_DAMAGE | ADD +3.0 | "近战伤害 +3" |
| warding | ON_DAMAGE_TAKEN / DAMAGE_REDUCTION | MULTIPLY 0.85 | "受到伤害 ×0.85" |
| vampiric | ON_KILL / HEAL_ON_KILL | ADD +1.0 | "击杀回复 1 生命" |

**Tooltip 显示逻辑**：
- 非 Shift：追加 `"§b词条: 锋利"`（仅名称）
- 按住 Shift：追加 `"§b锋利: §e近战伤害 +3"`（名称 + 描述）
- 无词条的武器不追加任何内容

### Part 2: 修复铁剑无法伤害某些怪物

**原理**：MITE 中亡灵类怪物（EntityWight、EntityGhoul、EntityAncientBoneLord、EntityNightwing 等）免疫非银质武器伤害。当前开局发放的铁剑（`Item.swordIron`）无法伤害这些怪物。

**改动文件**：
- `DungeonManager.java` — `grantStarterGear` 方法中将 `Item.swordIron` 改为 `Item.swordSilver`

**验证步骤**：编译前需确认 MITE 1.6.4 中 `Item.swordSilver` 字段存在（通过 javap 验证）。

## 验证方式

1. `gradle build` 编译通过
2. 进入游戏，右键传送门开始 run
3. 打开背包，鼠标悬停在开局银剑上 → 应显示 "词条: 锋利"
4. 按住 Shift 悬停 → 应显示 "锋利: 近战伤害 +3"
5. 进入战斗，用银剑攻击亡灵类敌人 → 应能正常造成伤害