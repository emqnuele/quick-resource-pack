# Contributing to Quick Resource Pack

Developer notes: building, testing and releasing this branch.

## Development Setup (Fedora/Linux)

`main` targets Minecraft 26.1.x with Java 25. The 1.21.x line is maintenance-only
on its own branches (`release/1.21.x`, `1.21-maintenance`).

- Java 25 for active development on Minecraft 26.1.x (`./gw25`)
- Java 21 only for the 1.21.x maintenance line (`./gw21`)

### 1) Install both JDKs

```bash
sudo dnf install java-21-openjdk-devel java-25-openjdk-devel
```

### 2) Use project wrappers

Run Gradle through the provided wrappers:

```bash
./gw25 tasks --all
./gw25 runClient
./gw25 build
./gw25 check   # unit tests + core purity
```

`gw25` forces Gradle to run on Java 25, regardless of your system default Java.

For maintenance work on the 1.21.x line:

```bash
./gw21 --version
```

### 3) Optional custom JDK paths

If your JDKs are not in standard Fedora paths, set:

- `JAVA21_HOME`
- `JAVA25_HOME`

The wrappers automatically use these variables when present.

### 3b) Tests

```bash
./gw25 check   # JUnit unit tests (src/test) + checkCorePurity
```

Game-free logic lives in `com.emqnuele.quickresourcepack.core` and must not
import `net.minecraft` / `com.mojang` — `checkCorePurity` fails the build if it
does. CI (`.github/workflows/build-26.yml`) runs `check`, builds the jars and
compiles against every 26.1.x target.

### 4) Verify compatibility across 1.21.x (maintenance line only)

Run the compatibility checker script:

```bash
./scripts/check-compat-1.21.sh
```

What it does for each stable `1.21.x` release:

- Resolves latest Yarn mappings for that patch
- Resolves latest Fabric API build for that patch
- Selects patch-aligned Cloth Config and Mod Menu versions
- Runs `clean compileJava` with version overrides via `gw21`
- Writes per-version logs under `compat-logs/`

You can also run a targeted subset while developing:

```bash
./scripts/check-compat-1.21.sh 1.21.4 1.21.11
```

CI is included in `.github/workflows/compat-1-21.yml` (maintenance branches only,
never `main`):

- PR/Push: representative smoke matrix (`1.21.1`, `1.21.4`, `1.21.8`, `1.21.11`)
- Schedule/Manual: full stable `1.21.x` matrix

### 5) Build all Modrinth jars for 1.21.x

Use the release script:

```bash
./scripts/build-release-1.21.sh --base-version 1.1.0
```

This creates one jar per patch under `release/1.21.x/<mc-version>/` and a manifest file:

- `release/1.21.x/modrinth-upload.csv`

Useful options:

```bash
# build only selected targets
./scripts/build-release-1.21.sh --base-version 1.1.0 1.21.8 1.21.11

# include sources jars too
./scripts/build-release-1.21.sh --base-version 1.1.0 --include-sources

# preview commands without building
./scripts/build-release-1.21.sh --base-version 1.1.0 --dry-run
```

### 6) Publish versions to Modrinth via API

After generating jars (`release/1.21.x/...`), publish with:

```bash
export MODRINTH_TOKEN="your_modrinth_api_token"
./scripts/publish_modrinth_releases.py \
    --project quick-resource-pack \
    --manifest release/1.21.x/modrinth-upload.csv \
    --notes MODRINTH_RELEASE_1.1.0.md
```

Useful options:

```bash
# dry-run without uploading
./scripts/publish_modrinth_releases.py --project quick-resource-pack --dry-run

# upload only one patch build
./scripts/publish_modrinth_releases.py --project quick-resource-pack --only 1.21.11

# skip default dependencies if needed
./scripts/publish_modrinth_releases.py --project quick-resource-pack --no-default-deps
```

### 7) Check and build all 26.1.x releases

Run compatibility checks for the 26.x line:

```bash
./scripts/check-compat-26.sh
```

Build all patch-specific jars for Modrinth:

```bash
./scripts/build-release-26.sh --base-version 1.2.0
```

This writes artifacts and manifest to:

- `release/26.x/<mc-version>/...`
- `release/26.x/modrinth-upload.csv`

Publish using the wrapper script:

```bash
export MODRINTH_TOKEN="your_modrinth_api_token"
./scripts/publish-release-26.sh
```

Notes:

- The current stable targets are `26.1`, `26.1.1`, `26.1.2`.
- `26` is treated as an alias to latest known 26.x target in scripts.

