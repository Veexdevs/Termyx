# NOTICE — Third-party components and attributions

TERMYX is original software released under the MIT License. It integrates with, and learns from,
several open-source projects. Their licenses and copyright notices are respected and retained
where their code or assets are used. This file documents those relationships.

| Component | Upstream license | Relationship |
| --- | --- | --- |
| termux/termux-app | GPL-3.0 | Architectural reference for the Android terminal app |
| termux/termux-packages | GPL-3.0 | Package repository reference |
| termux/termux-tools | GPL-3.0 | Tooling reference |
| termux/proot-distro | GPL-3.0 | Reference for distribution install/launch via PRoot |
| termux/termux-services | GPL-3.0 | Reference for service management |
| termux/termux-gui | GPL-3.0 | Reference for Android GUI integration |
| termux/termux-api | GPL-3.0 | Reference for Android API integration |
| termux/termux-boot | GPL-3.0 | Reference for auto-start |
| RikkaApps/Shizuku | Apache-2.0 | Reference for the Shizuku permission bridge |
| topjohnwu/Magisk | GPL-3.0 | Reference for root management |
| qemu/qemu | GPL-2.0 | Reference for system virtualisation |

## Redistribution
- No upstream source is vendored into this repository.
- Where TERMYX invokes upstream binaries at runtime (e.g. `proot`, `proot-distro`,
  `qemu-system-*`), those binaries are obtained from their respective distributions/packages
  and keep their own licenses.
- Attribution is preserved in source headers that reference a specific upstream behaviour.

## Trademarks
"Termux" is a trademark of its respective owners. TERMYX is an independent project and is not
affiliated with or endorsed by Termux or any listed project.
