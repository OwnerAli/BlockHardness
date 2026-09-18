# BlockHardness

A Spigot library that gives blocks custom break times. It owns block breaking: it decides how long a block takes
and sends the crack animation, so consumer plugins never re-implement vanilla's mining maths.

Requires ProtocolLib.

## Mining a block

```java
BreakPlayer breakPlayer = BlockHardnessPlugin.instance
        .getBreakPlayerRegistry()
        .getBreakPlayer(player)
        .orElseThrow();

// 5 seconds flat, no vanilla drops
breakPlayer.startMining(block, 5.0, new MiningOptions(false, false));

// 5 seconds, faster with a better pickaxe / Efficiency / Haste
breakPlayer.startMining(block, 5.0, new MiningOptions(true, false));

// whatever this plugin's config says
breakPlayer.startMining(block, 5.0, MiningOptions.defaults());

// ignore the pickaxe, but let a Haste beacon help
breakPlayer.startMining(block, 5.0, MiningOptions.builder()
        .toolTier(false)
        .potionEffects(true)
        .build());
```

`MiningOptions` keeps the **decision** with the consumer while the **numbers** live here: one drop may want
"always exactly 5 seconds", another "5 seconds, faster with a better pick".

Build it with `MiningOptions.builder()` and state only what you have an opinion about — anything left unset resolves
from this plugin's config when `build()` is called, so options added in a later version cannot break your call site.
The toggles:

| Builder method | Meaning |
|---|---|
| `toolTier(boolean)` | does a better tool break the block faster? |
| `enchantments(boolean)` | does Efficiency help? |
| `potionEffects(boolean)` | do Haste and Conduit Power help? |
| `toolSpeed(boolean)` | all three at once |
| `speedOptions(SpeedOptions)` | a ready-made set, ignoring the toggles above |
| `saveProgress(boolean)` | remember this block's progress when the player mines something else |
| `dropVanillaBlock(boolean)` | drop the block's vanilla drops when it breaks |

The speed sources are independent on purpose: "a better pickaxe helps" and "Haste helps" are different questions for
a custom block. `new MiningOptions(applyToolSpeed, dropVanillaBlock)` stays as shorthand for all-or-nothing, and
passing `null` options is the same as `MiningOptions.defaults()`.

The old `startMining(Block, double, boolean dropVanillaBlock)` still works and never applies tool speed.

When any speed source is on:

```java
seconds = Math.max(minimumBreakSeconds, seconds / ToolSpeed.multiplierFor(player, block.getType(), speedOptions));
```

## Tool speed

`ToolSpeed.multiplierFor(Player, Material)` is public, so a plugin can show the effective break time in a GUI without
starting a dig; the `(Player, Material, SpeedOptions)` overload counts only the sources you ask for. It returns 1.0
for a bare hand, a non-tool or the wrong tool, and follows
[vanilla](https://minecraft.wiki/w/Breaking):

| Source | Value |
|---|---|
| Hand / non-tool / wrong tool | 1 |
| Wood 2, Stone 4, Copper 5, Iron 6, Diamond 8, Netherite 9, Gold 12 | tier speed |
| Shears | 15 on cobweb and leaves, 2 on wool, else 1 |
| Efficiency | **adds** (level² + 1) to the tier speed |
| Haste / Conduit Power | **multiplies** by (1 + 0.2 × level) |

Order: `(tierSpeed + efficiencyBonus) × hasteMultiplier`, floored at 1.0.

Tool speed only applies when the tool suits the block (`isPreferredTool`), so a diamond *sword* does not mine stone
8× faster. The multiplier is computed from the held item, never from `BLOCK_BREAK_SPEED` or
`Block#getBreakSpeed(player)` — plugins freeze that attribute to 0 while a custom block is being mined, so it reports
"infinitely slow". The underwater and off-the-ground penalties are not applied.

## Saved and decaying progress

By default a player who looks away from a half-mined block starts over. With `saveProgress` on, the progress is kept
per player per block, decays while they are away, and is picked up again when they come back:

```java
breakPlayer.startMining(block, 5.0, MiningOptions.builder().saveProgress(true).build());
```

Nothing is persisted: progress lives in the player's `BreakPlayer` and dies with their logout or a restart, which is
also what keeps it bounded. Three config limits stop it growing in the meantime — decay, a forget deadline and a cap
on tracked blocks per player, with the least recently mined block dropped first.

Read and write it directly, without starting a dig:

```java
double progress = breakPlayer.getProgress(block);   // 0.0 to just under 1.0, decay already applied
breakPlayer.setProgress(block, 0.5);                // half-mined; 1.0 does NOT break the block
breakPlayer.clearProgress(block);

MiningProgressStore store = breakPlayer.getProgressStore();
store.trackedBlocks();
store.clear();
```

`getProgress` returns the live value for the block being mined and the remembered one for anything else, so a GUI
can show a bar either way. A block that finishes breaking always forgets its progress, whatever the options said.

`MiningProgressStore` can also be used on its own — it is keyed by `BlockKey` (a world id and three ints, so it holds
no `Block`), takes a `ProgressLimits` and an injectable clock:

```java
MiningProgressStore store = new MiningProgressStore(ProgressLimits.noDecay(16));
```

## Config

```yaml
Tool-Speeds:
  # Matched against the start of the item's name, so tools from other plugins can be given a speed
  WOODEN_: 2.0
  STONE_: 4.0
  COPPER_: 5.0
  IRON_: 6.0
  DIAMOND_: 8.0
  NETHERITE_: 9.0
  GOLDEN_: 12.0

Mining:
  # Used for whatever a caller leaves unsaid
  Tool-Speed-Defaults:
    tool-tier: false
    enchantments: false
    potion-effects: false
  # Times never go below this; BreakPlayer treats it as an instant break
  minimum-break-seconds: 0.1

  Progress:
    # Remember a block's progress when the player mines something else
    save-by-default: false
    # Fraction of the bar lost per second while away; 0 never decays
    decay-per-second: 0.1
    # Forget a block this long after it was last mined; 0 never forgets
    forget-after-seconds: 60
    # Blocks one player may hold progress on; least recently mined dropped first. 0 is unlimited
    max-tracked-blocks-per-player: 16
```

Both tables are also built in as defaults, so a missing or partial section still works. A single
`Mining.apply-tool-speed-by-default` is still read as the fallback for all three sources. Reload at runtime with
`BlockHardnessPlugin.instance.getBlockHardnessConfig().reload()`.

## Events

- `BlockHardnessBlockInteractEvent` — a player started interacting with a block
- `CustomHardnessBlockBreakEvent` — a custom-hardness block finished breaking
