# Continuum Redesign

> Design pass. Builds on the audit (`docs/UI_OVERHAUL.md` + `docs/ui-audit/`). App source is untouched; this doc and `docs/redesign/` are the only outputs.
> Carried-in maintainer decisions: post cards converge on **card_3 + compact** (card_2/legacy deprecate); **UI-033 in scope** (icon variants + per-subreddit accents); two-pane **undecided**.

## R0. Direction

> **Pass status:** this run finalizes R0 only. Any R1–R4 material already present in this working tree predates this direction and remains an unapproved draft until its pass is explicitly run and confirmed.

### R0.1 Design thesis

**Continuum should become a precise reading instrument, not another social feed.** It should let a power user scan, compare, vote, swipe, and open media quickly, while giving a casual reader enough hierarchy and calm to browse without the app shouting at them. The redesign keeps every existing capability but replaces the current collection of inherited UI conventions with one recognizable grammar.

The direction is **Signal — crisp editorial**: dense where scanning matters, editorial where reading matters, and expressive only through purposeful motion and feedback.

### R0.2 Biggest structural problems

These are product-structure problems, not a shortage of rounded corners or token names.

1. **There is no stable content hierarchy.** Compact and card_3 stack metadata above the title, card_2 moves metadata below it, detail headers resemble feed cards, and inbox rows give four text blocks equal weight. Changing view mode changes the information model instead of only its density. This is audit §2 and the feed/post-card findings.
2. **Navigation systems compete rather than compose.** A drawer, optional cutout bottom bar, top feed tabs, per-screen toolbars, and several FABs all claim primary status. One-handed reach and the user's mental model change by screen. This is the R1 problem exposed by the audit's navigation inventory.
3. **The app has two theme architectures that do not speak.** The 87-field custom theme engine preserves valuable user choice, while the root theme exposes a small set of unrelated attributes and rows apply colors in code. Dynamic wallpaper color is copied into custom records instead of becoming a coherent role system. This creates hardcoded fills, preview drift, and per-bind cost (audit DS-1 and DS-11).
4. **Density is accidental rather than designed.** The app has useful bones—4dp clustering, compact thumbnails, configurable post modes, swipe levels—but no declared information budget. Similar content can be 200dp or materially taller depending on layout mode, and some cleanup paths hide functionality or rely on animation to fit.
5. **Interaction lacks a common language.** Swipe actions, FAB jumps, comment collapse, media drag-dismiss, custom back, and per-row layout animation all work locally, but they do not share state feedback, timing, accessibility semantics, or predictive-back behavior. Motion currently describes implementation accidents more than product intent.
6. **Important states and large screens are outside the visual system.** Loading is mostly a spinner, empty and offline states are inconsistent, accessibility labels are missing, and tablet support is a stretched single pane plus a rail. Redesign components therefore need explicit state and adaptive contracts, not only polished resting states.

### R0.3 Design principles

#### 1. Content owns the frame

- **Will:** keep source, title, media, and thread text in the strongest visual layer; collapse profile and community chrome after identity is established; give each screen one unmistakable primary action.
- **Will not:** place a banner, carousel, oversized title, or default FAB before the content the user came to read; repeat the screen name in multiple bars.
- **Why:** the audit found roughly 300dp headers, disappearing section context, and multiple toolbars/FABs competing on high-use screens.

#### 2. Density is a product capability

- **Will:** preserve compact, default, and media-first choices; keep vote, comment, save, share, and configured swipe actions directly available; use typography and alignment—not removed features—to achieve calm.
- **Will not:** turn every row into a large card, move power actions into hidden overflow by default, or reduce the information visible per screen without user choice.
- **Why:** Continuum's differentiators include many feed modes, three-level swipe configuration, lazy mode, comment tools, and extensive customization.

#### 3. One grammar, many palettes

- **Will:** map content, comments, states, and chrome to a single set of semantic roles across light, dark, AMOLED, static, dynamic, and imported user themes.
- **Will not:** hardcode row fills, tint from system colors, maintain separate screen recipes, or treat dynamic color as a skin that erases the existing theme model.
- **Why:** 164 layout hex values, per-bind color calls, and the disconnected 87-field theme engine are the root of visual inconsistency.

#### 4. Motion carries meaning

- **Will:** show where media or a thread went, expose commit points with restrained haptics, and use predictive back consistently.
- **Will not:** animate recycled row roots, use endless marquees, add spring motion to static content, or require an animation to understand a state change.
- **Why:** the current UI has accidental `animateLayoutChanges`, no shared-element continuity, and custom back paths outside Android's modern system.

#### 5. Every mode must remain operable

- **Will:** provide visible and announced state, 48dp touch targets, thumb-reachable primary actions, static large-font layouts, and mirrored start/end geometry.
- **Will not:** make gesture-only behavior, color-only state, unlabeled icon actions, or fixed-height text containers acceptable.
- **Why:** touch and TalkBack debt is extensive, while large-font and RTL behavior is only partially verified.

### R0.4 Product identity

#### Personality and mood

- **Personality:** observant, assured, technically literate, and quietly opinionated. It should feel closer to a well-made reading instrument than a social network or a Material component gallery.
- **Mood:** calm at rest and quick under the hand. Content supplies personality through community imagery, authors, and media; app chrome stays restrained.
- **Emotional target:** users can scan a dense feed without anxiety, open an article-like post detail without a dramatic mode change, and always understand what a gesture just did.

#### Signature language: the signal rail

A thin 2dp semantic rail is the one recurring brand device. It appears only where it communicates state or relationship:

- at the inline-start edge of a selected/active feed source;
- beside an unread inbox item;
- through comment depth guides, with contrast reduced at deeper levels;
- as a short community-accent segment when the optional per-subreddit accent is enabled.

The rail is not an outline around every component. It is a continuity mark that makes source, unread state, and thread structure legible while remaining quiet at rest. This gives Continuum an identity derived from information flow rather than a proprietary brand hue.

#### Typographic voice

- Use the platform sans-serif for navigation, controls, metadata, and short titles by default; it is fast, familiar, avoids APK cost, and respects OEM/user font choices.
- Retain the existing user-selected content family for post bodies, comments, and long messages.
- Use a compact hierarchy: strong titles, quiet one-line metadata, tabular numerals for scores/counts, sentence case, and no all-caps labels.
- Atkinson Hyperlegible remains an accessibility-oriented option, not the default brand face.
- No new font dependency is needed for R0.

#### Color philosophy

- **Dynamic first, user-controlled:** on Android 12+ the wallpaper palette may populate Material 3 color roles. Existing user themes and AMOLED mode remain first-class choices, not legacy fallbacks to be removed.
- **No fixed Continuum hue:** the identity survives when the palette changes because the signal rail, type hierarchy, spacing, and motion carry the brand. Static Indigo remains the recognizable fallback when dynamic color is unavailable or disabled.
- **Tonal depth:** background, content planes, selected states, and dividers come from surface roles. Shadows are reserved for genuinely floating controls and sheets.
- **Community accents:** UI-033 may tint the signal rail or compact source marker after explicit opt-in. It must not re-skin an entire community page or reduce text contrast.
- **AMOLED:** true black is the base, with one restrained elevated surface level and outline contrast; “dark AMOLED” is not a single black card on black.

Implementation cannot simply overlay Material dynamic colors on top of the existing engine. The later design-system pass must define an adapter/exporter from Material roles into the 87 custom-theme fields, preserving imported themes and per-mode preferences. This is a large but isolated theme-layer change; it does not require an API, auth, or database redesign.

