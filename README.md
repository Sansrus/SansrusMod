# SansrusMod

[Русская версия](README_RU.md)

A client-side Fabric utility mod for Minecraft 26.1. Adds small but useful tweaks.

---

## Features

### Inventory & UI
- **Map preview in tooltip** — hover a filled map to see its 128×128 preview without holding it.
- **Map preview in slots** — tiny 16×16 map overlay rendered directly on map icons in inventory slots.
- **Shift-drag items** — hold Shift and drag across slots to quick-move entire stacks.
- **Matching slot highlight** — highlights all slots containing the same item as the one you're hovering (enchanted books match by enchantment).
- **Highlight color picker** — full ARGB color picker for the matching-slot highlight.
- **Armor bar on HUD** — animated armor slots (boots → helmet) displayed to the left of the hotbar. Configurable duration or always-on. Armor with less than 20 durability always shown.
- **Book page holding** — hold the mouse button to continuously flip book pages. Scroll speed is adjustable.
- **Death history screen** — a dedicated GUI showing snapshots of your inventory at the moment of death. Navigate between deaths, copy inventory as image (Ctrl+C), delete entries (Delete).

### Chat & Commands
- **Cyrillic command input** — type commands in Russian layout; they are automatically converted to Latin. Smart parsing preserves Cyrillic in string arguments (messages, names).
- **Copy chat messages** — Shift+click any chat message to copy its text.
- **Chat anti-spam** — merges consecutive identical messages into one with a `*count` suffix.
- **Coordinate parser** — detects coordinates in chat (e.g., `100 200 300`, `x=100 z=300`), highlights them, click to create a Xaero waypoint.

### Combat & Survival
- **Death snapshot** — automatically saves your full inventory with timestamp and server name on death. Persists across restarts.
- **Death waypoints (Xaero)** — clickable chat message with death coordinates; click to create a skull waypoint.
- **Villager protection** — prevents accidental villager attacks unless sneaking.

### Visual
- **Item component tooltip** — press Right Alt while hovering an item to see all raw data components (enchantments, attributes, food, potions, trim, etc.) with color formatting.

### Misc
- **Disable RMB cooldown** — removes the internal use cooldown when RMB is held longer than the set threshold (in ticks).
- **RMB cooldown threshold** — how many ticks to hold RMB before the cooldown is removed. 0 = disabled immediately.
- **Book scroll speed** — adjust page-flip rate from 1 (slow) to 40 (fast).

---

## Dependencies

- **Fabric API** 0.149.0+26.1.2
- **YACL** (YetAnotherConfigLib) — config GUI (bundled at build time)
- **ModMenu** *(optional)* — access settings from the mods list
- **Xaero's Minimap** *(optional)* — required for death waypoints and coordinate parser

---

## Installation

1. Install **Fabric Loader** 0.19.2+ for Minecraft 26.1.
2. Place the mod `.jar` in your `.minecraft/mods/` folder.
3. (Optional) Install ModMenu and Xaero's Minimap for full functionality.

Configure the mod via **ModMenu → SansrusMod → Settings**, or edit `config/sansrusmod.json` manually.

---

## Configuration

All features are togglable from the YACL settings screen. Xaero-dependent options are hidden when the minimap is not loaded.

Default state: most features are **enabled**. Notable exceptions:
- `matchingSlotHighlight` — off
- `chatMessageCounter` — off
- `coordParser` — off
- `mapSlotPreview` — off
