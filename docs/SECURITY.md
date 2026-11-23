# TERMYX — Security

TERMYX treats security as a first-class concern, especially because it can run privileged
operations and connect to external AI services.

## API key storage
- Provider keys are **never** stored in plaintext preferences or committed to the repository.
- Android: `EncryptedSharedPreferences` (AES-256 GCM) — see `android/.../security/EncryptedKeyStore`.
- Desktop: file-backed store (`DesktopKeyStore`); production builds should migrate to the OS
  credential manager (Windows Credential Manager / libsecret / Keychain).
- The whole provider blob (including key) is encrypted at rest by the platform store.

## Command execution safety
- Every AI/agent action goes through the tool system (`core/.../agent/tools`). The agent cannot
  run arbitrary commands except via `terminal.execute` / `device.execute_authorized`.
- **Destructive and privileged tools are flagged `dangerous`** and require explicit user confirmation
  (`files.delete`, `device.execute_authorized`). The UI shows a confirmation dialog.
- File paths are resolved within a sandbox root with traversal protection (`resolveWithin`).
- Downloaded artifacts should be validated/integrity-checked before being treated as installed.

## Privilege handling
- TERMYX **never assumes root** and **never attempts to gain root** automatically.
- Root/Shizuku/KernelSU/APatch availability is detected at runtime; operations run only when granted.
- Sensitive operations request authorization before execution.

## No supply-chain substitution
- Upstream projects (Termux family, Shizuku, Magisk, QEMU) are referenced, not vendored. Their
  binaries are obtained from their own distributions and keep their licenses — see `NOTICE.md`.

## Reporting
Report security issues privately to the maintainers before public disclosure.