#### Shape language: frames, not bubbles

- Compact text rows are flat planes separated by quiet rules.
- Default and media-first posts may use a single 12dp content frame; nested cards are prohibited.
- Media and small controls use 8dp; sheets use the Material 3 large shape; pills are reserved for compact state or filter semantics.
- Corners identify component class; they do not decorate empty space.
- The resulting silhouette is intentionally less uniformly rounded than stock Material 3 while still using standard `ShapeAppearance` and component primitives.

#### Iconography

- Keep the existing coherent 24dp vector family for continuity, but establish filled/outline state pairs and audited `autoMirrored` directionality.
- Icons sit in 48dp targets; icon size and target size are separate tokens.
- Icons use semantic theme roles. State color is reserved for committed actions such as votes, subscriptions, and saved state.
- Status is never communicated by an unlabelled icon or color alone.

#### Motion character: continuity, not spectacle

- 100–150ms for local state such as vote confirmation, selection, and disclosure.
- 180–250ms for sheets, search transitions, and local containment changes.
- 250–300ms only for cross-screen continuity or media entry/exit.
- Use Material 3 Expressive motion attributes where the installed Views library exposes them, with standard emphasized/standard easing fallbacks.
- Shared-element continuity is reserved for media, where it prevents spatial disorientation. Text uses a restrained fade-through/container transition.
- Haptics are sparse confirmation signals—swipe arm, vote commit, collapse, send—and always respect both the app preference and the system setting.

### R0.5 Material You and Material 3 Expressive

The installed `com.google.android.material:material:1.14.0` supports the Material 3 DayNight role system, dynamic-color theme overlays, and the Material 3 Expressive theme family introduced in the 1.14 line. The Views documentation exposes selected Expressive treatments, including expressive bottom navigation, a medium FAB style, expressive progress behavior, and motion theme attributes.

The redesign therefore uses Material 3 as the component foundation and Material 3 Expressive as a **behavior and emphasis layer**, not as a wholesale skin:

- standard roles, shapes, typography, cards, dividers, sheets, and navigation provide the base;
- Expressive motion and component variants are allowed where they clarify selection, commitment, or navigation;
- every expressive treatment has an ordinary Material 3 fallback for unsupported API/device states;
- the core feed, comments, and settings remain View-based; a wholesale Compose migration is explicitly outside this redesign because it would combine product redesign with a framework rewrite.

The Material Components Views library is in maintenance mode, so the design depends only on shipped 1.14.0 APIs and standard AndroidX/framework behavior. It does not assume unreleased expressive components or a future library upgrade.

### R0.6 Candidate directions

#### Direction A — Signal: crisp editorial **(recommended)**

Flat, fast lists with a single metadata grammar; selective content frames; a semantic signal rail; strong editorial typography; restrained media continuity. The interface is dense by default but never cramped because row types have explicit information budgets.

- **User outcome:** power features stay one gesture or one tap away, while casual browsing remains calm and legible.
- **Distinctiveness:** comes from hierarchy, the signal rail, and motion—not a fixed gradient, giant cards, or custom icon family.
- **Feasibility:** strongest fit with existing RecyclerViews, compact media previews, custom themes, swipe actions, and three supported post families. Requires a substantial theme-role bridge and post-row consolidation, but not a new UI framework.
- **Trade-off:** visually restrained. Success depends on typography, spacing, and motion discipline rather than immediately obvious surface effects.

#### Direction B — Gallery: quiet editorial

Media-forward cards, large titles, more whitespace, fewer persistent row actions, and image-led navigation. Empty states and profile headers receive strong editorial composition.

- **User outcome:** stronger visual relaxation and photography/editorial browsing.
- **Benefit:** makes the app immediately distinctive and gives media more visual authority.
- **Failure against requirements:** persistent actions and metadata compete with large media; density falls; many settings remain reachable only through overflows; nearly every row family needs redesign.
- **Feasibility:** no new dependency is required, but uncapped/high-resolution media and broad row replacement increase performance risk.

#### Direction C — Pulse: Material 3 Expressive showcase

Expressive navigation, springier containers, shape morphs, prominent FABs, animated loading, and tonal cards used throughout the app.

- **User outcome:** lively and immediately recognizable as a modern Android application.
- **Benefit:** fastest route to visible Material 3 Expressive effects using shipped components.
- **Failure against requirements:** resembles a component showcase, increases chrome and motion density, weakens post/thread boundaries, and risks stock-demo visual sameness.
- **Feasibility:** supported by the installed library, but applying it broadly would become a large reskin. Later expressive gaps would be harder to fill because the Views library is in maintenance mode.

#### Comparison

| Criterion | A — Signal | B — Gallery | C — Pulse |
|---|---:|---:|---:|
| Preserves power-user density | Excellent | Weak | Moderate |
| Content-first hierarchy | Excellent | Good | Moderate |
| Distinct from stock Material | High | High | Low–moderate |
| Accessibility and reduced-motion fit | Excellent | Good | Moderate |
| Fits existing architecture | Good | Fair | Good |
| Implementation risk | Medium | High | Medium–high |
| Meets all stated principles | Yes | No | No |

### R0.7 Recommendation and guardrails

Choose **Direction A — Signal: crisp editorial**.

It is the only candidate that satisfies all five principles and all anti-goals. It converts the audit's strongest existing assets—compact density, multiple post modes, configurable swipes, comment tooling, media gestures, and deep theming—into a coherent identity instead of discarding them. It also creates a visible early result: a coherent post row and feed can ship before the riskier thread, viewer, and tablet work.

The recommendation includes these non-negotiable guardrails:

1. Do not create a card around every piece of content.
2. Do not remove functionality to obtain visual calm.
3. Do not use per-subreddit accent color until the core role system is correct and contrast-tested.
4. Do not animate recycled rows or use motion as loading decoration.
5. Do not make dynamic color bypass or invalidate existing custom themes.
6. Do not migrate the core experience to Compose as part of the visual redesign.
7. Do not let Material 3 Expressive styling override product hierarchy.

### R0.8 Feasibility register

| Concept | Android implementation | Existing support | Dependency / scale assessment |
|---|---|---|---|
| Material 3 foundation | `Theme.Material3.DayNight`, `TextAppearance`, `ShapeAppearance`, `MaterialCardView`, `MaterialDivider` | Material Components 1.14.0 | No new dependency; broad token migration, medium effort |
| Material You | `DynamicColors` / dynamic theme overlays on API 31+, static DayNight fallback below | Material 1.14.0 and existing `MaterialYouUtils` snapshot code | No new dependency; custom-theme exporter is a large, isolated change |
| Material 3 Expressive | `Theme.Material3Expressive.*`, supported widget styles, motion attributes, standard fallbacks | Material 1.14.0 | No new dependency; use only shipped component-specific APIs |
| Dense post/comment system | RecyclerView, Paging 3, ConstraintLayout, stable ViewHolders, existing swipe/collapse engines | Already present | No new dependency; adapter/layout consolidation is a large rewrite and must be phased |
| Media continuity | Activity transitions/shared elements, `postponeEnterTransition`, Glide/BigImageViewer | Glide and viewer stack present | No new dependency; five viewer variants make this medium/high risk |
| Predictive back | AndroidX Activity `OnBackPressedDispatcher` and predictive-back integration | Activity 1.13.0 is present | No new dependency; vendored Slidr/Hauler migration is high risk |
| Edge-to-edge and large screens | `WindowInsetsCompat`, existing `layout-sw600dp`; optional adaptive pane work | Manual insets and tablet qualifiers exist | No new dependency for insets; true two-pane requires a major Activity/fragment navigation refactor |
| Haptics | Android `Vibrator` constants respecting system settings | Existing swipe haptic | No new dependency; low effort |
| Type voice | Platform UI type plus existing configurable content fonts | Current triple font system | No new dependency; text-attribute migration required |

