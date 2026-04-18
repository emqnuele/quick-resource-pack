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

## Development Setup (Fedora/Linux)

This project uses a dual-JDK workflow:

- Java 21 for active development on Minecraft 1.21.x
- Java 25 kept installed for future migration to 26.x

### 1) Install both JDKs

```bash
sudo dnf install java-21-openjdk-devel java-25-openjdk-devel
```

### 2) Use project wrappers

Run Gradle through the provided wrappers:

```bash
./gw21 tasks --all
./gw21 runClient
./gw21 build
```

`gw21` forces Gradle to run on Java 21, regardless of your system default Java.

For future migration work, this wrapper is ready as well:

```bash
./gw25 --version
```

### 3) Optional custom JDK paths

If your JDKs are not in standard Fedora paths, set:

- `JAVA21_HOME`
- `JAVA25_HOME`

The wrappers automatically use these variables when present.

### 4) Verify compatibility across 1.21.x

Run the compatibility checker script:

```bash
./scripts/check-compat-1.21.sh
```

What it does for each stable `1.21.x` release:

- Resolves latest Yarn mappings for that patch
- Resolves latest Fabric API build for that patch
- Runs `clean compileJava` with version overrides via `gw21`
- Writes per-version logs under `compat-logs/`

You can also run a targeted subset while developing:

```bash
./scripts/check-compat-1.21.sh 1.21.4 1.21.11
```

CI is included in `.github/workflows/compat-1-21.yml`:

- PR/Push: representative smoke matrix (`1.21.1`, `1.21.4`, `1.21.8`, `1.21.11`)
- Schedule/Manual: full stable `1.21.x` matrix

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
