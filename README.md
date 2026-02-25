# MewHome

Server-side NeoForge mod for Minecraft 1.21.1. Replaces vanilla bed spawn mechanics with a simple home system.

## How it works

Place a bed — that's your home. No clicking beds to set spawn. No `/sethome`. Just place it.

## Commands

| Command | Who | What |
|---------|-----|------|
| `/home` | Everyone | Teleport to your bed |
| `/spawn` | Everyone | Teleport to world spawn |
| `/sethome` | Everyone | Tells you to place a bed |
| `/setspawn` | OP only | Set world spawn to your current position |

## Bed rules

- **Place a bed** — it becomes your home, you get a confirmation message
- **Break your own bed** — allowed, home point is removed with a warning
- **Break someone else's bed** — blocked, only OP can do this
- **OP breaks someone's bed** — allowed, owner gets notified
- **Click/sleep in any bed** — does NOT change your spawn point
- **Die with a bed** — respawn at your bed
- **Die without a bed** — respawn at world spawn

## Localization

The mod resolves translations server-side based on each player's client language. Ships with:
- English (`en_us`)
- Russian (`ru_ru`)

Any other language falls back to English. Adding a new language is one JSON file in `assets/mewhome/lang/`.

## Installation

Server-only mod. Drop the JAR into the server's `mods/` folder. Clients do not need it.

### Requirements
- Minecraft 1.21.1
- NeoForge 21.1+

## Building from source

```
./gradlew build
```

JAR output: `build/libs/mewhome-<version>.jar`

## License

[MIT](LICENSE)
