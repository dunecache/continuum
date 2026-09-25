# Settings audit (Pass 2.5)

## Current structure (verified)
- `activities/SettingsActivity.java` + `activity_settings.xml` (38 lines): `CoordinatorLayout` + collapsing toolbar + `FragmentContainerView`. Hosts ~30 `PreferenceFragmentCompat`s via `OnPreferenceStartFragmentCallback` (`:59-261`, title switch per fragment type).
- Preference XMLs: **35 files** in `res/xml/` (`main`, `theme`, `font`, `interface`, `post`, `comment`, `post_details`, `gestures_and_buttons`, `post_swipe_action`, `comment_swipe_action`, `navigation_drawer`, `notification`, `video`, `data_saving`, `download_location`, `backup_and_restore`, `api_keys`, `proxy`, `security`, `about`, `credits`, `debug`, …). Theme root `PreferenceActivityTheme` (`styles.xml:106-116`) with custom title/subtitle text styles + dialog button styles.
- Search: `fragment_settings_search.xml` (own screen over the preference tree): transparent `EditText` (`16sp` raw, `settings_search_hint`) + **48×48dp clear button** (`padding 12dp`, labelled — the only 48dp touch target found in XML this whole audit) + `?android:attr/listDivider` + results `RecyclerView`.

## Visual hierarchy problems
- 35 XML files × stock `Preference` rows = consistent but flat: no section cards, no icons in sampled XML (icon usage UNVERIFIED — most rows are title + summary + widget). Discoverability rests entirely on settings search.
- `toolbarId` in the collapsing layout points at `@id/toolbar_post_text_activity` (`activity_settings.xml:18`) — a copy-paste ID reference to another screen's toolbar; works (resolves to nothing local, falls back) but signals unowned layout.

## Density
- Stock preference row heights (system default, ~56-72dp) — comfortable. Search header `padding 16/4/8/8` is the tightest header in the audit.

## Touch targets
- Clear button 48×48 — exemplar (propagate pattern). Preference rows are full-width system targets (fine). Custom `SliderPreference`/`CustomFontPreference` attrs exist (`attr.xml:53-62`); row-level target sizes UNVERIFIED.

## States
- HAVE: settings search with live results list; `no_*` empty strings per list screen pattern. Backup/restore + API-keys screens exist as first-class destinations (fork differentiators).
- MISSING: no preview affordance inline except font/theme screens (font preview exists per Pass 1 file list; theme preview is its own activity).

## RTL / large font
- `paddingStart/End` in search header; preferences are framework-mirrored. Custom font scale (`font_*` triple scale) applies to settings rows via `PreferenceTitleTextStyle/Subtitle` (`font_16/font_default`) — settings respect the same scale as content (good). `16sp` raw query size ignores the scale (minor).

## Accessibility gaps
- Framework preferences announce title/summary/switch state (inherited, good). Clear button labelled (good). Custom preferences (`Slider`, `CustomFont`) need manual `contentDescription`/state announcement — UNVERIFIED.

## Performance red flags
- None structural: one fragment at a time in a container; search filters a `RecyclerView`. Preference count (~hundreds of keys across 35 files) inflates `SharedPreferences` reads on cold start — same 87-field theme wrapper + dozens of UI prefs read per activity (pre-existing, not settings-specific).