### R0.9 Direction-level success criteria

R1–R4 are valid only if the finished experience can demonstrate all of the following:

- posts and comments establish hierarchy before app chrome on every redesigned core screen;
- compact and default modes preserve direct access to voting, commenting, saving, sharing, and configured swipe actions;
- the same semantic hierarchy works with imported themes, Material You, static fallback, light, dark, and AMOLED;
- every primary action is reachable with one hand and operable with TalkBack at a 48dp minimum target;
- text remains usable at 200% font scale and layouts mirror correctly in RTL;
- media continuity and motion explain state changes rather than competing with reading;
- the app remains launchable and behaviorally complete after every implementation epic.

### R0 decisions requested

1. Approve **Direction A — Signal: crisp editorial**, or select B or C.
2. Approve the **signal rail** as Continuum's signature device, with per-community color limited to that rail/source marker and opt-in only.
3. Approve a staged Material You policy: opt-in while the 87-field theme bridge is validated, then eligible to become the default after lossless theme and contrast tests.

## R1. Structure

> **Pass status:** R1 is finalized in this run and follows the approved working direction: **Signal — crisp editorial**.

### R1.1 Current skeleton and structural constraints

The current shell is flexible but has no stable rank:

- `MainActivity` combines a drawer, a collapsing toolbar, dynamic feed tabs, a pager, an optional custom bottom bar, and a compose action.
- The bar is an action palette, not navigation: it has no selected destination, supports legacy 2/4-slot configurations, mixes commands such as refresh/sort with routes, and auto-hides during feed scrolling. It defaults off.
- The rail appears only on landscape/`sw600dp` resources and only when the bar preference is enabled. It does not currently share a complete destination model with the phone bar, and its inbox badge is skipped.
- The merged drawer contains most breadth—account, post collections, subscriptions, multireddits, history, and preferences—but one constructed section is empty and filter usage is not mounted.
- Feed tabs are account-scoped and variable-length. The old six-tab shape is migration history, not the current product limit.
- Navigation is manual Activities/Intents plus `ViewPager2`; there is no navigation graph. Deep links, notification routing, new-window tasks, and the serialized resume stack are established behavior that must survive.
- Post detail already has separate post and comment panes in landscape/`sw600dp`; broad two-pane redesign should preserve that capability rather than describe the app as having none.
- Anonymous use is capability-based, not a reduced rendering of the signed-in shell. Inbox/profile actions and several account destinations are unavailable or behave differently.

The structural problem is therefore not “add a bottom bar.” It is separating **destinations**, **local tabs**, and **one-off commands** into three explicit layers.

### R1.2 Navigation decision: four corners, local context, breadth in the Library

Use a **bottom-led hybrid on phones** and a **labeled navigation rail on large screens**.

#### Phone: persistent four-destination navigation

| Slot | Signed in | Anonymous | Behavior |
|---|---|---|---|
| 1 | Feed | Feed | Dynamic feed tabs; reselect scrolls to top |
| 2 | Search | Search | Same destination as the contextual top-bar action |
| 3 | Inbox | Saved | Inbox badge/notifications for signed-in; local saved content for anonymous |
| 4 | Library | Library | Account, subscriptions, collections, history, filters, and settings |

- The semantic bar is persistent on phones. It does not auto-hide during feed scrolling; predictable thumb reach is worth the vertical cost, and the top app bar/feed tabs may still collapse content chrome.
- The bar is destinations-only. Refresh, sort, layout, filter, go-to-top, and similar commands do not masquerade as tabs.
- Search remains available as a top-bar shortcut because search is contextual on many list screens, but both entry points resolve to the same Search destination.
- Account identity is the leading account avatar/menu in the top app bar. It opens the account switcher, profile/manage-account destinations, add account, and sign-in state without adding a fifth bottom slot.
- Post creation is a Feed top-bar action opening the existing post-type sheet. Inbox keeps its message-compose FAB because composing is that screen's primary action. Detail keeps inline reply and the existing labeled thread-jump control; there is no universal floating action button.
- Full-screen media hides application navigation. Returning restores the exact feed/detail position.

#### Library: breadth without competing with navigation

The existing drawer becomes a first-class Library destination. An edge swipe may continue to open it, but it has the same identity whether opened from the edge, the bottom destination, or a large-screen rail.

Its information architecture is:

1. **You** — account switcher, current profile, account management, sign in/add account, anonymous state.
2. **Library** — subscriptions, multireddits, followed users, favorites, history, recent, and account post collections.
3. **Organize** — main-page tabs, post/comment filters, filter usage, reminders, themes, and layout/customization entry points.
4. **System** — Settings, API/security, backup, app information, and existing quick preferences.

This preserves every current destination while removing the empty section and making the currently unmounted filter-usage capability reachable if it remains supported. Existing labels may be clarified, but no feature is removed to simplify the drawer.

### R1.3 Three navigation layers

#### Global destinations

Only the four bottom/rail destinations are global: Feed, Search, Inbox/Saved, and Library. A destination registry owns each item's stable ID, icon, label, capability requirement, host/intent factory, selected state, badge provider, and reselect behavior.

This registry is an application concept, not a requirement to introduce Jetpack Navigation. It can drive the existing Activity/Intent architecture while eliminating duplicated Main/detail bar mappings.

#### Local context tabs

Tabs remain local to the content they change:

- dynamic main feed sources on Feed;
- Posts / Subreddits / Users on search results;
- Notifications / Messages in Inbox;
- Posts / About for subreddits and multireddits;
- Posts / Comments for users;
- Settings retains its fragment back stack.

Local tabs are not additional global destinations and do not appear in bottom navigation. Feed tabs are scrollable and remain visible while the title chrome collapses so users do not lose their current source.

#### Commands and sheets

Refresh, sort, layout, filter, mark-all-read, and similar actions are contextual commands. They live in a top-bar overflow, an inline control, or a bottom sheet appropriate to the screen. A command may be pinned by existing settings but never occupies a navigation slot without an explicit label explaining that it is a shortcut.

### R1.4 Top-level behavior

- **Tap active Feed:** return to the current feed tab and scroll to top. Pull-to-refresh remains the explicit refresh gesture; tapping at the top does not unexpectedly refresh.
- **Tap inactive global destination:** open or reuse that top-level host and apply the correct account/anonymous variant.
- **Tap Search:** open the existing Search activity; if already open, focus the query without discarding scope/history.
- **Tap Inbox/Saved:** return to the existing host and correct tab; reselect Inbox returns to Notifications when currently in Messages.
- **Tap Library:** open the Library hub at the last selected group when state is restorable; otherwise open You.
- **Account switch:** preserve current behavior by rebinding root navigation and clearing incompatible detail stacks.
- **Deep link/notification/share intent:** resolve directly to the requested content and establish the correct root/back stack; do not force a route through Feed.
- **Legacy custom actions:** keep the existing 2/4-action setting reachable under Advanced during transition. It is explicitly labeled **Custom actions**, not navigation, and cannot be the default for a new install. Account-scoped stored values are migrated rather than discarded.

