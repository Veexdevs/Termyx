# TERMYX — Module Status & Honest Limitations

This document tracks what is genuinely implemented versus what requires an external
component or privilege. TERMYX never fakes functionality.

## ✅ Implemented and functional
- **Core command runner** (`core`): real `ProcessBuilder` execution with timeout, stdout/stderr capture.
- **Terminal** (Android + Desktop): real local shell session streaming I/O. Android uses `/system/bin/sh`.
- **File manager**: real read/create/edit/delete in the sandbox root.
- **Code editor**: real editing + run via the appropriate interpreter.
- **AI providers**: real HTTP to OpenAI / Anthropic / Gemini-compatible endpoints, encrypted key storage, test connection, model listing.
- **Autonomous agent**: real planner → tool executor with confirmation, replanning on failure, progress events.
- **Server manager**: real OS processes with start/stop/restart, auto-restart, logs, persistence; Android foreground service for 24/7 best-effort.
- **Device access**: real root (`su`) and Shizuku-package detection; authorized execution behind confirmation.

## 🟡 Architected — needs an optional component / privilege
- **SYSTEMS (PRoot distros)**: the manager invokes `proot-distro` for real. Ubuntu/Debian/Arch/Alpine/
  Fedora/Kali/Void/openSUSE are modelled. Installation only succeeds when the `proot-distro` native
  tooling is present in the environment; otherwise it reports the requirement. chroot/QEMU paths are
  designed but not yet wired (documented, not faked).
- **Desktop environment**: the architecture (taskbar, app menu, file explorer, process manager) is
  specified; GUI Linux application rendering needs a Wayland/X11 compat layer that is out of scope for
  the initial build. Shown as a documented limitation, not simulated data.
- **Shizuku API**: package presence is detected; full AIDL usage (bound `ShizukuService`) is the next step.

## Known constraints (by design, not bugs)
- Android limits true background execution (Doze, OEM killers, battery). Servers are best-effort persistent.
- Terminal uses a process pipe, not a full PTY; job control (job suspension) is limited until a native PTY is added.
- Desktop key store is a JSON file; production should use the OS credential manager.
- No network/remote servers work without connectivity; local servers do not require it.
