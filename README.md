# WanderBots — Minecraft 1.21.11 Paper

WanderBots creates real server-side fake-player entities using `ServerPlayer`. The bots have physical player hitboxes, are in the server player list/TAB, receive damage, fall under normal entity gravity, wander continuously, turn away from walls, jump over simple one-block obstacles, can get random armor/enchantments and can hold random items.

## Commands

All commands require `wanderbots.admin` (OP by default).

```text
/wanderbots spawn [amount]
/wanderbots remove <name|all>
/wanderbots list
/wanderbots equip <name|all>
/wanderbots hold <name|all>
/wanderbots respawn <name|all>
/wanderbots reload
```

Examples:

```text
/bots spawn 10
/bots equip all
/bots hold all
/bots remove all
```

## What the plugin does

- **Random names:** generated from adjective/animal/colour-style words, guaranteed unique while the bot exists.
- **TAB list:** bots are registered through the vanilla `PlayerList.placeNewPlayer(...)` path, so they appear as players in TAB rather than as armour-stand NPCs.
- **Gravity:** the code never pins Y. The server's normal player physics updates vertical motion; bots can fall and land naturally.
- **Wall avoidance:** a lightweight steering controller checks the collision shape in front of each bot, turns before solid obstacles, and can jump over a one-block obstacle. It also turns on horizontal collision and steers back toward its spawn area when it wanders too far.
- **Damage/death:** bots are regular `ServerPlayer` entities and are not made invulnerable. They can be hit by players/mobs and can die. Auto-respawn is enabled by default after 100 ticks.
- **Random armor:** `/bots equip <name|all>` chooses a random armor tier, fills the four armor slots, then applies a random selection of common armor enchantments.
- **Random held item:** `/bots hold <name|all>` gives the bot a random item and may randomize its stack size.
- **Random skins:** the plugin uses a built-in seed list and can auto-discover additional public Minecraft profile candidates, then obtains the signed skin texture property from Mojang's session profile endpoint and caches it locally. This is designed to grow to 300+ skin candidates instead of hard-coding only a handful of skins.

## Requirements

- Paper **1.21.11**
- Java **21**
- A normal Paper server, not Spigot-only
- Internet access from the server is recommended for automatic skin discovery/skin warming. The bots still work without it; skins fall back to cached/default profiles.

## Build

Paper's supported NMS build system for 1.21.11 is `paperweight-userdev`. Paper documents the 1.20.5–1.21.11 remapping workflow and the `paperDevBundle` setup here:

https://docs.papermc.io/paper/dev/userdev/

From this folder, install Gradle 9+ and run:

```text
gradle build
```

The production plugin jar is created under:

```text
build/libs/
```

Use the jar that does **not** end in `-dev.jar`.

## Install the finished plugin

1. Make sure your server is running Paper **1.21.11** and Java **21**.
2. Stop the server.
3. Put the built `WanderBots-1.0.0.jar` into the server's `plugins` folder.
4. Start the server once.
5. Check that `plugins/WanderBots/config.yml` was created.
6. Join the server as OP.
7. Run `/bots spawn 10`.
8. Run `/bots equip all` and `/bots hold all`.
9. Open TAB — the bots should be listed there.

## First-time skin warming

On a fresh installation, skin discovery is asynchronous so the main server thread is not blocked by HTTP calls. The plugin stores successful texture properties in `plugins/WanderBots/skin-cache.yml`.

For a brand-new server, the first few bots may use cached/default skins until the pool warms. Once the cache contains more entries, new bots will pick randomly from the larger pool.

## Performance

Fake players are real server entities. A hundred bots is not the same CPU cost as a hundred nametag-only NPCs. Start with 5–20 bots, then increase the count while watching TPS/MSPT. The default movement controller is intentionally lightweight.

## Notes

This is intentionally **Paper 1.21.11-specific** because it uses server internals. Minecraft/Paper changed its internal mappings after 1.21.11, so this jar is not intended for 1.21.10, 26.1, or 26.2 without a rebuild/update.