### R1.5 Top app bar discipline

Every redesigned root or detail screen uses one `MaterialToolbar`/`MaterialToolbar` host with:

- navigation icon only when there is a real parent destination;
- a contextual title that may become the feed source name while scrolling;
- no more than two trailing actions before overflow;
- account avatar as the leading identity control on roots;
- no second toolbar embedded in a collapsing header.

Subreddit and user headers collapse behind this bar. On collapse, the bar retains a compact source marker, title, and the primary Follow/Join action so context and the main commitment do not disappear.

### R1.6 Large screens and foldables

#### Medium width: labeled rail

At `sw600dp` or equivalent landscape width, replace bottom navigation with a labeled rail using the same semantic destinations, selected state, and badges. Content receives the reclaimed bottom inset. The existing Feed/Post/Comments split remains available.

#### Expanded width: list/detail panes

Recommend phased two-pane behavior for high-value paths:

1. Feed list | post/comments detail;
2. subreddit or user list | selected post/comments;
3. search results | selected post;
4. Inbox conversation/message list | selected thread.

Compact and medium widths retain one content pane plus rail. The expanded layout is a target Big bet, not a reason to fragmentize every Activity in the first release. Hosts may be embedded destinations or purpose-built pane Activities, but behavior remains identical to the compact route.

#### Foldables

- A closed/folded compact window uses phone navigation.
- An opened device adapts by available width, not device name: medium uses the rail; expanded uses panes.
- State must survive folding. No destination may depend on a permanent physical hinge.
- Spanning the hinge is used only when both panes need meaningful width; otherwise the app avoids placing controls or reading text across the fold.

Implementation can use existing resource qualifiers and `WindowMetrics`/`WindowLayoutInfo`; no new UI dependency is required, but broad two-pane hosting is a major navigation refactor.

### R1.7 Predictive-back contract

All back paths move toward AndroidX Activity's `OnBackPressedDispatcher`, which exposes started/progressed/cancelled predictive-back events through `OnBackPressedCallback`.

The order is consistent:

1. close an open dialog, menu, or bottom sheet;
2. close inline search/edit state;
3. close the Library hub/drawer;
4. leave the current detail/destination through the normal parent route;
5. at a root, apply the existing configured root-back behavior.

Preserve special contracts:

- composers prompt before abandoning an unsent draft or active submission;
- media stops playback and resolves drag/Hauler/Slidr dismissal through one back policy;
- lock screen continues consuming back until authentication succeeds;
- settings pops its fragment back stack;
- account switching clears incompatible detail Activities.

Touch drag remains available where users value it, but the equivalent system back gesture must preview the same outcome and cancel cleanly. A screen must always remain dismissible from a visible toolbar Up control when gestures are disabled.

### R1.8 Old → new navigation map

```text
OLD CURRENT SHELL                         NEW SIGNAL SHELL
──────────────────                         ───────────────
MainActivity                              Feed destination
  Drawer = breadth + account               Library destination
    account / posts / subs / prefs           You / Library / Organize / System
    multireddits / history / themes
  Optional 48dp action bar                Persistent semantic navigation
    2 or 4 arbitrary slots                 Feed / Search / Inbox|Saved / Library
    no selected state                       selected + badges + reselect contract
    auto-hides while scrolling              persistent outside full-screen media
  Dynamic top feed tabs                   Local context tabs
  Per-screen toolbars and FABs              One top app bar; commands in overflow/sheets
  Rail on some large layouts              Labeled rail on all medium-width layouts
  Stretched single pane                   Phased list/detail panes when expanded
  Slidr / Hauler / mixed back             OnBackPressedDispatcher predictive contract
  Manual destination duplication          One semantic destination/action registry
```

### R1.9 Migration and feasibility

#### Migration order

1. Introduce the versioned semantic destination registry without changing stored preferences.
2. Distinguish an absent preference from an explicit “custom actions disabled” choice.
3. Add Feed, Inbox/Saved, Library, selected state, and account-capability rules.
4. Apply signed-in and anonymous defaults independently, preserving account-scoped migration.
5. Rebind the current Main bar and then eligible detail hosts; do not force a bottom bar onto post detail.
6. Rebind rail labels, selected state, and badges.
7. Regroup Library rows and make filter usage reachable.
8. Add shared back handling, then pane hosting as a separate Big bet.

#### Likely implementation surface

- Shell/layouts: `activity_main.xml`, `app_bar_main.xml`, `bottom_app_bar.xml`, `layout-land/app_bar_main.xml`, and navigation menu resources.
- Hosts: `MainActivity.java`, `NavigationWrapper.java`, and the subreddit/user/multireddit hosts that currently embed the bar.
- Settings/migration: `CustomizeBottomAppBarFragment`, account-scoped keys, and the existing migration stage.
- Library: `NavigationDrawerRecyclerViewMergedAdapter` and its section adapters.
- Back/media: `BaseActivity`, `ViewPostDetailActivity`, Slidr/Hauler hosts, and composer/lock-screen special cases.
- Deep links/resume: `LinkResolverActivity`, `ShareDataResolverActivity`, notification routing, and `ResumeState`.

No new dependency is required for the four-destination shell or predictive-back callbacks. The major rewrite flags are the semantic registry, account migration, duplicate hosts, and expanded-width pane hosting; all can ship behind a versioned navigation mode.

### R1.10 R1 acceptance criteria

A structure implementation is not complete until:

- signed-in and anonymous users each have four stable, labeled destinations with correct selected state;
- every existing account, library, filter, theme, reminder, settings, and utility destination remains reachable;
- dynamic feed tabs remain account-scoped, horizontally discoverable, and visible during long feeds;
- root bars do not auto-hide, while full-screen media can hide all chrome;
- search, deep links, notifications, share intents, new-window tasks, and resume-stack replay retain their behavior;
- draft, lock-screen, settings, drawer/sheet, and media back contracts are preserved and predictive where supported;
- medium-width layouts use a labeled rail with inbox/saved badge parity;
- expanded-width pane work preserves the same routes and state as compact navigation;
- TalkBack announces destination, selected state, badge, and reselect result;
- light, dark, AMOLED, RTL, 200% font, gesture-disabled, and fold/unfold states pass.


## R2. Screens (links to docs/redesign/)

### R2.1 Feed — [mockup](redesign/feed.html)
- **Intent:** content owns first paint; reach everything with the thumb; never lose place.
- **Layout:** top bar (screen title + search action + overflow only) → feed tab strip (existing 6 tabs, tonal selected pill) → single-column rows → bottom bar (Feed/Search/Inbox-badged/Profile). Drawer demoted per R1. No banner, no hero, no double toolbar.
- **States:** first load = skeleton rows (tonal blocks, no shimmer); pull-to-refresh = themed spinner (keep); empty (no subscriptions/results) = editorial illustration + copy + CTA (Direction-A moment); error = existing error art + retry; offline = inline banner, list stays.
- **Interactions:** swipe actions unchanged (3-level config is a lead — keep); tap row → detail with fade-through (text) or shared-element (media, UI-020); tab double-tap scrolls to top (exists — keep); list position retained across rotation/theme change.
- **Theme:** tonal cards on tinted background, hairline borders instead of shadows; meta in secondary, scores tabular bold; AMOLED = true-black bg + raised cards. Dynamic palette via exporter; static Indigo fallback.
- **RTL/large font:** single meta line ellipsizes (never wraps); action row holds at 200% (vote cluster pinned start, overflow absorbs); bottom bar unlabeled-icons + TalkBack titles (UI-008).
- **Build notes:** reorder existing row internals (`item_post_compact*`, card_3 family; card_2 deprecates per decision); `MaterialDivider` between compact rows, tonal card for default/media; Paging + DiffUtil untouched; `CompactThumbnailPreloader` + aspect reservation kills load jump (UI-013).

