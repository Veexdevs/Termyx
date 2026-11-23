# TERMYX — Visual Identity

**Slogan:** Beyond the Terminal.

## Principles
- **Black & white.** No saturated colors. Surfaces are pure black (`#000000`) or pure white
  (`#FFFFFF`) with neutral greys for outlines and dividers.
- **Minimalist & professional.** No gradients, no gloss, no decorative noise.
- **Modern.** Generous spacing, clear typographic hierarchy, subtle motion only.
- **Rounded typography.** Prefer a humanist/rounded typeface (e.g. a rounded sans). The app
  ships with a system rounded fallback until a custom font is bundled in `assets/fonts/`.
- **Original icons.** Icons are monochrome line glyphs drawn in `res/drawable` (Android) and
  as vector assets (desktop). No third-party icon packs that carry restrictive licenses.

## Color tokens (Material3)
- Light: primary `#000000`, background `#FFFFFF`, outline `#D0D0D0`
- Dark:  primary `#FFFFFF`, background `#000000`, outline `#3A3A3A`

## Motion
- Keep transitions ≤ 250 ms, ease-out.
- No parallax, no bouncing, no confetti.

## Logo
- A terminal window outline containing a prompt chevron and a block cursor.
- See `android/src/main/res/drawable/ic_launcher.xml` for the canonical vector.
