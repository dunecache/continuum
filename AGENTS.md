# AGENTS.md

Guidance for AI coding agents working in this repository.

## Project

A fork of **Continuum for Reddit** (Android, descended from Infinity for Reddit). The goal of this fork is a **clean, modern, cohesive UI** without breaking existing functionality. Behavior and data layers stay as close to upstream as possible; the visual layer is where we make changes.

> Before starting: read the repo's `README`, `build.gradle` files, and the `app/src/main` tree. Verify the stack below against what is actually in the repo. If this file disagrees with the code, the code wins, then update this file.

## Priorities (in order)

1. **Don't break the app.** It must build and launch after every change.
2. **Visual consistency.** One spacing scale, one type scale, one radius scale, one elevation approach.
3. **Clarity and density balance.** Content (posts, comments, media) is the hero. Chrome stays quiet.
4. **Small, reviewable diffs.** Keep upstream merges feasible.

## Stack (verify against repo)

- Language: Java (some Kotlin possible). Match the language of the file you edit. Do not convert files unless asked.
- UI: XML layouts + Material Components, View-based. Not Compose unless the repo already uses it.
- Typical libs: Retrofit/OkHttp, Room, Glide, ExoPlayer/Media3, Dagger/Hilt. Do not swap or upgrade libraries as part of UI work.
- Build: Gradle wrapper (`./gradlew`).

## Commands

```bash
./gradlew assembleDebug          # build debug APK
./gradlew lint                   # Android lint
./gradlew test                   # unit tests
./gradlew installDebug           # install on connected device/emulator
```

If the build is heavy or the environment is constrained (e.g. Termux), build only the module you touched and avoid full clean builds.

## Design system rules

Define once, reuse everywhere. Never hardcode values in layouts.

- **Spacing:** 4dp base grid. Tokens like `space_4`, `space_8`, `space_12`, `space_16`, `space_24` in `dimens.xml`. No magic numbers such as `13dp`.
- **Type scale:** Use `TextAppearance` styles (e.g. `Title`, `Body`, `Meta`, `Caption`). No inline `textSize` in layouts.
- **Shape:** Small set of corner radii (e.g. 8dp, 12dp, 16dp) as shape appearances.
- **Color:** Use theme attributes (`?attr/colorSurface`, `?attr/colorOnSurface`, etc.), never raw hex in layouts. All colors must exist for **light, dark, and AMOLED** themes.
- **Elevation:** Prefer tonal surfaces and thin dividers over heavy shadows.
- **Icons:** One icon set, one stroke weight, 24dp default. Tint via theme attributes.
- **Motion:** Short, subtle (150 to 250ms). Respect the system "remove animations" setting.

## UI guidelines

- Post cards: clear hierarchy (title > media > meta). Subreddit/user/time on one quiet meta line. Actions row aligned and evenly spaced.
- Comments: readable indentation with thin depth guides; do not let deep threads squash the text column.
- Touch targets: minimum 48dp.
- Text contrast: WCAG AA at minimum, in every theme.
- Support **RTL** layouts: use `start`/`end`, never `left`/`right`.
- Support large font scale and small screens; nothing should clip or overlap at 200% font size.
- Every list needs designed **loading, empty, and error** states.
- Respect user customization: existing theme/font/layout preferences must keep working. New visual options go through the existing settings system.

## Code conventions

- Match surrounding style, naming, and formatting. Do not reformat unrelated code.
- Prefer styles and themes over per-view attributes. Extract repeated layout chunks into `<include>` or custom views.
- Put strings in `strings.xml` (no hardcoded UI text). Do not edit translation files by hand except adding keys to the default file.
- Keep view logic out of layouts and business logic out of adapters/holders.
- Avoid nested layouts deeper than necessary; prefer `ConstraintLayout` or flat structures for list items (scroll performance matters).
- Do not add dependencies without asking.

## Workflow for agents

1. State the plan briefly: which screens/files, which tokens or styles are added or changed.
2. Make the smallest change that achieves the goal. One screen or component per change when possible.
3. Build (`assembleDebug`) and fix errors before reporting done.
4. Check the change in **light, dark, and AMOLED**, and with large font + RTL if layout changed.
5. Summarize what changed and anything you could not verify (e.g. no device available).

## Do not

- Do not touch API/auth/network code, database schemas, or migrations for a UI task.
- Do not remove features or settings to "simplify" the UI without approval.
- Do not commit API keys, client IDs, keystores, or signing configs.
- Do not rename packages or app IDs unless explicitly asked.
- Do not mass-rewrite upstream files (hurts future merges). Prefer additive changes (new styles, new tokens) over edits scattered across the codebase.
- Do not introduce tracking, ads, or analytics.

## Upstream sync

- Keep an `upstream` remote and rebase or merge regularly.
- Isolate fork-specific UI work in clearly named files/styles (e.g. `styles_fork.xml`, `dimens_fork.xml`) where possible to reduce conflicts.
- Note any intentional divergence from upstream in `docs/DIVERGENCES.md`.

## Definition of done

- Builds cleanly, lint introduces no new warnings.
- Uses design tokens only (no new hardcoded colors, sizes, or strings).
- Verified in light, dark, and AMOLED themes.
- No regressions in scrolling, media playback, or navigation on the touched screens.
- Screenshots before/after included in the PR description.

```
```