### R2.2 Post card (compact / default / media-first) — [mockup](redesign/post-card.html)
- **Intent:** one card language, three densities; mode switch reflows the same information, never reorders it.
- **Layout (all modes):** quiet meta line (`avatar? subreddit · user · time`, 12sp secondary, max 1 line) → title (16sp semibold, 3-line cap) → optional badge line (flair/spoiler/NSFW/type — restored to card_3, UI-011) → optional body excerpt (4 lines, content voice) or capped 16:10 media → action row (up / score / down / comments / spacer / save / share, all 48dp, tabular score in fixed column).
- **Compact:** 72dp rounded thumb trailing; meta + title + badges stack leading. **Default:** text-first, badges always visible. **Media-first:** aspect-reserved preview (16:10 default, native ratio when known pre-load); tap → shared-element viewer; play/type overlays 48dp.
- **Vote affordances:** 48dp up/down with accent fill + `CLOCK_TICK` haptic on commit; score column fixed width so arrows never shift (keep `comment_score_min_width` token, extend to cards).
- **States:** image loading = reserved-ratio tonal block + indicator; error = retry tile (keep string); NSFW/spoiler = blurred preview + explicit reveal (existing behavior, keep).
- **Theme/RTL/font:** as Feed; badge chips use tonal fill + AA text; meta `toStartOf`-safe; title cap prevents action push at 200%.
- **Build notes:** converge `item_post_compact*` + `item_post_card_3_*` onto one ViewHolder family over time (Big bet, phased: theme + reorder first, dedupe layouts second); card_2 enters deprecation (hide from picker, keep rendering old prefs); ViewBinding names preserved until the dedupe epic.

### R2.3 Post detail + comments — [mockup](redesign/post-detail.html)
- **Intent:** the detail header reads as *source*, the thread as *conversation*; the boundary between them is unmistakable; long threads stay navigable one-handed.
- **Layout:** top bar (back + `subreddit · Comments` + search + overflow) → full-bleed detail header (meta line, 19sp bold title, body voice 15/1.55, badge line, action row with reply) → sticky thread bar (count pill + sort + collapse-all) → comment rows → MovableFAB (jump, labeled per UI-019).
- **Comment row:** header trims to author · score · time (+flair under name, never in the score lane); body in content voice; toolbar keeps vote cluster + reply + overflow (save moves to overflow on narrow rows — existing compaction engine, tightened defaults); divider only between top-level threads, guides within.
- **Depth visualization:** 2dp tonal guides, 7-color rotation (keep `CommentIndentationView`); text column stops shrinking past level 4 (guides continue full-depth); collapsed thread = tonal row with `+N` pill + author + tap-to-expand (keep pixel-stable behavior).
- **Quick navigation:** FAB tap/long-press (keep) + thread bar count ("318 comments") + in-comment search panel (keep, best-labelled control set) + collapse-all toggle (new, cheap: drives existing collapse prefs).
- **Gestures/haptics:** header tap collapses (keep); swipe actions unchanged (keep config); `CLOCK_TICK` on vote/collapse; swipe-back via predictive path (UI-021).
- **States:** post load = skeleton header + thread shimmer blocks (no shimmer animation — static blocks per principle 4); empty = `no_comments_yet` + be-first CTA; error = existing error art + retry; offline = cached thread if present, else error.
- **Theme/RTL/font:** header surface raised one step above thread bg; guides use outline-variant role; time/score tabular; depth indents mirror in RTL (start-based); 200% font wraps body, header meta ellipsizes.
- **Build notes:** flatten single-block markdown bodies to TextView spans over time (UI-017, Big bet); keep `CommentsRecyclerViewAdapterNew` + `ListAdapter` diffing; collapse semantics exposed for TalkBack (UI-018); ViewPager2 multi-post swipe retained.

### R2.4 Media viewer — [mockup](redesign/media-viewer.html)
- **Intent:** media is the hero; chrome appears on demand and gets out of the way; every viewer behaves the same.
- **Layout (unified contract across image/gif/video/gallery/imgur):** scrim top bar (back + title/sub + download + share) → edge-to-edge stage → pager dots (galleries) + subtitle `N of M` → scrim bottom action bar (rotate, download, share, wallpaper/info). Chrome auto-hides on zoom/scroll, single tap restores. One contract replaces five dialects.
- **Enter/exit:** shared-element from feed thumb (`postponeEnterTransition` with Glide/BigImageViewer); drag-down anywhere dismisses through the predictive-back path; fallback = fade (never the legacy slide).
- **Gestures:** pinch + double-tap (images, keep), ported pinch/pan (video, keep — consolidate onto `ZoomLayout` only if it proves equivalent in the motion epic; default = keep hand-rolled), pager swipe + arrows.
- **Controls:** 5-action bar keeps its labels (audit exemplar); download/share show **inline progress in their own slot** (UI-023); over-media whites become scrim roles (themed, not hardcoded); shadowbox panel keeps pixel-stability, gains icon label.
- **States:** loading = spinner on black (keep); error = retry tile (keep string); NSFW = blurred + reveal (keep); gallery = dots + `N of M` in subtitle.
- **Theme/RTL/font:** viewer is intentionally monochrome (black + white + accent dots) in all 3 modes; subtitles ellipsize; rotate L/R swap meaning correctly in RTL (labels already distinguish).
- **Build notes:** unify the 5 activities behind one chrome contract incrementally (bar layouts first, transitions second, activity merge last or never — merging is NOT required); `BigImageViewer.cancelAll` discipline stays; `animateLayoutChanges` leaves PlayerView/AppBar (UI-023).

### R2.5 Subreddit + User profile — [mockup](redesign/subreddit-profile.html)
- **Intent:** identity in one glance, content one scroll away; profile and subreddit share one header contract.
- **Layout:** 88dp banner as atmosphere (hidden when none — no more 160dp wall) → card with 64dp avatar overlapping the banner edge, name + stats, bordered Join/Follow ≥48dp → one-line stats (members · online / karma · cakeday, tabular) → 2-line clamped description (tap expands) → sticky tabs (Posts/About, Posts/Comments).
- **Sticky behavior:** on collapse the toolbar keeps avatar-dot + name + Join/Follow (primary action never scrolls away — fixes the weak subscribe affordance).
- **User extras:** Follow + Save share the action line (save keeps its documented placeholder strategy, grows to 48dp); own profile swaps Follow for Edit.
- **States:** banner/icon loading = tonal placeholders (new); private/banned/quarantined = editorial notice + action (opt-in/quarantine accept stay exactly as functional today); About tab keeps sidebar content with the 144dp magic replaced by real insets.
- **Theme/RTL/font:** stats tabular; description clamp holds at 200%; start/end mirroring; Join/Follow label + state announced.
- **Build notes:** shrink header layouts (`activity_view_subreddit/user_detail.xml` + sw600dp/land twins); bordered chip style (replaces `#00000000` stroke hack); label both FABs; banner placeholder drawables; keep dual tab theming + `TabLayoutMediator` wiring.

