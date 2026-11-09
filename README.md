# Quick Resource Pack

A client-side Fabric mod that switches resource packs with one key, without
the loading screen. Give packs their own keys, pick one from a wheel, swap
whole pack lists at once, and let packs change by themselves on a server, in
a world or in a dimension.

## Features

- **Quick Switch**: tap `R` to turn your main pack on or off.
- **Pack Wheel**: hold `R`, point at a pack, let go. You can also click a
  slot or press 1 to 9.
- **No loading screen**: packs load in the background while you keep
  playing, with a progress toast.
- **Slots**: one or more packs with a key of their own. Shift, Ctrl and Alt
  combos work, and the editor warns when Minecraft already uses a key.
- **Three modes** per slot:
  - **Toggle** adds its packs on top of the ones you use.
  - **Profile** swaps your whole pack list, and gives it back on the next
    press.
  - **Cycle** steps through packs in order.
- **Rules**: turn a slot on by itself on a server (`*.example.net`), in a
  singleplayer world, or in a dimension.
- **One menu**: open it from the button on the Resource Packs screen, from
  Mod Menu, with a key, or with `/qrp`.
- **Commands**: `/qrp`, `/qrp toggle <slot>`, `/qrp wheel`, `/qrp list`,
  `/qrp reload`, `/qrp where`.
- **Optional on-screen icons** for active slots, like status effects.
- **Messages** as a toast, above the hotbar, in chat, or off.

## Requirements

- Minecraft 26.3 (on older versions, keep using 1.2.0 for 26.2 or 1.1.1 for
  26.1 and 1.21.x)
- [Fabric Loader](https://fabricmc.net/)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Mod Menu](https://modrinth.com/mod/modmenu) (optional)

Grab the file built for your exact Minecraft version from the
[Modrinth project](https://modrinth.com/mod/quick-resource-pack).

## Getting started

1. Install Fabric Loader and Fabric API.
2. Put the Quick Resource Pack `.jar` in your `mods` folder.
3. Join a world and press `R`. The menu opens so you can make your first
   slot.

Keys can be changed under `Controls` > `Key Binds` > `Quick Resource Pack`.

## Configuration

Everything is set in game. Settings are stored in
`config/quickresourcepack.json`. Settings from 1.x are moved over
automatically, and a corrupt file is backed up to `.bak` and reset to
defaults instead of crashing.

## Support

- Bug reports and ideas:
  [GitHub issues](https://github.com/emqnuele/quick-resource-pack/issues)
- Downloads and changelogs:
  [Modrinth](https://modrinth.com/mod/quick-resource-pack)

## Developers

See [CONTRIBUTING.md](CONTRIBUTING.md) for build, test and release notes.

## License

MIT, see [LICENSE](LICENSE).
