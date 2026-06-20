# Accessibility contrast and font scaling audit

Issue: #239

Date: 2026-06-20

## Scope

Checked the mobile UI areas called out in #239:

- static brand palette in `ui/theme`,
- Material You dynamic color behavior,
- category badges/icons and reward badges,
- Start hero and place cards,
- map overlays and status banners,
- profile/ranking badge presentation.

This pass focused on code-level contrast risks and layouts that can fail with larger Android font sizes. Manual screenshot capture is deferred to #241 TalkBack/checklist coverage.

## Contrast findings

The light theme used bright brand colors as `primary` and `secondary` while pairing them with white content. Those pairs were below WCAG AA for normal text:

| Pair | Before | After |
| --- | ---: | ---: |
| Blue on white text | 3.68:1 | 5.75:1 |
| Green on white text | 3.30:1 | 5.13:1 |
| Orange category text | 3.79:1 | 5.60:1 |
| Gold reward badge text/icon | 1.79:1 | 5.93:1 |

Changes made:

- Light theme `primary`, `secondary`, and `tertiaryContainer` now use accessible brand variants.
- Category colors used as badge text were darkened where needed.
- Reward badge colors used as text/icon accents were darkened where needed.

## Dynamic colors

Material You dynamic colors are still enabled on Android 12+. Core UI uses Material `on*` color pairs for app bars, buttons, surfaces, banners and chips, so dynamic palettes should preserve readable foreground/background combinations.

Static brand/category/reward accents remain fixed where they carry domain meaning. Those fixed accents were adjusted to pass AA on light surfaces and remain readable on dark surfaces.

## Font scaling

Minimum supported behavior for this pass:

- Android font scale up to `1.3x`: key screens should remain usable without clipped primary content.
- Android font scale up to `1.5x`: dense horizontal cards may wrap/truncate secondary text, but primary names/actions should remain reachable.

Change made:

- The Start hero no longer has a fixed height. It uses a minimum height and can grow with larger text.

Known limits to verify manually in #241:

- long place names in ranking/list/detail cards at `1.5x`,
- map overlays with filters plus permission/GPS banners,
- profile badge dialog with several badges,
- dynamic color palettes with very low-chroma wallpapers.

## Result

The code-level contrast risks found in this pass were addressed. Remaining acceptance criteria should be verified manually with TalkBack, dark mode, dynamic colors and Android font scaling as part of #241.