### R2.6 Search — [mockup](redesign/search.html)
- **Intent:** type, disambiguate scope, pick — in that order, with nothing else competing.
- **Layout:** search bar (back + query + clear only) → single scope pill row (`All of Reddit ▾`) → Recent section (static two-line rows, tap fills, arrow re-runs) → Trending as horizontal chips → results keep tabbed ViewPager (Posts/Subreddits/Users) with result rows restyled to the card language.
- **Declutter:** random-subreddit, link-handler, incognito move to overflow (all survive — no removal); history/delete-all leave the scope row for the Recent header (`View all · Clear`, labeled).
- **No marquee, single list:** recents truncate statically (two lines: query + scope); autocomplete renders inline in the same list (kills nested RecyclerViews); trending chips replace mixed-radius cards.
- **States:** empty query = recents + trending; typing = inline autocomplete w/ spinner row; no results = editorial empty + scope suggestion ("try All of Reddit"); history empty = `no_search_history` (keep).
- **Theme/RTL/font:** 17sp query (down from `font_20`, still largest input); scope pill tonal; rows 56dp+; mirroring safe; TalkBack complete (every icon labeled incl. result FAB).
- **Build notes:** rebuild `activity_search.xml` around one list (`ConcatAdapter`: recents/autocomplete/trending); retire `RelativeLayout` scope row + grid + nested scrollers; keep `SearchActivity` intents, history storage, result activities (row layouts only).

### R2.7 Compose + Inbox — [mockup](redesign/compose-inbox.html)
- **Intent:** composing feels safe (nothing vanishes, drafts survive); inbox scans in seconds, unread shouts.
- **Compose layout:** top bar (close + `New {text,link,image…} post` + Post action, enabled when valid) → one modern destination row (icon + subreddit + account + Rules link) → labeled Title field (semibold, title voice) → labeled Body field (content voice, `Markdown supported` caption — hint never replaces the label) → sticky labeled markdown bar (48dp, `adjustResize` kept). Flair/spoiler/NSFW chips with fixed 8dp gaps; notification toggle row kept.
- **Compose states:** rotation-safe send (keep) + draft autosave/restore (new — the biggest functional gap); upload progress inline for media posts (new); submit error as inline banner with retry (new, replaces silent failure); crosspost keeps attribution.
- **Inbox layout:** Notifications/Messages tabs (keep) → rows with author · time meta, bold subject, one-line preview; **unread = accent edge + bold + announced** (fixes code-only unread); chat bubbles keep 70% cap + 12dp padding; copy/reply labeled; compose FAB kept (audit exemplar).
- **Theme/RTL/font:** labels persist at 200% (fields grow, never overlap); picker rows mirror without magic offsets (fixed as part of UI-026); unread edge mirrors to inline-start.
- **Build notes:** `TextInputLayout` (or persistent header labels) across 6 compose activities; shared scaffold already exists — label + draft + progress deltas only; inbox row re-layout + unread flag plumbing (adapter + seen-state store, additive).

### R2.8 Settings + Navigation + Onboarding — [mockup](redesign/settings-nav.html)
- **Settings intent:** the same framework rows, findable. Keep `PreferenceFragmentCompat` + search; group top level into cards with summaries (Appearance / Feeds & posts / Gestures / Media / Account / System); fix `toolbarId` copy-paste + raw `16sp`; label custom prefs; keep 48dp clear + backup/API-keys destinations.
- **Drawer intent (per R1):** the library. 9 sections → You / Library / Filters / System; account switcher + karma kept; header uses insets (kills 40dp hack); rows 52dp+ with leading glyphs; all destinations still reachable, nothing removed.
- **Onboarding intent:** first impression is Continuum, not Reddit's OAuth WebView. One primer screen (value props + Sign-in / Anonymous choices) → existing WebView / Custom-Tab / AppAuth paths unchanged; 2FA line becomes contextual (shows only at the 2FA step); offline retry kept; lock-screen Lottie gains `importantForAccessibility=no` + respects reduced motion.
- **Theme/RTL/font:** settings rows inherit the type scale (keep `PreferenceTitleTextStyle`); drawer mirrors; primer fits small screens + 200% font (single CTA column).
- **Build notes:** drawer regroup = adapter section mapping + header insets (S–M); settings = grouping XML + two one-line fixes; primer = one new Compose/Views screen in front of `LoginActivity` paths (no auth-logic touch); biometric depth stays as-is.

R2 covers 10/10 screen groups. Open R2 questions for the maintainer: skeleton-block vs shimmer (spec: blocks); collapse-all default; ZoomLayout verdict deferred; banner-hide rule; trending chips confirmed; draft autosave retention window (propose 7 days).

## R3. Design System

> Implementable spec. Every token names its Android target so the build agent never guesses. `?attr/` names below that do not exist yet are to be created; existing ones are reused.

### R3.1 Color
- **Roles used (M3 core):** `colorPrimary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `surfaceContainerLow/High`, `outlineVariant`, `error` + `colorAccent` retained as the legacy alias of `colorPrimary` during migration (not removed — user themes reference it).
- **Dynamic:** M3-scheme→`CustomTheme` exporter (UI-003): wallpaper palette in, 87 fields out (light/dark/amoled). Surfaces from neutral roles, accents from accent roles, read-post/filled/tab/navBar fields derived with fixed transforms (documented in code, golden-tested).
- **Static fallback:** Indigo lineage (`#0336FF/#002BF0/#FF1868` light; `#242424/#121212` dark; `#000000` AMOLED) for <S, opt-out, and first run before wallpaper read.
- **AMOLED rules:** true-black `surface` + `backgroundColor`; cards raised one tonal step (never pure-black-on-black); dividers `outlineVariant` at 60%; media viewer stays monochrome-black in all modes.
- **Migration map (87 fields → roles, excerpt):** `backgroundColor→surface`, `cardViewBackgroundColor→surfaceContainerLow`, `filledCardViewBackgroundColor→surfaceContainerHigh`, `primaryTextColor→onSurface`, `secondaryTextColor→onSurfaceVariant`, `colorPrimary/Accent→primary roles`, `dividerColor→outlineVariant`, `navBarColor→surface (edge-to-edge scrim)`, tab collapsed/expanded sets → `primaryContainer/onPrimaryContainer` pairs. Full 87-row table lives in the UI-003 epic spec (delivery), not here.

### R3.2 Type scale
- **Roles:** `Title` (card/detail titles, 16sp semibold, 3-line cap), `Body` (post/comment/message text, 14–15sp, 1.45–1.55 line height), `Meta` (one-line meta, 12sp secondary), `Caption` (badges, counts, timestamps-inline, 11sp), `Input` (fields, 16–17sp), `Display` (empty-state + viewer titles, 20sp bold).
- **Styles:** `TextAppearance.Continuum.{Title,Body,Meta,Caption,Input,Display}` in `styles.xml`, each pointing at the existing `?attr/font_*` scale so user font-size prefs keep working (UI-004 does the wiring + Large-bug fix, already shipped).
- **Weights:** regular 400 body/meta, semibold 600 titles/scores, bold 700 reserved for Display + unread subjects. No italic styling (user fonts supply their own).
- **Line heights:** unitless via `lineHeight` on Body (1.5) and detail body (1.55); everything else default.
- **Font choice + licensing:** UI typeface = system default (zero APK cost, respects OEM/user settings); content families = the already-bundled set (Inter, Manrope, Noto Sans, Atkinson Hyperlegible et al. — all SIL OFL or Apache-licensed; keep license files with the fonts, never add a font without one). Tabular numerals (`font-variant-numeric` equivalent: use `android:fontFeatureSettings="tnum"` where supported, else per-view) for scores, counts, stats.
- **Attrs:** existing `font_default/10/12/16/18/20`, `title_font_*`, `content_font_*` retained; add `caption` step (`caption_font`, 11sp at Normal) to close the badge-size gap.

