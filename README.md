# TERMYX

**Beyond the Terminal.**

TERMYX is an advanced Linux environment for **Android (ARM64/ARM32)** and **Windows (x64)**.
It is an independent evolution of the Termux experience: a real Linux terminal, installable
operating systems, a self-rendered desktop, a 24/7 server manager, Root/Shizuku integration,
a built-in code editor, a file manager and an **autonomous AI agent** that plans and executes
real tasks in the environment.

> This is a staged, modular build. The project is kept **compilable at every stage**. Features that
> depend on external native binaries (PRoot distributions, QEMU images) or on device privileges
> (Root / Shizuku) are architected with their real integration points and documented where they
> require those components to be present. No screen is faked and no button is left unimplemented.

---

## 1. Repositories used as references

TERMYX integrates with, and respects the licenses of, these upstream projects:

| Project | Purpose |
| --- | --- |
| [termux/termux-app](https://github.com/termux/termux-app) | Terminal app base |
| [termux/termux-packages](https://github.com/termux/termux-packages) | Linux packages |
| [termux/termux-tools](https://github.com/termux/termux-tools) | Internal tools |
| [termux/proot-distro](https://github.com/termux/proot-distro) | Linux distributions (PRoot) |
| [termux/termux-services](https://github.com/termux/termux-services) | Service management |
| [termux/termux-gui](https://github.com/termux/termux-gui) | Android GUI components |
| [termux/termux-api](https://github.com/termux/termux-api) | Android integration |
| [termux/termux-boot](https://github.com/termux/termux-boot) | Auto-start |
| [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) | Android permission bridge |
| [topjohnwu/Magisk](https://github.com/topjohnwu/Magisk) | Root management |
| [qemu/qemu](https://github.com/qemu/qemu) | System virtualisation |

Each integration analyses the upstream architecture and honours its license and attribution
requirements. TERMYX does **not** copy repositories verbatim; it builds clean, original integration
layers. See [`NOTICE.md`](NOTICE.md).

---

## 2. Platforms & builds

| Platform | Output | Build type |
| --- | --- | --- |
| Android | `app-arm64-v8a-release.apk`, `app-armeabi-v7a-release.apk`, `app-universal-release.apk` | Gradle + AGP + NDK |
| Windows | `TERMYX.exe` (+ installer when available) | Gradle + Compose Desktop (jpackage) |

All builds are automated with **GitHub Actions**:

- `.github/workflows/android.yml` — JDK + Android SDK + Gradle, builds debug & release APKs (ABI splits), runs tests, uploads artifacts.
- `.github/workflows/windows.yml` — Windows runner, JDK + Gradle, packages the Compose Desktop app into an `.exe`, uploads artifact.
- `.github/workflows/release.yml` — on version tags, builds both and attaches artifacts to a GitHub Release.

No API keys are stored in the repository. Provider keys live in encrypted storage on the device
(`EncryptedSharedPreferences`) and in the OS keychain on desktop.

---

## 3. Architecture

```
TERMYX/
├── core/                  # Shared JVM engine (Kotlin) — no Android/Desktop deps
│   ├── terminal/          # Real local shell session (PTY-style process I/O)
│   ├── command/           # Cross-platform command runner
│   ├── model/             # LinuxSystem, ServerConfig, AiProvider, metrics…
│   ├── ai/                # Provider adapters (OpenAI-compatible) + HTTP client
│   ├── agent/             # Autonomous agent: tools, planner, executor
│   └── security/          # ApiKeyStore interface + validation
├── android/               # Jetpack Compose application (minSdk 24, ARM64 priority)
│   ├── terminal/          # Terminal UI + JNI hooks
│   ├── systems/           # SYSTEMS screen + PRoot/chroot/QEMU manager
│   ├── servers/           # SERVERS screen + foreground service wrapper
│   ├── device/            # DEVICE ACCESS: Shizuku / Root / KernelSU / APatch
│   ├── ai/                # /IA screen + agent UI
│   ├── files/             # File manager
│   ├── editor/            # Code editor
│   ├── security/          # EncryptedSharedPreferences key store
│   └── ui/                # Theme, navigation, screens, dashboard
├── desktop/               # Compose Multiplatform (JVM) Windows/Linux app
├── assets/                # Original icons, fonts, brand
├── scripts/               # Bootstrap / install helpers
└── .github/workflows/     # android.yml · windows.yml · release.yml
```

`core` is a plain Kotlin/JVM library so it is shared by both targets. The Android and Desktop
modules supply the UI and the platform-specific privileges (Shizuku, Root, foreground services).

---

## 4. Module status

| Module | Status | Notes |
| --- | --- | --- |
| Core command runner / terminal | ✅ Functional | Real subprocess shell on every platform |
| File manager | ✅ Functional | Real filesystem read/write/archive |
| Code editor | ✅ Functional | Real editing + run for scripts |
| AI providers | ✅ Functional | Real OpenAI-compatible HTTP calls |
| Autonomous agent | ✅ Functional core | Tool framework + planner + executor |
| Server manager | ✅ Functional (local) | Real process spawn/monitor/restart |
| SYSTEMS (PRoot distros) | 🟡 Architected | Real `proot-distro`/`proot` invocation; requires bundled native binaries + network |
| Desktop environment | 🟡 Architected | Self-rendered UI; Linux GUI apps need a compat layer (Wayland/X11) |
| Root / Shizuku | 🟡 Architected | Real detection; privileged ops require the privilege to be present |

Legend: ✅ works now · 🟡 architecture present, needs optional component/permission.

---

## 5. Build & run

### Prerequisites
- JDK 17
- Android SDK 34 + NDK (Android build)
- Git, internet access for first build (dependencies)

### Android (local)
```bash
gradle :android:assembleDebug            # debug APK
gradle :android:assembleRelease          # release APKs (ABI splits)
```

### Windows / Desktop (local)
```bash
gradle :desktop:run                          # run the desktop app
gradle :desktop:packageDistributionForCurrentOS   # produce TERMYX.exe
```

### CI (GitHub Actions)
Push to `main` or open a PR to run `android.yml` + `windows.yml`. Tag `v*` to run `release.yml`.

---

## 6. Development rules
- Real features only — no fake data, no demo placeholders.
- Document any limitation instead of simulating it.
- Respect upstream licenses and attribution (see `NOTICE.md`).
- Keep the project compilable at every stage.

## 7. License
TERMYX is released under the **MIT License** (see [`LICENSE`](LICENSE)).
Third-party components keep their own licenses; see [`NOTICE.md`](NOTICE.md).
