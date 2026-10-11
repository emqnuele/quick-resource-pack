# Quick Resource Pack

A Fabric mod that lets you toggle a chosen resource pack on and off with a
single keybind — no menu diving. Pick a pack once, then switch it instantly
whenever you like.

## Features

- **Quick Toggle**: press a key (default: `R`) to instantly enable or disable
  your selected resource pack.
- **Quick Menu**: an optional keybind (unbound by default) opens a small
  in-game menu with the toggle, a pack picker shortcut and
  notification/auto-apply switches.
- **Configuration screen**: pick the pack from a dropdown via Mod Menu and
  Cloth Config.
- **Async reload**: packs reload in the background to minimize game freezes.
- **In-game notifications**: chat feedback when a pack is enabled, disabled
  or still loading.
- **Screen-safe keybinds**: the toggle never fires while you are typing in
  chat or have any screen open, and queued presses are drained so they do
  not retrigger later.

## Requirements

- [Fabric Loader](https://fabricmc.net/)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cloth Config API](https://modrinth.com/mod/cloth-config)
- [Mod Menu](https://modrinth.com/mod/modmenu) (optional, recommended — it
  hosts the configuration screen)

Grab the file built for your exact Minecraft version from the
[Modrinth project](https://modrinth.com/mod/quick-resource-pack).

## Installation

1. Install Fabric Loader for your Minecraft version.
2. Download the Quick Resource Pack `.jar` matching your game version.
3. Place it, together with the requirements above, in your `mods` folder.
4. Launch the game.

## Usage

1. **Select a pack**: go to `Mods` > `Quick Resource Pack` > `Configure`
   and choose a pack from the dropdown list.
2. **Toggle**: press `R` (rebindable under `Controls` > `Key Binds`) to
   switch the selected pack on or off.
3. **Quick menu** (optional): bind `Open Quick Menu` to get a small in-game
   panel with the same options.

## Configuration

Settings live in `config/quickresourcepack.json` (editable in-game through
the Mod Menu screen):

- `selectedResourcePack`: unique name of the pack to toggle (default: empty,
  i.e. none selected).
- `showNotifications`: chat feedback on toggle (default: on).
- `autoApplyOnStart`: apply the selected pack when the game starts
  (default: off).

A corrupt config file is backed up to `.bak` and reset to defaults instead
of crashing.

## Support

- Bug reports and ideas:
  [GitHub issues](https://github.com/emqnuele/quick-resource-pack/issues)
- Downloads and changelogs:
  [Modrinth](https://modrinth.com/mod/quick-resource-pack)

## Developers

See [CONTRIBUTING.md](CONTRIBUTING.md) for build, test and release notes.

## License

MIT — see [LICENSE](LICENSE).