### R3.3 Shape, spacing, elevation/tonal rules
- **Shape appearances** (`styles.xml`): `Shape.Continuum.Small` 8dp (thumbs, media, controls), `Medium` 12dp (cards, rows, dialogs), `Large` 16dp (sheets, viewer panels); 28dp reserved for bottom-sheet container + FAB morph. No other radii.
- **Spacing tokens** (`dimens.xml`): `space_2/4/8/12/16/24/32` on the 4dp grid (measured histogram: 16/24/8/0/32/4 dominate — the scale ratifies existing practice). Component paddings compose from these; magic numbers (32dp scope offset, 144dp sidebar pad, 40dp header margin) are deleted, not tokenized.
- **Elevation:** zero shadow elevation on cards/rows — depth is tonal (`surfaceContainerLow` on `surface`, borders `outlineVariant` 1dp). Real elevation survives only for: FAB/rail bar (3dp), bottom sheets/dialogs (16dp scrim pattern, keep), viewer chrome (scrim, not shadow).

### R3.4 Iconography
- **One set:** current 24dp rounded vectors (audit: coherent enough to keep). Weights: filled for selected/active (vote committed, joined, saved), outline for resting. Never mix on one row.
- **Sizes:** 24dp glyphs in 48dp targets (UI-007); 20dp dense variant allowed only inside the 72dp compact thumb badge; status glyphs (archived/locked/crosspost) 16dp inline with badges, not 24dp floated.
- **Tint:** theme roles only (`onSurfaceVariant` resting, `primary` committed); `autoMirrored` on every directional icon (arrows, reply, send); no `app:tint` literals, no `@android:color` tints (closes UI-011 fallback + DS-1 hex).

### R3.5 Motion tokens + transition catalog
- **Durations:** `motion_150` state flips (vote, collapse, toggle, chip select); `motion_200` container moves under 300dp (sheets, search panel, expand); `motion_250` cross-screen (list→detail fade-through, viewer enter/exit). No other durations.
- **Easing:** `emphasized_in` (enter/expand), `standard_out` (exit/collapse/dismiss); linear only for progress indicators.
- **Catalog:** (1) fade-through list→detail (text rows); (2) shared-element thumb→viewer (media rows, `postponeEnterTransition`); (3) fade+scale dialog/sheet enter; (4) predictive slide-back for Slidr/Hauler paths (UI-021); (5) collapse→badge morph (comments, layout transition scoped to the row only — never `animateLayoutChanges` on recycled roots).
- **Haptics map:** swipe arm `CLOCK_TICK`; vote commit `CONFIRM` (or `CLOCK_TICK` <30); collapse `CLOCK_TICK`; send/post `CONFIRM`; refresh tick on trigger; all behind the existing user toggle AND the system setting (UI-022).

### R3.6 Component specs (attr/style/token map)
- **Post card (3 modes):** `MaterialCardView` tonal (`surfaceContainerLow`, `Medium` shape, 1dp `outlineVariant`) for default/media; compact = transparent + `MaterialDivider`; meta `Meta`, title `Title` (3-line), badges `Caption` chips (tonal fill, 8dp gaps), actions 48dp `Title`-scale score tabular. Attrs: `?attr/cardViewBackgroundColor`→roles per R3.1; `space_8/12/16`; `Shape.Continuum.Medium`.
- **Comment row:** header `Meta` (author semibold + score tabular + time), body `Body`, toolbar vote cluster + reply + overflow (compaction engine kept); guides `outlineVariant` 2dp/12dp; collapsed row `Caption` + `+N` pill (`primaryContainer`).
- **Chips/badges:** `Caption`, tonal fill, 6dp radius (`Small` family), 8dp gaps, 48dp min touch for interactive (Join/Follow), static for flair.
- **Buttons:** M3 `MaterialButton` styles as-is; primary = filled `primary`, secondary = tonal, tertiary = text; destructive (hide) keeps red band mapping from swipe colors.
- **Top bars:** `MaterialToolbar` + title `Title`, ≤2 actions + overflow; collapse keeps avatar-dot + name + primary action.
- **Bottom sheets/dialogs:** `Large` shape, tonal surface, drag handle, `motion_200 emphasized_in`; rows 56dp+ with leading glyph + summary `Caption`.
- **Empty states:** editorial illustration slot + `Display` title + `Body` copy + primary CTA (the Direction-A moment); per-screen copy in R2.
- **Swipe bands:** keep per-action colors (`colors.xml` bands) — already the clearest level signal; text labels added next to icons at level 2+ for TalkBack.

## R4. Delivery

> Ship order keeps the app shippable every step: foundation → visible rows → threads → headers/search → media/motion → platform. Safe wins front-load visible improvement; Big bets land after their foundations.

### EPIC-01: Tokens + theme foundation
- Goal: every screen reads from one system; dynamic color works without breaking user themes
- Scope: spacing/shape/type/color tokens, divider + tint + target sweeps, M3 exporter
- Replaces/absorbs: UI-001, UI-002, UI-003, UI-004 (remainder; bug line shipped), UI-005, UI-006
- Design refs: R3.1–R3.4
- Build notes: `dimens.xml`, `styles.xml` (`TextAppearance.Continuum.*`, `Shape.Continuum.*`), exporter in `MaterialYouUtils` + full 87-row map, Roborazzi goldens per mode
- Effort: L
- Risk: med (theme regressions; golden coverage is the net)
- Depends on: none
- Acceptance criteria: zero hardcoded values in touched layouts; AA in light/dark/AMOLED; RTL + 200% font pass; user themes migrate losslessly
- Type: Safe win (foundational, invisible when right)

### EPIC-02: Accessibility + touch floor
- Goal: the app is fully operable by touch-all-sizes and TalkBack users
- Scope: 48dp audit fixes, all icon labels (rows, search, FABs, viewer, copy), rail badge parity, collapsed-thread semantics, marquee removal
- Replaces/absorbs: UI-007, UI-008 (shipped), UI-014, UI-019, UI-025 (a11y slice), UI-027 (FAB slice)
- Design refs: R1.6 rail note, R2 per-screen a11y bullets, R3.4
- Build notes: `NavigationWrapper.setInboxCount` rail branch, per-layout padding/delegate pass, TalkBack smoke script (manual; CI has no screen reader)
- Effort: M
- Risk: low
- Depends on: EPIC-01 (labels ride the token pass per screen)
- Acceptance criteria: TalkBack traverse of feed→detail→thread→viewer→compose announces purpose + state; every action ≥48dp on device; no marquee anywhere
- Type: Safe win

