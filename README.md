# Quick Resource Pack

A Fabric mod for Minecraft that allows you to quickly toggle a specific resource pack on and off using a keybind. Perfect for quickly changing textures or switching between without navigating deep into menus.

## Features

- **Quick Toggle**: Press a key (Default: `R`) to instantly enable or disable your selected resource pack.
- **Async Loading**: Loads resource packs in the background to minimize game freezes.
- **Configuration Menu**: Easily select which pack to toggle via ModMenu and Cloth Config.
- **In-Game Notifications**: Get feedback when a pack is enabled, disabled, or loading.
- **Pack Screen Integration**: Adds a convenient "Set for Quick Toggle" button directly in the vanilla Resource Pack screen.

## Installation

1. Install **Fabric Loader**.
2. Download the mod `.jar` file.
3. Place the file in your `mods` folder.
4. Ensure you have the following dependencies installed:
    - [Fabric API](https://modrinth.com/mod/fabric-api)
    - [Cloth Config API](https://modrinth.com/mod/cloth-config)
    - [Mod Menu](https://modrinth.com/mod/modmenu)

## Usage

1. **Select a Pack**:
    - Go to `Mods` > `Quick Resource Pack` > `Configure`.
    - Select your desired resource pack from the dropdown list.
    - Alternatively, go to the vanilla Resource Packs screen, select a pack, and click "Set for Quick Toggle".
2. **Toggle**:
    - Press `R` (configurable in Keybinds) to toggle the selected pack on or off.

## Configuration

The config file is located at `config/quickresourcepack.json`. You can also edit it via the Mod Menu integration.

- `selectedResourcePack`: The unique name of the resource pack to toggle.
- `showNotifications`: Enable/disable in-game toast notifications.
- `autoApplyOnStart`: Automatically apply the selected pack when the game starts.

## License

ARR License
