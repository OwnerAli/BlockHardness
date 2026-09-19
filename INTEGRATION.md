# Integrating with BlockHardness

For a plugin that wants blocks to take a custom time to break. BlockHardness owns the timing, the crack animation
and the mining maths; your plugin decides *which* blocks are special and *how long* they should take.

## Setup

Depend on it at compile time and at runtime:

```kotlin
// build.gradle.kts
repositories { maven("https://jitpack.io") }
dependencies { compileOnly("com.github.OwnerAli:BlockHardness:0.0.1") }
```

```yaml
# plugin.yml
depend: [BlockHardness]
```

BlockHardness itself needs ProtocolLib on the server.

## The loop

`BlockHardnessBlockInteractEvent` fires on every arm swing at a block within 5 blocks. Listen for it, decide whether
the block is yours, and call `startMining` — once per swing. Progress accrues from the time between calls, so stop
calling and mining stops.

```java
@EventHandler
public void onInteract(BlockHardnessBlockInteractEvent event) {
    Block block = event.getBlock();
    Player player = event.getPlayer();

    MyDrop drop = drops.find(block);
    if (drop == null) return;

    BlockHardnessPlugin.instance.getBreakPlayerRegistry()
            .getBreakPlayer(player)
            .ifPresent(breakPlayer -> breakPlayer.startMining(block, drop.seconds(), options(drop)));
}
```

`CustomHardnessBlockBreakEvent` fires when a block finishes breaking — that is where you give out your drops.

The `BreakPlayer` for a player exists from join to quit; `getBreakPlayer` returns `Optional.empty()` if they are not
online. Don't cache it across logins — a rejoining player gets a new one.

## MiningOptions

The third argument to `startMining`. Build it per call: BlockHardness holds the *numbers*, you hold the *decision*.

```java
private MiningOptions options(MyDrop drop) {
    return MiningOptions.builder()
            .toolTier(drop.usesToolSpeed())   // does a better pickaxe break it faster?
            .saveProgress(drop.isPersistent())// resume where the player left off?
            .dropVanillaBlock(false)          // you are giving out the drops yourself
            .build();
}
```

| Builder method | Effect |
|---|---|
| `toolTier(boolean)` | tool tier (wood 2 … gold 12) applies |
| `enchantments(boolean)` | Efficiency applies (`level² + 1`, added) |
| `potionEffects(boolean)` | Haste / Conduit Power apply (`1 + 0.2 × level`, multiplied) |
| `toolSpeed(boolean)` | all three at once |
| `speedOptions(SpeedOptions)` | a ready-made set, overriding the three above |
| `saveProgress(boolean)` | keep this block's progress when the player mines something else |
| `dropVanillaBlock(boolean)` | let BlockHardness drop the block's vanilla drops |

**Anything you don't set comes from BlockHardness's config**, resolved when `build()` runs. So set only what your
plugin has an opinion about, and the server owner controls the rest. `MiningOptions.defaults()` is all-config;
`new MiningOptions(applyToolSpeed, dropVanillaBlock)` is a shorthand where tool speed is all-or-nothing.

Options added in future versions won't break your call site — that is the point of the builder. Don't hand-roll an
options object or compute break times yourself.

## Break times and tool speed

Pass the *base* time in seconds, as if mined by hand. BlockHardness divides by the tool speed multiplier for the
sources you enabled and floors the result at `minimum-break-seconds` (0.1 by default, which it treats as an instant
break).

To show a player what a block would take without starting a dig:

```java
double multiplier = ToolSpeed.multiplierFor(player, block.getType());          // every source
double multiplier = ToolSpeed.multiplierFor(player, block.getType(), sources); // only these
double seconds = baseSeconds / multiplier;
```

Returns 1.0 for a bare hand, a non-tool, or a tool that doesn't suit the block. Don't read `BLOCK_BREAK_SPEED` or
`Block#getBreakSpeed` to do this yourself — plugins (probably yours) freeze that attribute to 0 while a custom block
is being mined, so it reports "infinitely slow".

## Progress

With `saveProgress(true)`, a half-mined block is remembered per player, decays while they are away, and resumes when
they come back. Nothing is persisted: it dies with the player's logout and with a restart. The server owner bounds it
with `decay-per-second`, `forget-after-seconds` and `max-tracked-blocks-per-player`.

Read or seed it without starting a dig:

```java
double progress = breakPlayer.getProgress(block);  // 0.0 to just under 1.0, decay already applied
breakPlayer.setProgress(block, 0.5);               // half-mined; 1.0 does NOT break the block
breakPlayer.clearProgress(block);
```

`getProgress` gives the live value for the block being mined and the remembered value for any other, so a bar or
hologram can use it either way. A block that finishes breaking always forgets its progress.

Saved blocks keep their cracks on screen (the server owner can turn that off), so you don't need to render anything
yourself to show a half-mined block.

## Gotchas

- **`BlockHardnessBlockInteractEvent` extends `BlockBreakEvent`.** So does `CustomHardnessBlockBreakEvent`. If your
  plugin has a `BlockBreakEvent` listener, it fires for both of these too — check the concrete type, or you will
  process a swing as a break. Cancelling them does nothing; BlockHardness does not check.
- **The interact event fires per swing, on any block**, including ordinary ones. Filter before doing real work; this
  runs several times a second per player.
- **Set `dropVanillaBlock(false)`** if you handle drops in your break listener, or the block drops twice.
- **`setProgress(block, 1.0)` does not break the block.** It fills the bar. Breaking happens through mining.
- **`startMining` must run on the main thread** — it sets blocks, plays sounds and fires events.
- The old `startMining(Block, double, boolean dropVanillaBlock)` still exists and never applies tool speed. Passing
  `null` options is the same as `MiningOptions.defaults()`.

## Config knobs you don't own

These live in BlockHardness's `config.yml` and belong to the server owner. Your plugin shouldn't duplicate them:
tool speed per tier (`Tool-Speeds`), which speed sources apply by default, `minimum-break-seconds`, and everything
under `Mining.Progress`. Consult them, if you must, through
`BlockHardnessPlugin.instance.getBlockHardnessConfig()`.