### EPIC-03: Feed + post cards
- Goal: one card language, quiet meta, bounded titles, skeleton states, no jank
- Scope: compact/default/media-first reorder + caps + badge restore, skeleton/empty design, bind-cost + aspect-reservation perf, card_2 deprecation (hide from picker, keep rendering)
- Replaces/absorbs: UI-009, UI-010, UI-011 (remainder), UI-012, UI-013
- Design refs: R2.1, R2.2 + mockups
- Build notes: `item_post_compact*` + `item_post_card_3_*` reorder/theme first; ViewHolder dedupe second (phased Big-bet tail); `ConcatAdapter` untouched (Paging stays)
- Effort: L
- Risk: med (highest-visibility surface; adapter is 7457 lines)
- Depends on: EPIC-01, EPIC-02
- Acceptance criteria: meta one line at 200% font; titles bounded; skeleton→content sans jump; scroll profile improved; card_2 hidden from picker; 3 modes × 3 themes verified
- Type: Big bet

### EPIC-04: Comments + detail
- Goal: unmistakable post/thread boundary, readable deep threads, keep jump nav
- Scope: detail header treatment + thread bar (count/sort/collapse-all), row trim + toolbar defaults, markdown flattening, depth cap + semantics, FAB labeling
- Replaces/absorbs: UI-015, UI-016, UI-017, UI-018, UI-019 (remainder)
- Design refs: R2.3 + mockup
- Build notes: `ViewPostDetailFragmentNew` + `CommentsRecyclerViewAdapterNew`; markdown flattening is the risky half (rendering parity matrix required)
- Effort: XL
- Risk: high (UI-017 parity; thread behavior is core identity)
- Depends on: EPIC-03 (card language + tokens)
- Acceptance criteria: boundary obvious in 3 themes; depth-10 readable at 360dp; depth announced; jump/search/collapse-all work one-handed; RTL + 200% font pass
- Type: Big bet

### EPIC-05: Headers, search, inbox, compose
- Goal: content-first community pages; search that gets out of the way; safe composing; scannable inbox
- Scope: 88dp header contract + sticky Join/Follow + banner placeholders (subreddit/profile), search rebuild (one list, no marquee), inbox hierarchy + unread, compose labels + drafts + inline progress, settings grouping + two one-line fixes, drawer regroup + insets, onboarding primer
- Replaces/absorbs: UI-024 (insets slice), UI-025 (remainder), UI-026, UI-027 (remainder), UI-028, UI-029
- Design refs: R2.5–R2.8 + mockups, R1.2 drawer map
- Build notes: header layouts (+sw600dp/land twins), `activity_search.xml` `ConcatAdapter` rebuild, draft store (additive, 7-day retention), primer screen ahead of `LoginActivity` (no auth touch)
- Effort: L
- Risk: med (breadth, not depth; each screen independently shippable — split PRs per screen)
- Depends on: EPIC-01, EPIC-02
- Acceptance criteria: first paint shows content on community pages; search completes TalkBack-clean; drafts survive rotation + process death (spot); unread visible + announced; primer → all 3 auth paths work
- Type: Safe win (batched small wins)

### EPIC-06: Media contract + motion language
- Goal: one viewer behavior, continuity from feed, explanatory motion everywhere
- Scope: unified chrome contract (5 viewers), shared-element enter, predictive-back migration, motion/haptic tokens, download progress, viewer a11y
- Replaces/absorbs: UI-020, UI-021, UI-022, UI-023
- Design refs: R2.4 + mockup, R3.5
- Build notes: bar layouts → transitions → dispatcher migration; `postponeEnterTransition` + Glide/BigImageViewer coordination; vendored Slidr is the hard part (fork-or-replace decision inside the epic)
- Effort: L
- Risk: high (UI-021 Slidr + 5-viewer matrix)
- Depends on: EPIC-03 (stable row geometry), EPIC-04 (thread media parity)
- Acceptance criteria: thumb→viewer continuity on photo rows; predictive back previews on 34+; haptics system-respecting; progress inline; viewer TalkBack complete
- Type: Big bet

### EPIC-07: Platform finish
- Goal: modern-Android citizenship with no behavior change
- Scope: edge-to-edge leftovers, per-app language + RTL/font verification bundle, monochrome/splash spot-checks, notification polish, R8 + baseline profiles
- Replaces/absorbs: UI-024 (remainder), UI-031, UI-032
- Design refs: R3 platform notes, audit §4
- Build notes: insets pass, `localeConfig` + backport, Macrobenchmark profiles for feed/detail/comments/media, R8 keep-rules tuning
- Effort: M
- Risk: med (R8 on a big Java tree; profiles need a reference device in CI)
- Depends on: EPIC-03–EPIC-06 (profile after they land for clean numbers)
- Acceptance criteria: cold-start/jank numbers improve; no overlap on gesture/cutout emulators; picker works at minSdk 24
- Type: Safe win

### EPIC-08: Identity extras
- Goal: the differentiators — icons, per-sub accents, widgets/share polish
- Scope: launcher icon variants, opt-in per-sub accent (exporter-derived, default off), Glance widget (optional), Sharesheet preview/DirectShare
- Replaces/absorbs: UI-033 (now in scope per decision), UI-030 is NOT here (see below)
- Design refs: R0.3 color philosophy, R3.1 exporter
- Build notes: additive only; accent pipeline rides the EPIC-01 exporter; widget is a new module-sized surface — split it out if it grows
- Effort: M
- Risk: med (scope creep magnet; gate each sub-item separately)
- Depends on: EPIC-01
- Acceptance criteria: default experience pixel-identical with everything off; each extra independently toggleable; 3 themes + RTL + 200% font pass
- Type: Safe win (gated) — Big bet if the widget ships

### UI-### reconciliation (final)
- Absorbed: UI-001–007, UI-009–029, UI-031–032 (all land via EPIC-01–07; UI-004/008 already shipped, counted absorbed)
- Kept as specified: UI-030 two-pane (still maintainer-undecided — sits outside the epic order until decided; if approved it follows EPIC-04 as its own Big bet)
- Obsolete: none (card_2 work redirected, not deleted — deprecation tracked inside EPIC-03)
- New from redesign: rail inbox-badge parity (folded into EPIC-02), collapse-all (EPIC-04), draft autosave (EPIC-05), onboarding primer (EPIC-05), empty-state illustration system (EPIC-03/04/05 per screen)

### Rollback / feature-flag approach
- Token/theme work (EPIC-01) is global and unflaggable — safety comes from Roborazzi goldens + the exporter keeping user themes byte-compatible, plus shipping it first alone so regressions attribute cleanly.
- Structural changes ship behind `SharedPreferences` gates where cheap: meta-line style (EPIC-03), header compaction (EPIC-05), collapse-all default (EPIC-04), per-sub accents + widget (EPIC-08, default off). Pattern already exists in-app (feature prefs) — reuse it, no new flag framework.
- Big bets (EPIC-03/04/06) split into theme-first / behavior-second PRs; behavior halves ride a beta channel (`minifiedRelease`/beta track per `app/build.gradle`) for one release before default-on.
- Rollback for any epic = revert its PR stack (epics are ordered to keep stacks disjoint: tokens → rows → threads → headers → media → platform).

## Decisions needed from the maintainer
1. Confirm direction B (or pick A/C) — everything downstream branches from this.
2. `AGENTS.md` + namespace questions from the audit (§6 Q1–Q2) still open.
3. Two-pane (§6 Q6): R1 will assume single-pane-first with list/detail pane targets for tablets unless you rule it out.
