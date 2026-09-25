# UI Overhaul Backlog

> Pass 0 complete. Read-only audit; no source modified. This doc + `docs/ui-audit/` are the only audit outputs.

## 0. Architecture & Inventory

### 0.1 Sources read (Pass 0)
- `README.md` — fork of Infinity for Reddit, Java Reddit client, custom Client ID, 6 main tabs
- `AGENTS.md` — **MISSING at repo root** (glob `AGENTS*` found nothing). Rule 5 ("respect AGENTS.md") is therefore UNVERIFIED; audit proceeded with rule 5 interpreted conservatively (no API/auth/network/DB proposals).
- `build.gradle` (root: AGP `9.3.0`, Kotlin `2.3.21`), `settings.gradle` (`:app`, `:lint-checks`), `gradle.properties`, `app/build.gradle` (full dependency + SDK block)
- `app/src/main/AndroidManifest.xml` (645 lines, ~60 `<activity>` entries)
- `app/src/main/res/layout/activity_main.xml`, `app_bar_main.xml`
- `app/src/main/res/values/styles.xml` (570 lines), `colors.xml`, `dimens.xml`, `attr.xml`
- `app/src/main/java/ml/docilealligator/infinityforreddit/activities/MainActivity.java` (nav/drawer/tabs grep), `activities/BaseActivity.java` (insets block lines ~250-300), `utils/MaterialYouUtils.java` (1-80), `Infinity.java` (MaterialYou refs)
- Directory listings: `activities/` (76 files), `fragments/` (26), `bottomsheetfragments/` (~38), `adapters/`, `res/layout/` (281 files), `res/xml/` (prefs), `customviews/compose/`

### 0.2 Build / platform baseline
- `compileSdk 37`, `minSdk 24`, `targetSdk 36` (`app/build.gradle:71-82`)
- `applicationId "org.cygnusx1.continuum"`, `namespace 'ml.docilealligator.infinityforreddit'` (package rename not done — UI code still lives under `ml.docilealligator.infinityforreddit`)
- `versionCode 230`, `versionName "8.3.1.8"`
- Java `VERSION_24` source/target + `coreLibraryDesugaringEnabled true` (`app/build.gradle:135-143`)
- `viewBinding = true`, `compose = true`, `resValues = true` (`app/build.gradle:176-183`); ViewBinding is the dominant inflation path (`setContentView(binding.getRoot())` in all sampled activities)
- AGP `9.3.0` + `kotlin-compose` plugin `2.3.21`, Compose BOM `2025.11.01`

### 0.3 Language mix (verified by `find`)
- `app/src/main/java`: **703 `.java` + 228 `.kt`** (~75% Java / 25% Kotlin by file count)
- Kotlin is concentrated in: newer activities (`ApiStatisticsActivity.kt`, `AppAuthLoginActivity.kt`, `CopyMultiRedditActivity.kt`, `FetchRandomSubredditActivity.kt`, `RandomSubredditOptionsActivity.kt`, `ReminderListingActivity.kt`, `SetReminderActivity.kt`), settings fragments (`AccountSettingsManagementPreferenceFragment.kt`, `CommentSwipeActionPreferenceFragment.kt`, etc.), `customtheme/`, post-swipe/gallery helpers (`PostCardPreviewStyle.kt`, `GalleryGifAutoplay.kt`, `CompactThumbnailPreloader.kt`)
- Java remains the feed/post/comments core (`MainActivity.java`, `PostFragment*.java`, `PostRecyclerViewAdapter.java`, `CommentsRecyclerViewAdapterNew.java`, `ViewPostDetailFragmentNew.java`)

### 0.4 UI toolkit
- **Views (dominant), not Compose.** 281 layout XMLs in `res/layout/` (+ `layout-land/`, `layout-sw600dp/` qualifiers, both present).
- **Compose is isolated:** 11 files contain `@Composable`; the library is `customviews/compose/` (`AppTheme.kt`, `CustomAlert.kt`, `CustomAppBar.kt`, `CustomButton.kt`, `CustomImage.kt`, `CustomLoadingIndicator.kt`, `CustomSwitch.kt`, `CustomText.kt`, `CustomTextField.kt`) consumed by a handful of Compose activities (`CopyMultiRedditActivity.kt`, `ReminderListingActivity.kt`, `SetReminderActivity.kt`, `AppAuthLoginActivity.kt`) with theme `AppTheme.ComposeActivity` (translucent, `AndroidManifest.xml:43-57`). Feed/post/comments/settings are Views.
- Material library: `com.google.android.material:material:1.14.0` (`app/build.gradle:426`); Compose M3 `material3:1.5.0-alpha01` + `adaptive:1.1.0` (forced via `resolutionStrategy`, `app/build.gradle:564-573`)
- AppCompat `1.7.1`, ConstraintLayout `2.2.1`, CardView `1.0.0`, SwipeRefreshLayout `1.2.0`, RecyclerView `1.4.0`, ViewPager2 `1.1.0`, Preference `1.2.1`, SplashScreen `core-splashscreen:1.0.1`
- No Jetpack Navigation component: **no nav graph XML** under `res/` (only `item_nav_drawer_*`, `nav_header_main`, `navigation_rail_menu`, `navigation_drawer_preferences.xml`), no `NavController`/`NavHostFragment` hits in `java/`. Navigation = manual Intents between ~60 manifest activities + `ViewPager2` tab pages + `DrawerLayout`.

### 0.5 Navigation approach (verified)
- `MainActivity` (`activities/MainActivity.java:160`): `CustomDrawerLayout` (`activity_main.xml`) > `app_bar_main.xml` (`CoordinatorLayout` + `AppBarLayout` + `CollapsingToolbarLayout` + `MaterialToolbar` + `TabLayout`) + `ViewPager2` (`view_pager_main_activity`) + `bottom_app_bar` include. Tabs bound via `TabLayoutMediator` (`MainActivity.java:245,615`). Drawer content is a `RecyclerView` inside `NavigationView` driven by `NavigationDrawerRecyclerViewMergedAdapter`.
- Bottom path: optional `BottomAppBar` (`BOTTOM_APP_BAR_KEY`, `MainActivity.java:353`) + `NavigationRail` (`R.id.navigation_rail`, `MainActivity.java:359`). Custom bottom-app-bar preview exists in `CustomThemePreviewActivity`.
- Swipe-back: vendored `customviews/slidr` + theme family `AppTheme.Slidable` / `SlidableWithActionBar` (translucent window, `styles.xml:32-42`) applied to most detail activities in the manifest (History, Subreddit, User, Post detail, Inbox, Search, etc.).
- Media dismiss: `app.futured.hauler:hauler:5.0.0` + `AppTheme.Draggable` (translucent black) for `ViewRedditGalleryActivity`, `ViewImgurMediaActivity`, `ViewVideoActivity`; opaque `AppTheme.Shadowbox` for `shadowbox/ShadowboxActivity` (`styles.xml:44-58`, `AndroidManifest.xml:301-587`).
- Bottom sheets are pervasive: ~38 classes in `bottomsheetfragments/` (post/comment/sort/filter/share/theme/speed/URL menus).
- Settings: `SettingsActivity` (`PreferenceActivityTheme`) hosting ~30 `PreferenceFragmentCompat`s + ~35 `res/xml/*_preferences.xml` files.

### 0.6 Theming system (verified, detail deferred to Pass 1)
- Root style `AppTheme` parent `Theme.Material3.DayNight` but immediately re-points to custom attrs (`styles.xml:4-10`); siblings: `NoActionBar`, `NoActionBarWithTransparentStatusBar`, `Launcher` (splash drawable), `ComposeActivity`, `Slidable`, `SlidableWithActionBar`, `Draggable`, `Shadowbox`, `PreferenceActivityTheme`, `MaterialAlertDialogTheme`.
- Custom theme engine, not M3 dynamic color by default: `customtheme/CustomTheme*.java/.kt` (Room entity `CustomTheme`, DAO, `CustomThemeWrapper`, `LocalCustomThemeRepository`, online repo/paging), `utils/CustomThemeSharedPreferencesUtils.java`, per-mode SharedPreferences (light/dark/amoled) + `MaterialYouUtils.java` which synthesizes "Material You / Material You Dark / Material You Amoled" themes from wallpaper colors (2s sleep + `RecreateActivityEvent`).
- `attr.xml`: 32 attrs — font scale (`font_*`, `title_font_*`, `content_font_*` dimensions), font families, and core surfaces (`colorPrimary`, `colorPrimaryDark`, `colorAccent`, `colorPrimaryLightTheme`, `primaryTextColor`, `secondaryTextColor`, `backgroundColor`, `cardViewBackgroundColor`).
- `colors.xml` (light defaults) is tiny (33 lines); `values-night/colors.xml` + `values-night-v27/v31/styles.xml`, `values-land-v28/styles.xml` carry mode overrides. `dimens.xml` is near-empty (8 entries: nav header, fab, fab_clearance, sheet padding, staggered offset, `post_compact_thumbnail_size` 112dp, `shadowbox_panel_icon_size` 24dp, `comment_score_min_width` 64dp) — most spacing lives in layouts (Pass 1 target).
- Edge-to-edge: manual in `BaseActivity.java:263+` — on SDK 35+ (`VANILLA_ICE_CREAM`) installs a decor-view insets listener painting `colorPrimary` behind status + a nav-bar scrim (`updateNavBarScrim`) when immersive mode is off; otherwise sets `navigationBarColor`/`statusBarColor` from the custom theme. `enableOnBackInvokedCallback="true"` in manifest (`AndroidManifest.xml:35`).
- RTL declared supported (`supportsRtl="true"`).

### 0.7 Image / video / markdown
- Images: Glide `5.0.7` (+ compiler, `glide-transformations:4.3.0`, `compose:1.0.0-beta09`), `aspect-ratio-imageview:1.0.9`, `android-gif-drawable:1.2.32`, APNG `glide-plugin:3.0.5`, `BigImageViewer:1.8.1` + `GlideImageLoader`, `zoomlayout:1.9.0`
- Video: Media3 `1.10.1` (`exoplayer`, `-dash`, `-hls`, `-ui`, `-smoothstreaming`, `-datasource-okhttp`)
- Markdown: Markwon `4.6.2` (core, strikethrough, linkify, recycler-table, simple-ext, inline-parser, image-glide) + `commonmark-ext-gfm-tables`, `better-link-movement-method:2.2.0`
- Motion: Lottie `6.7.1`; lists: `paging-runtime/guava:3.5.0`, `fastscroll:1.3.0`, `flow-layout:1.3.3`; Giphy `ui:2.3.18`; QR `zxing:4.3.0`

### 0.8 List implementation
- `RecyclerView` everywhere (feed, comments, inbox, drawer, galleries). Key adapters (verified by listing `adapters/`): `PostRecyclerViewAdapter.java` (feed), `PostDetailRecyclerViewAdapterNew.java`, `CommentsListingRecyclerViewAdapter.java` / `CommentsRecyclerViewAdapterNew.java` (+ `CommentsFooter/Status` `.kt`), `PostCardPreviewStyle.kt`, `CompactThumbnailPreloader.kt`, `MainPageTabsRecyclerViewAdapter.java`, gallery adapters. Paging 3 backs online-theme and listing data sources (`OnlineCustomThemePagingSource` seen; feed paging to confirm in Pass 2).

### 0.9 Screen inventory (Activity → layout, class drives it)
| # | User-visible screen | Activity class | Layout |
|---|---|---|---|
| 1 | Home / feed tabs | `activities/MainActivity.java` | `activity_main.xml` + `app_bar_main.xml` + `bottom_app_bar.xml`; pages `fragment_post.xml` via `fragments/PostFragment.java` / `PostFragmentBase.java` |
| 2 | Post detail + comments | `activities/ViewPostDetailActivity.java` | `activity_view_post_detail.xml`; body `fragments/ViewPostDetailFragmentNew.java`, rows `item_post_detail_*.xml`, comments `fragment_comments_listing.xml` |
| 3 | Subreddit | `activities/ViewSubredditDetailActivity.java` | `activity_view_subreddit_detail.xml` (+ `fragments/SidebarFragment.java` for sidebar) |
| 4 | User profile | `activities/ViewUserDetailActivity.java` | `activity_view_user_detail.xml` |
| 5 | Search entry | `activities/SearchActivity.java` | `activity_search.xml`; history `SearchHistoryActivity.java` / `activity_search_history.xml`; results `SearchResultActivity.java` / `activity_search_result.xml`, `SearchSubredditsResultActivity.java`, `SearchUsersResultActivity.java` |
| 6 | Inbox / messages | `activities/InboxActivity.java` | `activity_inbox.xml` + `fragments/InboxFragment.java`; compose `SendPrivateMessageActivity.java` / `ViewPrivateMessagesActivity.java` |
| 7 | Submit / compose | `PostTextActivity/PostLinkActivity/PostImageActivity/PostVideoActivity/PostGalleryActivity/PostPollActivity.java` + `SubmitCrosspostActivity.java` | `activity_post_text/link/image/video/gallery/poll.xml`, `activity_submit_crosspost.xml`; comment `CommentActivity.java` / `activity_comment.xml`, edit `EditCommentActivity/EditPostActivity.java` |
| 8 | Media viewer | `ViewImageOrGifActivity.java` / `activity_view_image_or_gif.xml`; `ViewVideoActivity.java` / `activity_view_video*.xml`; `ViewImgurMediaActivity.java` (+ `ViewImgurImage/VideoFragment.java`); `ViewRedditGalleryActivity.java` (+ `ViewRedditGallery*Fragment.java`); `shadowbox/ShadowboxActivity.java` / `activity_shadowbox.xml`; `WebViewActivity.java` |
| 9 | Settings | `activities/SettingsActivity.java` | `activity_settings.xml`; ~30 fragments in `settings/` + `res/xml/*_preferences.xml` |
| 10 | Nav drawer / bottom nav | part of `MainActivity` | `nav_header_main.xml`, `item_nav_drawer_*.xml`, `bottom_app_bar.xml`, `res/menu/navigation_rail_menu.xml` |
| 11 | Onboarding / login | `LoginActivity.java` / `activity_login.xml`; `LoginChromeCustomTabActivity.java`; `AppAuthLoginActivity.kt` (Compose) | as listed |
| 12 | Subscribed / multi-reddits | `SubscribedThingListingActivity.java`, `SelectedSubredditsAndUsersActivity.java`, `UserMultiRedditsActivity.java`, `Create/EditMultiRedditActivity.java`, `ViewMultiRedditDetailActivity.java` | `activity_subscribed_thing_listing.xml`, `activity_selected_subreddits.xml`, `activity_user_multi_reddits.xml`, etc. |
| 13 | History / visited / saved | `HistoryActivity.java`, `RecentlyVisitedActivity.java` (+ `HistoryPostFragment.java`), `AccountSavedThingActivity.java`, `AccountPostsActivity.java`, `FilteredPostsActivity.java` | `activity_history.xml`, `activity_recently_visited.xml`, `fragment_post_history.xml`, etc. |
| 14 | Theming | `CustomizeThemeActivity.java`, `CustomThemeListingActivity.java` (+ `CustomThemeListingFragment.java`), `CustomThemePreviewActivity.java` | `activity_customize_theme.xml`, `activity_custom_theme_listing.xml`, `activity_theme_preview.xml`, `fragment_theme_preview_posts/comments.xml` |
| 15 | Filters | `PostFilterPreferenceActivity.java`, `CustomizePostFilterActivity.kt`, `CommentFilterPreferenceActivity.java`, `CustomizeCommentFilterActivity.java`, `CommentFilterUsageListingActivity.java` | `activity_post_filter_preference.xml`, `activity_customize_post_filter.xml`, etc. |
| 16 | Misc utility | `RulesActivity.java`, `WikiActivity.java`, `ReportActivity.java`, `FullMarkdownActivity.java`, `SelectUserFlairActivity.java`, `FetchRandomSubredditActivity.kt`, `RandomSubredditOptionsActivity.kt`, `LockScreenActivity.java`, `SuicidePreventionActivity.java`, `QRCodeScannerActivity.java`, `ApiStatisticsActivity.kt`, `EditProfileActivity.java` | `activity_rules/wiki/report/...xml` respectively |
| 17 | Share / links | `ShareDataResolverActivity.java`, `LinkResolverActivity.java` (intent filters for reddit hosts + `SEND`) | no dedicated UI |

Post-card layout modes (verified file names, behavior UNVERIFIED until Pass 2): `item_post_compact*.xml` (2 variants + right-thumbnail), `item_post_card_2_*.xml` (compact-link, right-thumbnail, gallery, text, video-autoplay + legacy controller, with-preview), `item_post_card_3_*.xml` (same family), `item_post_detail_*.xml` (gallery, image/gif autoplay, link, no-preview, text, video/gif preview, video autoplay + legacy).

### 0.10 Pass 0 open questions / UNVERIFIED
- `AGENTS.md` absent — confirm whether repo policy lives elsewhere (`CONTRIBUTING`, `.github/`) before Pass 1 proposes file moves.
- Feed paging wiring (`paging-runtime` usage in `PostFragment` vs plain `RecyclerView` + endless scroll) — to verify in Pass 2 (Home/feed).
- Swipe-action implementation (colors `swipeAction*` + `post/comment_swipe_action_preferences.xml` exist) — gesture UX vs Apollo deferred to Pass 3 with code evidence.
- Compose coverage claim ("isolated") rests on `@Composable` grep (11 files); full call-graph UNVERIFIED.

## 1. Design System Findings

> Pass 1 complete. Read-only; counts via `rg` over `app/src/main/res/layout/` (281 files) plus files opened below.

### DS-1 Colors bypass M3 roles — severity HIGH (verified)
- What exists: 8 color attrs (`attr.xml:42-51`: `colorPrimary`, `colorPrimaryDark`, `colorAccent`, `colorPrimaryLightTheme`, `primaryTextColor`, `secondaryTextColor`, `backgroundColor`, `cardViewBackgroundColor`). `colors.xml` is 33 lines (light defaults `#0336FF/#002BF0/#FF1868`); `values-night/colors.xml` 12 lines (`#242424/#121212`, divider `#69666c` vs light `#E0E0E0`). Root `AppTheme` parents `Theme.Material3.DayNight` but re-points to custom attrs (`styles.xml:4-10`).
- The real palette is **87 int fields** on the Room entity `customtheme/CustomTheme.java:39-...` with **87 getters** in `customtheme/CustomThemeWrapper.java` (per-mode `SharedPreferences`: light/dark/amoled). Defaults are inline `Color.parseColor(...)` in every getter (e.g. `#0336FF/#242424/#000000` in `getColorPrimary()`).
- Layout adoption of attrs is good: **3014 `?attr/` refs vs 17 `@color/` refs**; only 17 `@color` hits total.
- But layouts still carry **164 hardcoded hex in 76 files**: 110× `#00000000` (transparent — mostly legit), 21× `#FFFFFF` (all in `exo_playback_control_view*.xml`, gallery viewer), 12× `#000000`, plus semantic colors `#F9A825/#E53935/#43A047/#444141/#40000000/#80000000/#CCFFFFFF/#FBEEFC` (poll/share/shadowbox). Exo/gallery whites ignore theme attrs.
- No M3 roles (`surfaceContainer`, `onSurfaceVariant`, `primaryContainer`…) anywhere; tonal surfaces UNVERIFIED-absent (zero `ShapeAppearance`/tonal hits, see DS-4/5).

### DS-2 Typography triple-scale works but isn't M3 — severity MED (verified)
- Three parallel scales × families: `FontStyle` / `TitleFontStyle` / `ContentFontStyle` (`attr.xml:5-40`) with 5–6 steps each (`styles.xml:159-294`: XSmall→XLarge + `ContentFontStyle.XXLarge`) and three family styleables (`FontFamily.*`, `TitleFontFamily.*`, `ContentFontFamily.*`, `styles.xml:296-492`, 15 fonts in `res/font/` incl. Atkinson Hyperlegible).
- `android:textSize` appears **981× in 229 layout files**, but **959 use `?attr/`** (`font_default` 581, `font_12` 222, `font_16` 46, `title_font_18` 40, `content_*` ~30). Only **~22 raw `sp`** remain: `16sp×10` (`activity_api_statistics.xml`, `fragment_settings_search.xml`, `item_settings_search.xml`), `14sp×4`, `36sp×3` + `20sp×2` + `24sp` (all in `shared_post*.xml` share sheets), `11sp/12sp/13sp` singles.
- `TextAppearance` is effectively unused: **2 files** (`view_table_entry_cell.xml`, `item_settings_search.xml`). No M3 type scale (`display/headline/title/label/body`).
- Bug (verified): `TitleFontStyle.Large` sets `<item name="font_default">16sp</item>` instead of `title_font_default` (`styles.xml:231`) — title scale step is silently wrong at Large.

### DS-3 Spacing has rhythm but zero tokens — severity HIGH (verified)
- `dimens.xml` holds **8 entries only** (`nav_header_vertical_spacing/height`, `fab_margin/clearance`, sheet padding, staggered offset, `post_compact_thumbnail_size` 112dp, `shadowbox_panel_icon_size` 24dp, `comment_score_min_width` 64dp).
- Layouts carry **2954 hardcoded `margin/padding` dp values**. Histogram by value clusters on a 4dp grid (good accident, not a system): 16dp×1369, 24dp×911, 8dp×709, 0dp×504, 32dp×480, 4dp×295, 48dp×162, 6dp×135, 12dp×103, 72dp×67, 20dp×57, 36dp×46, 1dp×38, 64dp×32, then long tail (150/144/96/56/42/40/30/10/3/2dp). No `space_4/8/12/16/24` token exists.

### DS-4 Shapes are ad-hoc — severity MED (verified)
- Zero `ShapeAppearance` / `MaterialShapeDrawable` / `shapeAppearance` hits in `java/`, `res/layout/`, `res/values/`.
- `cardCornerRadius` is inline per file (**33 files** set corner/elevation attrs): 12dp dominates (filter rows, `item_post_card_3_*`), 16dp (main post cards `item_post_*with_preview/gallery/text/video*`), 8dp (inner gallery `item_post_gallery*.xml`), 24dp (share sheets `shared_post*.xml`). No single radius scale; share-sheet 24dp vs card 12/16dp vs gallery-inner 8dp is undocumented.

### DS-5 Elevation uses shadows, not tonal surfaces — severity MED (verified)
- No tonal-surface roles (follows from DS-1). Depth = `CardView` default shadows + manual nav-bar scrim painted in `activities/BaseActivity.java:263+` (`updateNavBarScrim`) on API 35+; `AppTheme` hardcodes `android:navigationBarColor @android:color/black` (`styles.xml:8`).
- `values-night-v27/styles.xml` + `values-night-v31/styles.xml` only restyle the launcher/splash (`navigationBarColor #121212`, SplashScreen API black); no surface elevation overlays for dark/AMOLED.

### DS-6 Icons: one set, code-tinted — severity MED (partial)
- `res/drawable/` holds **243 files**, overwhelmingly `ic_*` 24dp vectors (prefix histogram fragments by noun: bookmark/arrow/add/wallpaper/upvote/share…). Only day/night variants found are theme-picker icons (`ic_*_theme_preference_day_night_24dp.xml` in `drawable/` + `drawable-night/`).
- Tint is programmatic, not thematic: `adapters/PostRecyclerViewAdapter.java` alone has **100 `setTextColor|setBackgroundColor|setCardBackgroundColor|setColorFilter` calls**; same pattern expected in comment/detail adapters (UNVERIFIED beyond feed). `autoMirrored` RTL coverage UNVERIFIED.

### DS-7 Duplication is structural — severity MED (verified)
- Font styles triplicated (`FontStyle`/`Title`/`Content` × XSmall…XLarge, `styles.xml:159-294`); `Theme.Normal/NormalDark/AmoledDark` triplicated (`styles.xml:496-531`); checkbox/switch dark overlays duplicated (`Widget.App.CheckBox.Dark` vs `Switch.Dark`, `styles.xml:135-151`).
- Post rows exist in **3 families** (`item_post_compact*.xml`, `item_post_card_2_*.xml`, `item_post_card_3_*.xml`, each × link/right-thumbnail/gallery/text/video-autoplay±legacy/with-preview) with per-file copies of the same 24dp icon / 8-16dp padding / `?attr/font_12` meta pattern. Bottom-sheet rows repeat the 32dp-icon + `font_default` text pattern (`fragment_post_options_bottom_sheet.xml`, `fragment_comment_more_bottom_sheet.xml`, `fragment_share_link_bottom_sheet.xml`…). `layout_weight` in **68 files** (nested LinearLayouts — perf follow-up in Pass 2).

### DS-8 Hardcoded strings are contained — severity LOW (verified)
- **36 literal `android:text|hint|contentDescription`** values (not `@string`): ~20 are font-preview names (`fragment_font_preview.xml`: "Balsamiq Sans", "Inter Bold"…), rest are preview/placeholder (`"2 Hours"×3`, `"1234"×2`, `"u/edgan"`, `"abcd.efg"`, single letters). Only 4 files use `tools:text`. No mass hardcoded-UI-text problem.

### DS-9 RTL foundations present, polish UNVERIFIED — severity LOW (verified/partial)
- `supportsRtl="true"` (manifest), `marginStart/End` adopted (**231 `marginEnd`, 205 `marginStart`**), no `marginLeft/Right` hits in the sampled grep. Drawable mirroring, `layout-land` deltas, and large-font clipping deferred to Pass 2 per-screen checks.

### DS-10 Touch targets likely under 48dp — severity MED (partial, needs Pass 2)
- Fixed icon boxes dominate: `24dp×24dp` (152 height + 151 width hits), `20dp` (28+28), `36dp` (15+15); explicit `48dp` appears only **8×** (5 height, 3 width). Whether `minHeight`/touch delegates compensate must be verified on post cards/comments/bottom sheets in Pass 2.

### DS-11 Custom-theme engine is the migration surface — severity HIGH (verified)
- Storage: `CustomTheme` Room entity (`customtheme/CustomTheme.java`, Gson import/export for online themes) + `CustomThemeDao` + `Local/OnlineCustomThemeRepository` + `CustomThemeSharedPreferencesUtils` (3 pref files). Runtime: `CustomThemeWrapper` (87 getters, per-`themeType` LIGHT/DARK/AMOLED pref switch + `Color.parseColor` defaults) injected into every activity/adapter; Compose bridge `customviews/compose/AppTheme.kt` collects the same Room flows (`currentLight/Dark/AmoledCustomThemeFlow`) with Indigo fallbacks (`getIndigo/Dark/Amoled`) — but wraps content in a bare `MaterialTheme {}` (`AppTheme.kt:111`), so **Compose M3 color scheme is never wired**.
- Material You today is a **snapshot, not dynamic color**: `utils/MaterialYouUtils.java:82-170` (API 31+) copies `system_accent*/neutral*` into "Material You / Dark / Amoled" `CustomTheme` rows + prefs, then posts `RecreateActivityEvent`; pre-S path (`:173+`) derives light/dark from `WallpaperManager.getWallpaperColors` lighten/darken. Gated on `Build.VERSION_CODES.S` / `O_MR1`; `minSdk 24` devices below O_MR1 get nothing (fallback UNVERIFIED).
- Migration must preserve (no-removal rule): **3 mode slots**, all 87 fields (incl. read-post title/content/card colors, `filledCard` variants, tab collapsed/expanded background+text+indicator sets, `navBarColor`, `isLightStatusBar` + toolbar-collapse flags), triple font scale + families, online-theme Gson schema, per-user Room rows, `MATERIAL_YOU_SENTRY_COLOR` re-apply flow (`Infinity.java:333,391-402`). Anything M3 must land as an exporter into these slots or a parallel opt-in theme, or existing user themes break.

## 2. Screen Audits (summary + links to docs/ui-audit/)
- Batch 1 (this pass): [home-feed](ui-audit/home-feed.md), [post-card](ui-audit/post-card.md).
- Feed: Paging 3 + DiffUtil + Toro autoplay + swipe actions + SwipeRefresh are HAVE; gaps are hierarchy (2-line meta, card_2 split meta flows), unbounded titles, 0 XML contentDescriptions, `animateLayoutChanges` on 14 row layouts, ~100 programmatic color calls + 79 Glide refs per bind path, no skeleton state.
- Post cards: 3 families (compact / card_2 / card_3) + right-thumbnail ID-copies; card_3 ships commented-out flair block with malformed XML + hardcoded `#FBEEFC` fill; no-preview fallback tints a system color (`@android:color/tab_indicator_text`); play affordance 36dp; same action-row touch-target question as feed.
- Batch 2 (this pass): [post-detail-comments](ui-audit/post-detail-comments.md), [subreddit](ui-audit/subreddit.md).
- Detail/comments: ViewPager2 multi-post + MovableFAB next/prev-top-level nav + in-comment search panel (labelled controls) are HAVE; gaps are weak post/comment boundary, 7-element comment header + 8-control toolbar crowding, nested markdown RecyclerView per comment, `animateLayoutChanges` on rows, depth guides purely visual, FAB with no contentDescription, unbounded titles/bodies.
- Subreddit: collapsing banner (160dp GIF) + 72dp icon + borderless subscribe chip + 50/50 counts + Posts/About ViewPager2; gaps are chrome-heavy first paint, `chipStrokeColor #00000000` hardcoded, FAB `@null` description while doubling as sort, GIF banner cost, 144dp sidebar bottom magic number.
- Batch 3 (this pass): [user-profile](ui-audit/user-profile.md), [search](ui-audit/search.md).
- User profile: subreddit scaffold + Posts/Comments tabs + save-bookmark (labelled, documented placeholder strategy — best small control in audit); gaps are same banner/chip/FAB-`@null` issues, 24dp save target, unbounded name vs end-pinned save overlap risk.
- Search: toolbar-embedded `font_20` query + 4 labelled utility icons + scope/history/delete-all `RelativeLayout` row + 2-col recent grid (marquee-forever cards) + autocomplete + tabbed results; gaps are crowded toolbar, legacy scope row, 12dp vs 16dp card radii, `marquee_forever` animation + TalkBack cost, nested `RecyclerView`s in `NestedScrollView`, result FAB `@null`, unlabelled history/delete-all icons.
- Batch 4 (this pass): [inbox](ui-audit/inbox.md), [submit-compose](ui-audit/submit-compose.md).
- Inbox: Notifications/Messages tabs + labelled compose FAB (best FAB in audit) + flat 4-text notification rows + 70%-capped chat bubbles; gaps are subject/title competition, code-only unread state, unlabelled copy icon/rows.
- Submit/compose: shared scaffold (account → destination → flags → title/body → markdown bar, `adjustResize`, rotation-safe send) across 6+ activities; only screen using M3 `MaterialDivider`; gaps are hint-only labels (no `TextInputLayout`), chip `margin 16dp` gaps, legacy `RelativeLayout` picker rows with magic 32dp offsets, small `rules` button, UNVERIFIED drafts/upload/error states.
- Batch 5 (this pass): [media-viewer](ui-audit/media-viewer.md), [settings](ui-audit/settings.md).
- Media viewer: 5 Hauler/Slidr viewers (drag-dismiss HAVE, pinch HAVE incl. ported video zoom, double-tap images HAVE) + labelled 5-action image bar (exemplar) + shadowbox pixel-stable panel; gaps are no shared-element transitions, hardcoded over-media whites (`#FFFFFF/#CCFFFFFF/#80000000`), shadowbox icon `@null`, `animateLayoutChanges` on PlayerView/AppBar, hand-rolled video touch pipeline.
- Settings: container + ~30 fragments + 35 pref XMLs + live search (48×48 labelled clear — only 48dp target found in XML); gaps are flat stock rows, copy-pasted `toolbarId` pointing at another screen's toolbar, raw `16sp` query size, UNVERIFIED custom-preference announcements.
- Batch 6 — Pass 2 complete: [navigation](ui-audit/navigation.md), [onboarding-login](ui-audit/onboarding-login.md).
- Navigation: merged-adapter drawer (176dp legacy-RelativeLayout header, 16dp rows — largest targets in app) + 48dp bottom bar (gone, runtime-labelled) + rail; documented rail TalkBack bug (announces "Option N" placeholder titles — fix sites named in `navigation_rail_menu.xml` comment); header avatar `marginTop 40dp` instead of insets; center FAB cutout gap.
- Onboarding/login: WebView OAuth + Custom-Tab + Compose AppAuth paths, offline retry, labelled alt-method FAB, Lottie lock screen; gaps are unowned WebView first-run, permanent 2FA line, Lottie a11y/loop cost. Pass 2 covers 12/12 screen groups.

## 3. Benchmark Gap Matrix (table)

> Pass 3 complete. Apollo is discontinued; ratings below use my own knowledge of it and hedge with UNSURE where memory is uncertain. Evidence paths are all from this repo.

| Area | Apollo benchmark (hedged) | Continuum status | Evidence |
|---|---|---|---|
| Swipe posts: vote/save/hide, configurable | Apollo had configurable swipe actions (UNSURE of level count) | **HAVE** (likely exceeds on configurability) | 3 levels × L/R in `xml/post_swipe_action_preferences.xml`; actions upvote/downvote/save/hide/mark-read(-and-hide)/unread/toggle/share/profile/comment/crosspost/reminder (`utils/SwipeActionPreferences.kt:36-46`, `colors.xml` swipe bands); painter `customviews/SwipeActionPainter.kt`, touch `PostFragmentBase.java:155-280` |
| Swipe comments: vote/save/reply | Apollo had comment swipes (UNSURE of set) | **HAVE** | Comment actions upvote/downvote/save/reply/share/share-as-image(±thread) (`SwipeActionPreferences.kt:52-59`), `xml/comment_swipe_action_preferences.xml` |
| Swipe back | Apollo: edge swipe back | **HAVE** | Vendored Slidr + `AppTheme.Slidable` family on most detail activities (`styles.xml:32-42`, manifest); vertical Slidr on image viewer (`ViewImageOrGifActivity.java:162`) |
| Pull-to-refresh feel | Apollo: tight branded refresh | **PARTIAL** — works, unbranded | Stock `SwipeRefreshLayout`, only theme tinting (`PostFragment.java:1980-1981`); no branded indicator, no skeleton |
| Post layouts compact/large/card | Apollo: compact/list/grid-ish variety (UNSURE of exact set) | **HAVE** (breadth) | 6 prefs × ~18 row layouts: compact(×4), card_2(×7), card_3(×5), gallery, detail(×8) (`PostRecyclerViewAdapter.java:497-560+`) |
| Media previews + thumbs | Apollo: fast inline previews | **PARTIAL** — present, heavy | `CompactThumbnailPreloader` + 112dp token (good); full-bleed cards un-capped (`match_parent + adjustViewBounds`), `animateLayoutChanges` on 14 rows, ~79 Glide refs in adapter |
| Meta-line design | Apollo: single quiet line (my memory — UNSURE) | **MISSING** (vs that bar) | 2-line stacked meta + 60% guideline + split flows (`item_post_compact.xml:26-92`, card_2 double FlowLayout) — Pass 2 hierarchy notes |
| Depth guides | Apollo: readable rainbow-ish guides (UNSURE of colors) | **HAVE** | `CommentIndentationView`: 2dp lines, 12dp spacing, 7 colors, single-divider option |
| Collapse behavior | Apollo: tap-to-collapse threads | **HAVE** | Header tap + `FULLY_COLLAPSE_COMMENT` pref + pixel-stable collapsed row (`CommentsRecyclerViewAdapterNew.java:366-443`, issues #219/#385) |
| Jump to next top-level | Apollo: jump button (my memory — UNSURE) | **HAVE** | MovableFAB tap/long-press + menu + volume keys (`ViewPostDetailActivity.java:322-326,488-501,879-882,991-994`) |
| Sticky/quick nav | Apollo: jump bar concept (UNSURE) | **PARTIAL** | FAB + in-comment search panel (labelled) HAVE; no sticky comment-nav bar, no "X of Y" position readout |
| Readable indentation (deep) | Apollo stayed readable deep (UNSURE of cap) | **PARTIAL** | Guides + compaction (`CommentToolbar.requiredWidth`, 64dp score token) HAVE; width grows per level, no verified cap; nested markdown RecyclerView per comment |
| Type scale | Apollo: tight iOS scale (UNSURE) | **PARTIAL** | Triple `?attr` scale respected in 959/981 `textSize` uses; no M3 scale; `TitleFontStyle.Large` bug (`styles.xml:231`); `TextAppearance` in 2 files |
| Font choices | Apollo: system fonts | **HAVE** (exceeds on choice) | 15 bundled families × 3 roles (`res/font/`, `styles.xml:296-492`), Compose mirror (`AppTheme.kt:143-164`) |
| Spacing rhythm | Apollo: consistent rhythm | **MISSING** | 8-entry `dimens.xml` vs 2954 hardcoded margins; 4dp clustering accidental (DS-3) |
| Dividers | Apollo: hairlines | **PARTIAL** | Bare 1dp Views (feed/comments, code-colored) vs M3 `MaterialDivider` only in compose screens — standardize pending |
| Subtle animation | Apollo: springy micro-motion | **MISSING** | `animateLayoutChanges` misused on rows; no shared-element/container-transform (only `overridePendingTransition` slide on video dismiss `:484`); Toro autoplay present |
| Media gestures | Apollo: pinch/double-tap/dismiss | **HAVE** | Pinch + double-tap images (`ViewImageOrGifActivity.java:301-302`), ported video pinch/pan (`ViewVideoActivity.java:1091-1235`), Hauler dismiss + Slidr vertical |
| Media transitions | Apollo: zoom-from-thumbnail (UNSURE) | **MISSING** | No shared-element enter; viewers open full-bleed without continuity |
| Media controls | Apollo: minimal chrome | **PARTIAL** | Labelled 5-action image bar (exemplar) + custom exo controls; hardcoded over-media colors; no download progress in bar |
| Haptics | Apollo: rich haptics (UNSURE) | **PARTIAL** | Swipe-action haptic on level-arm (`SwipeActionPainter.kt:45-49`, `VIRTUAL_KEY` + `FLAG_IGNORE_GLOBAL_SETTING`, user-toggleable) — but force-ignores global setting; no other haptic surfaces found |
| List→detail motion | Apollo: smooth push | **MISSING** | Activity-per-screen Intents, no transition framework; `Slidable` translucency is for swipe-back, not enter |
| Themes | Apollo: themes + icon options (UNSURE of depth) | **HAVE** (depth likely exceeds) | 87-field Room themes × 3 modes + online share + Material-You snapshot (`MaterialYouUtils.java`) |
| Per-subreddit accents | Apollo: UNSURE (do not assert) | **MISSING** | No per-sub accent/theme wiring found (only favorite-sub ordering + filters) |
| Layout options | Apollo: per-feed view modes | **HAVE** | 6 post layouts + autoplay/NSFW/spoiler routing + columns prefs (`number_of_columns_in_post_feed_preferences.xml`) |
| Icon choices | Apollo: custom icons (my memory — UNSURE) | **MISSING** | Single adaptive launcher (`mipmap-anydpi-v26/ic_launcher.xml`); no alternates found |
| Multi-account | Apollo: fast switcher | **HAVE** | Drawer account sections + switcher + management (`navigationdrawer/Account*.java`, `InboxActivity` account switch `:268`) |
| Inbox UX | Apollo: clean messages | **PARTIAL** | Tabs + labelled compose FAB + 70% bubbles HAVE; flat 4-text rows, code-only unread, unlabelled copy |
| Search UX | Apollo: fast scoped search | **PARTIAL** | Scoped search + history grid + autocomplete + tabbed results HAVE; crowded toolbar, marquee cards, nested lists, result FAB `@null` |

### Pass 3 takeaways
- Confident leads (keep): swipe configurability (3 levels), comment collapse + FAB jump nav + in-comment search, font/theme breadth, media gesture coverage, multi-account.
- Highest-leverage gaps: single quiet meta line; spacing/shape tokens; shared-element + micro-motion language; skeleton states; per-subreddit accents and icon choices (only if maintainer wants parity — both are additive, no-removal-safe); haptic language beyond swipes; unread/empty-state design.

## 4. Android Platform Opportunities

> Pass 4 complete. Costed against `minSdk 24 / targetSdk 36 / compileSdk 37` (Pass 0). All androidx backports below work at 24 unless noted.

- **Dynamic color → exporter, not replacement.** `Material 1.14.0` is in place and `MaterialYouUtils` already snapshots `system_accent*/neutral*` (API 31+) with a wallpaper lighten/darken fallback (API 27+) into the 87-field engine. Adopting `DynamicColors.applyToActivitiesIfAvailable()` directly would fight the custom slots; the cheap path is an M3-scheme→`CustomTheme` exporter (light/dark/amoled) + keep snapshot for <31. Cost **M**; fallback mandatory (minSdk 24 covers pre-S majority of the supported range).
- **M3 components: re-theme, don't re-widget.** Cards/buttons/chips/tabs/bottom-bar/text-fields/dividers(some)/switches/indicators are already M3 classes — the gap is roles (no `surfaceContainer`, no `TextAppearance` scale, no `ShapeAppearance`). Token pass (M1) unlocks tonal surfaces with zero widget swaps. Cost **S–M**.
- **Edge-to-edge: finish, don't start.** Manual insets + nav scrim on API 35+ (`BaseActivity.java:263+`) already handle Android 15 forced edge-to-edge; remaining debt is local: drawer header `marginTop 40dp` instead of insets, sidebar `paddingBottom 144dp` magic, bottom bar `paddingBottomSystemWindowInsets=false`, `fitsSystemWindows` mixed. Cost **S**.
- **Predictive back: Slidr is the risk.** Manifest opts in (`enableOnBackInvokedCallback`) and `MainActivity` uses `OnBackPressedCallback`; but swipe-back is vendored Slidr + Hauler drags, which don't participate in the predictive-back animation — they need `OnBackPressedDispatcher` + `BackHandler` migration or they will feel broken on 34+. Cost **M**.
- **Shared-element / container transform: greenfield.** No transition framework usage (one `overridePendingTransition` slide). Feed→detail image continuity needs transition names + `postponeEnterTransition` cooperating with Glide/BigImageViewer — worth it for media, the app's hero content. Cost **M** (media only; skip for text rows).
- **Themed icon + splash: verify, likely done.** Adaptive icon **already ships `monochrome`** (`mipmap-anydpi-v26/ic_launcher.xml`); splash uses `Theme.SplashScreen` on v31+ with drawable fallback. Both need light/dark/spot checks, not implementation. Cost **S**.
- **Large screen: one pane + qualifiers today.** `layout-sw600dp/` (6 files) + `isTablet` bool + column-count logic exist, but no two-pane list/detail and the Compose `adaptive:1.1.0` dependency sits unused by Views. Two-pane detail (feed+comments, subreddit+post) is the real L-tablet/foldable win. Cost **M–L**.
- **Per-app language: missing, cheap.** 20+ `values-*` string locales ship with no `localeConfig` / `setApplicationLocales` found. AppCompat backport covers minSdk 24. Cost **S–M**.
- **RTL + dynamic font: foundations, device verification left.** Start/end + barriers + `supportsRtl` + `?attr` sp scale are in; Pass 2 lists the per-screen checks (FlowLayout direction, marquee, magic offsets). Cost **S**.
- **Haptics: respect the setting.** Swipe haptic uses `VIRTUAL_KEY` + `FLAG_IGNORE_GLOBAL_SETTING` (`SwipeActionPainter.kt:45-49`) — deliberately overrides the system toggle. Migrate to system-respecting constants (`CLOCK_TICK`/`CONFIRM`, API 30+/34 with fallback) and extend the language to vote/collapse/refresh. Cost **S**.
- **Notifications: polish, not plumbing.** Channels (`NotificationUtils`), pull worker (`PullNotificationWorker`), and `POST_NOTIFICATIONS` exist; download/submit run as `BIND_JOB_SERVICE` services. Remaining: exact-vs-inexact scheduling audit, download progress style. Cost **S**.
- **Widgets + share sheet: optional additive.** No app widgets found (Glance front-page widget would be new surface, **M–L**); share is a custom bottom sheet — adding Sharesheet preview/DirectShare targets is **S–M**. Neither blocks the core overhaul.
- **Performance: profile, don't rewrite lists.** Diffing is HAVE (Paging `ItemCallback` + comment `ListAdapter`); thumb sizing is good (112dp token + preloader), full-bleed media is not; `release` ships `minifyEnabled false` (only `minifiedRelease` shrinks) and **no baseline profiles** exist — R8 + Macrobenchmark profiles are the cheapest cold-start/jank win. Cost **M**.

## 5. Prioritized Backlog (by milestone)

> Pass 5 synthesis. Ordered by impact/effort/risk (foundation first). All items preserve existing settings/customization (no-removal rule) and avoid API/auth/network/DB changes.

### M1 — Foundation / tokens (do first; everything else depends on these)

### UI-001: Spacing tokens
- Area: system
- Current state: 8-entry `res/values/dimens.xml` vs 2954 hardcoded margin/padding dps in 281 layouts (DS-3)
- Problem: no shared scale; density drifts per screen
- Proposal: add `space_4/8/12/16/24` (+ `space_2/32`) tokens, migrate layouts file-by-file starting with post rows and bottom sheets
- Benchmark: Apollo spacing rhythm (UNSURE of values) / n/a
- Android feature: n/a (enables M3 tonal/scale work)
- Effort: M
- Risk: low (mechanical; snapshot/golden tests exist via Roborazzi)
- Depends on: none
- Acceptance criteria: no new hardcoded margins in touched layouts; light/dark/AMOLED identical spacing; RTL-safe (start/end only); 200% font without overlap
- Confidence: verified

### UI-002: Shape scale
- Area: system
- Current state: zero `ShapeAppearance` hits; inline `cardCornerRadius` 8/12/16/24dp across 33 files (DS-4)
- Problem: cards/sheets/gallery disagree on radius
- Proposal: 3 shape appearances (small 8, medium 12, large 16/28 for sheets) + apply to post cards, filter rows, search cards, sheets
- Benchmark: n/a
- Android feature: M3 shapes
- Effort: S
- Risk: low
- Depends on: UI-001
- Acceptance criteria: one radius per component class; verified light/dark/AMOLED, RTL, large font
- Confidence: verified

### UI-003: M3 color-roles exporter (keep 87-field engine)
- Area: system / theming
- Current state: 8 color attrs + 87-field `CustomTheme`/`CustomThemeWrapper` with `Color.parseColor` defaults; Material You is a snapshot (`MaterialYouUtils.java`) (DS-1, DS-11)
- Problem: M3 roles/tonal surfaces unavailable; hardcoded hex in exo/gallery/share layouts
- Proposal: build M3-scheme→`CustomTheme` exporter (light/dark/amoled incl. read-post/filled/tab/navBar fields + `MATERIAL_YOU_SENTRY_COLOR` flow); keep Room/Gson schema unchanged; replace layout hex with attrs
- Benchmark: n/a
- Android feature: Material You dynamic color (API 31+, wallpaper fallback preserved for 27+, static fallback <27)
- Effort: M
- Risk: med (theme regressions across 3 modes; needs golden coverage)
- Depends on: none (parallelizable with UI-001)
- Acceptance criteria: user themes untouched; exporter output passes contrast AA in light/dark/AMOLED; RTL/large-font unaffected
- Confidence: verified

### UI-004: Type scale on TextAppearance + fix Large bug [TITLE-BUG FIXED — pending CI build]
- Area: system
- Current state: triple `?attr` scale (959/981 uses) but no `TextAppearance`; `TitleFontStyle.Large` writes `font_default` not `title_font_default` (`styles.xml:231`); 22 raw `sp` left (DS-2)
- Problem: no M3 type roles; title step silently wrong at Large
- Proposal: fix the one-line bug; add `TextAppearance.Continuum.{Title,Body,Meta,Caption}` mapping to the attr scale; migrate rows; clear the 22 raw `sp`
- Benchmark: n/a
- Android feature: M3 type scale
- Effort: S
- Risk: low
- Depends on: none
- Acceptance criteria: Large title size correct; rows use TextAppearance; 200% font verified on cards/comments; light/dark/AMOLED unaffected
- Confidence: verified

### UI-005: Standardize dividers on MaterialDivider
- Area: system
- Current state: bare 1dp Views (feed/comments, code-colored `mDividerColor`) vs M3 `MaterialDivider` only in compose screens
- Problem: two divider systems, preview-invisible colors
- Proposal: adopt `MaterialDivider` everywhere with theme color; delete code coloring
- Benchmark: Apollo hairlines (UNSURE)
- Android feature: M3 dividers
- Effort: S
- Risk: low
- Depends on: UI-003
- Acceptance criteria: dividers visible in preview + all 3 themes; RTL/large-font n/a
- Confidence: verified

### UI-006: Theme-driven icon tint (remove per-bind coloring)
- Area: system
- Current state: ~100 programmatic color calls in `PostRecyclerViewAdapter` alone; 24dp untinted vectors (DS-6)
- Problem: bind-time cost + theme drift; day/night variants only for picker icons
- Proposal: tint via theme attrs/styles; keep code tint only for vote-state; audit `autoMirrored`
- Benchmark: n/a
- Android feature: n/a
- Effort: M
- Risk: med (vote/state colors must stay exact)
- Depends on: UI-003
- Acceptance criteria: identical pixels in light/dark/AMOLED; RTL-mirrored arrows verified; bind сокращ (fewer calls)
- Confidence: partial (comment/detail adapters UNVERIFIED)

### UI-007: 48dp touch-target floor
- Area: system
- Current state: 24dp icon boxes dominate (152+151 hits); explicit 48dp only 8× (DS-10); rows affected: vote/save/share, search icons, save-bookmark 24dp, copy 32dp, rules button
- Problem: below platform minimum on icon-only actions
- Proposal: audit + enforce 48dp (padding/minHeight/delegates) on all icon buttons, starting with post/comment action rows
- Benchmark: n/a
- Android feature: accessibility
- Effort: M
- Risk: low (may add row height; needs design sign-off per row)
- Depends on: UI-001
- Acceptance criteria: every icon action ≥48dp on device; light/dark/AMOLED + RTL + large font verified
- Confidence: partial (on-device measurement pending)

### UI-008: Rail TalkBack quick win [QUICK WIN] [IMPLEMENTED — pending CI build]
- Area: navigation
- Current state: rail announces placeholder "Option N" (documented in `menu/navigation_rail_menu.xml`); bottom-bar XML `@null` rescued at runtime (`MainActivity.java:1438`)
- Problem: most severe documented a11y bug in audit; tiny fix
- Proposal: set real titles/descriptions at the two named sites (`NavigationWrapper.setOtherActivitiesContentDescription`, `setBottomAppBarContentDescription`); add regression test
- Benchmark: n/a
- Android feature: accessibility
- Effort: S
- Risk: low
- Depends on: none
- Acceptance criteria: TalkBack announces bound action per slot; light/dark/AMOLED + RTL n/a; large font n/a
- Confidence: verified

### M2 — Feed + post cards

### UI-009: Single quiet meta line
- Area: post cards / feed
- Current state: 2-line stacked subreddit+user + 60% guideline + split FlowLayouts (`item_post_compact.xml:26-92`, card_2 double flow)
- Problem: tallest, noisiest element; inconsistent across compact/card_2/card_3
- Proposal: one meta line (subreddit • user • time) in `font_12`/secondary across all modes; guideline/barrier cleanup
- Benchmark: Apollo quiet meta (UNSURE of exact form)
- Android feature: n/a
- Effort: M
- Risk: med (user habit; needs pref-compatible rollout)
- Depends on: UI-001, UI-004
- Acceptance criteria: meta fits one line at 200% font (ellipsize); RTL mirrors; 3 themes legible (AA)
- Confidence: verified

### UI-010: Bound titles and content
- Area: post cards
- Current state: titles (`title_font_18`) unbounded in all modes; content capped at 4 lines
- Problem: long titles push actions arbitrarily far
- Proposal: cap titles (e.g. 3–4 lines + ellipsize, per-mode tuned) and keep content cap
- Benchmark: n/a
- Android feature: n/a
- Effort: S
- Risk: low
- Depends on: UI-004
- Acceptance criteria: 10-line title stays bounded; RTL + large font verified; 3 themes fine
- Confidence: verified

### UI-011: Card_3 fill + dead-code + fallback tint cleanup [IMPLEMENTED — pending CI build; goldens need re-record]
- Area: post cards
- Current state: `cardBackgroundColor="#FBEEFC"` hardcoded (`item_post_card_3_with_preview.xml:12`); commented-out flair block with malformed XML (`:179-269`); no-preview fallback tints `@android:color/tab_indicator_text` (`item_post_compact.xml:167`)
- Problem: dark/AMOLED risk; dead code ships; fallback ignores custom themes
- Proposal: theme the fill; delete or restore the flair block (restore preferred — flair parity); retint fallback to theme attr
- Benchmark: n/a
- Android feature: n/a
- Effort: S
- Risk: low
- Depends on: UI-003
- Acceptance criteria: card_3 correct in light/dark/AMOLED; flair shows where feed had it; fallback follows theme; RTL/large-font fine
- Confidence: verified

### UI-012: Feed loading + empty states
- Area: home/feed
- Current state: spinner + full-screen error only; no skeleton; empty = `PagingData.empty()` with UNVERIFIED visuals
- Problem: pop-in + blank-list doubt
- Proposal: skeleton rows for first load + designed empty (illustration/copy/CTA) for no-subscriptions/no-results; keep error strings
- Benchmark: Apollo smooth loads (UNSURE)
- Android feature: n/a
- Effort: M
- Risk: low
- Depends on: UI-001, UI-002
- Acceptance criteria: skeleton→content with no layout jump; empty states in 3 themes; RTL + large font verified
- Confidence: partial (empty visuals UNVERIFIED)

### UI-013: Row jank (transitions + bind cost + uncapped media)
- Area: feed / cards
- Current state: `animateLayoutChanges` on 14 row layouts; ~100 color + 79 Glide calls per bind path; full-bleed media un-capped (`match_parent + adjustViewBounds`)
- Problem: bind/recycle cost + layout jumps on image load
- Proposal: remove row `animateLayoutChanges`; move static theming to styles; cap/reserve media aspect (ratio placeholder) keeping 112dp thumb token + preloader
- Benchmark: Apollo fast previews (UNSURE of technique)
- Android feature: n/a (enables shared-element later)
- Effort: M
- Risk: med (adapter is 7457 lines; behavior must not change)
- Depends on: UI-003, UI-006
- Acceptance criteria: no bind-time visual change; scroll jank down on profiled trace; 3 themes + RTL + large font unchanged
- Confidence: verified

### UI-014: Feed/card accessibility pass
- Area: feed / cards
- Current state: 0 XML `contentDescription` in all sampled rows; play overlay 36dp; `font_10` badges
- Problem: TalkBack gaps + small affordances
- Proposal: label all icon buttons (vote/save/share/comment/status) + play overlay ≥48dp + badge contrast AA
- Benchmark: n/a
- Android feature: accessibility
- Effort: S
- Risk: low
- Depends on: UI-007
- Acceptance criteria: TalkBack traverses a row sensibly; targets ≥48dp; AA in 3 themes; RTL + large font verified
- Confidence: verified (runtime assignment UNVERIFIED — confirm or set in XML)

### M3 — Comments + detail

### UI-015: Post/comment boundary
- Area: post detail
- Current state: detail header shares card styling on flat `cardViewBackgroundColor` — weak separation from first comment
- Problem: users lose the post→thread transition
- Proposal: distinct detail header treatment (surface + spacing + sticky action affordance), reusing M1 tokens
- Benchmark: n/a
- Android feature: n/a
- Effort: S
- Risk: low
- Depends on: UI-001, UI-002, UI-003
- Acceptance criteria: boundary obvious in 3 themes; RTL + large font verified
- Confidence: verified

### UI-016: Comment density (keep the compaction engine)
- Area: comments
- Current state: 7-element header + 8-control toolbar; `CommentToolbar.requiredWidth()` + 64dp score token already compact well
- Problem: crowded at 360dp despite good engineering
- Proposal: trim default visible toolbar set + collapse header meta (edited/score/badge rules) with the existing compaction, not against it
- Benchmark: Apollo readable threads (UNSURE)
- Android feature: n/a
- Effort: M
- Risk: med (power-user muscle memory)
- Depends on: UI-009 (meta language)
- Acceptance criteria: default row fits 360dp at Normal font; 200% font wraps sanely; RTL + 3 themes verified
- Confidence: verified

### UI-017: Markdown nesting performance
- Area: comments
- Current state: nested `RecyclerView` per comment (`nestedScrollingEnabled=false`) + root `animateLayoutChanges`
- Problem: N×M layout passes in long threads
- Proposal: flatten single-block bodies to TextView/Markwon span; reserve RecyclerView for tables/complex; drop row `animateLayoutChanges`
- Benchmark: n/a
- Android feature: n/a
- Effort: L
- Risk: high (rendering parity across markdown features)
- Depends on: UI-013
- Acceptance criteria: thread scroll profile improves with pixel-identical output; 3 themes + RTL + large font verified
- Confidence: partial (Markwon table behavior UNVERIFIED)

### UI-018: Depth readability + semantics
- Area: comments
- Current state: 2dp/12dp 7-color guides, width grows per level, purely visual, no cap verified
- Problem: deep threads squeeze text; TalkBack gets no depth
- Proposal: cap effective indent (guides continue, text column stops shrinking) + expose depth/collapse state semantically
- Benchmark: Apollo deep-thread readability (UNSURE)
- Android feature: accessibility
- Effort: M
- Risk: med
- Depends on: UI-016
- Acceptance criteria: depth-10 thread readable at 360dp; depth announced; 3 themes + RTL + large font verified
- Confidence: partial (cap behavior UNVERIFIED)

### UI-019: FAB labeling (keep search panel)
- Area: post detail
- Current state: MovableFAB nav (tap/long-press/menu/volume) has no contentDescription; in-comment search panel is the best-labelled control set in audit
- Problem: flagship nav is silent to TalkBack
- Proposal: label FAB (state-aware: next/previous) + keep search panel as pattern; no behavior change
- Benchmark: n/a
- Android feature: accessibility
- Effort: S
- Risk: low
- Depends on: none
- Acceptance criteria: TalkBack announces FAB purpose/state; search controls unchanged; 3 themes + RTL + large font n/a
- Confidence: verified

### M4 — Media + gestures + motion

### UI-020: Shared-element feed→viewer transitions
- Area: media viewer
- Current state: full-bleed open with no continuity (only slide on video dismiss); gestures HAVE (pinch/double-tap/Hauler/Slidr)
- Problem: jarring enter/exit for the hero content
- Proposal: shared-element image transitions (names + `postponeEnterTransition` with Glide/BigImageViewer) for image/gallery/imgur; keep Hauler/Slidr dismiss
- Benchmark: Apollo zoom-from-thumbnail (UNSURE)
- Android feature: activity transitions / container transform
- Effort: M
- Risk: med (viewer zoo: 5 activities)
- Depends on: UI-013 (stable row geometry first)
- Acceptance criteria: enter/exit continuity on photo rows; text rows unaffected; 3 themes + RTL + large font n/a
- Confidence: verified

### UI-021: Predictive-back migration
- Area: system / media
- Current state: opted in (`enableOnBackInvokedCallback`) but Slidr + Hauler bypass the predictive animation
- Problem: broken-feeling back on Android 34+
- Proposal: migrate Slidr/Hauler dismiss to `OnBackPressedDispatcher`/`BackHandler` with predictive animations; keep thresholds
- Benchmark: n/a
- Android feature: predictive back
- Effort: M
- Risk: high (vendored Slidr fork; gesture conflicts)
- Depends on: none
- Acceptance criteria: back preview works on 34+; pre-34 behavior unchanged; RTL/large-font n/a
- Confidence: verified

### UI-022: System-respecting haptics
- Area: system
- Current state: swipe haptic forces `VIRTUAL_KEY + FLAG_IGNORE_GLOBAL_SETTING` (user-toggleable but overrides system)
- Problem: ignores global haptics setting; no language elsewhere
- Proposal: respect system (`CLOCK_TICK`/`CONFIRM` w/ fallback) + extend to vote/collapse/refresh subtly, all toggleable
- Benchmark: Apollo rich haptics (UNSURE)
- Android feature: haptics API
- Effort: S
- Risk: low
- Depends on: none
- Acceptance criteria: system-off means off; subtle elsewhere; 3 themes + RTL + large font n/a
- Confidence: verified

### UI-023: Media controls polish
- Area: media viewer
- Current state: exemplar 5-action bar but no download progress; hardcoded over-media whites; shadowbox icon `@null`; `animateLayoutChanges` on PlayerView/AppBar
- Problem: fire-and-forget downloads; unthemed chrome; layout churn in playback
- Proposal: inline download/share progress; theme over-media chrome (or document as intentional); label shadowbox icon + video controls; drop viewer `animateLayoutChanges`
- Benchmark: Apollo minimal chrome (UNSURE)
- Android feature: n/a
- Effort: M
- Risk: med
- Depends on: UI-003
- Acceptance criteria: progress visible; controls labelled; playback unaffected; RTL + large font verified
- Confidence: verified

### M5 — Platform + remaining screens

### UI-024: Edge-to-edge finish
- Area: system
- Current state: 35+ scrim path exists; debt: drawer `marginTop 40dp`, sidebar `paddingBottom 144dp`, bottom bar `paddingBottomSystemWindowInsets=false`, mixed `fitsSystemWindows`
- Problem: overlap/gap bugs on tall-status/gesture devices
- Proposal: insets everywhere (header, sidebar, bars); delete magic numbers
- Benchmark: n/a
- Android feature: edge-to-edge / WindowInsets
- Effort: S
- Risk: low
- Depends on: UI-001
- Acceptance criteria: no overlap on gesture-nav + cutout emulators; 3 themes + RTL + large font verified
- Confidence: verified

### UI-025: Search clarity
- Area: search
- Current state: 4-icon toolbar + legacy scope row (magic 32dp) + `marquee_forever` cards + nested RecyclerViews + result FAB `@null` + unlabelled history/delete icons
- Problem: crowded, animating, nested, partially silent
- Proposal: declutter toolbar (move random/link out); modernize scope row; static truncation (drop marquee); single list (drop nesting); label all icons + FAB
- Benchmark: Apollo scoped search (UNSURE)
- Android feature: accessibility
- Effort: M
- Risk: med (heavily used entry point)
- Depends on: UI-001, UI-002, UI-007
- Acceptance criteria: no marquee; ≥48dp icons; TalkBack complete; 3 themes + RTL + 200% font verified
- Confidence: verified

### UI-026: Compose field labels
- Area: submit/compose + message compose
- Current state: hint-only transparent `EditText`s (no `TextInputLayout`); legacy picker rows with magic offsets; chip `margin 16dp` gaps; small `rules` button
- Problem: hints vanish; fragile rows; chip gaps
- Proposal: visible labels (`TextInputLayout` or persistent headers); fix chip margins; modernize picker rows; 48dp rules affordance; keep `adjustResize` + rotation-safe send + `MaterialDivider` pattern
- Benchmark: n/a
- Android feature: accessibility
- Effort: M
- Risk: med (6 activities share the scaffold)
- Depends on: UI-001, UI-007
- Acceptance criteria: labels persist during input; no overlap at 200% font; RTL mirrors; 3 themes verified
- Confidence: verified

### UI-027: Profile/subreddit header polish
- Area: subreddit / user
- Current state: 300dp+ collapsing chrome; borderless `#00000000` chips; subreddit FAB `@null`-as-sort; user FAB `@null`; 24dp save target; GIF banners with UNVERIFIED placeholders
- Problem: slow first paint; weak primary action; silent FABs
- Proposal: compress header (smaller banner, sticky subscribe/follow); bordered chips; label FABs; 48dp save; banner placeholders; keep save-bookmark pattern + 50/50 counts + dual tab theming
- Benchmark: n/a
- Android feature: n/a
- Effort: M
- Risk: med (high-visibility headers)
- Depends on: UI-002, UI-007
- Acceptance criteria: first paint shows content sooner (profiled); actions labelled; 3 themes + RTL + large font verified
- Confidence: verified

### UI-028: Inbox rows + unread design
- Area: inbox
- Current state: flat 4-text rows (subject vs title compete); unread is code-only; copy icon unlabelled
- Problem: scanability + silent state
- Proposal: hierarchy pass (subject/title/meta roles) + visible + announced unread + labelled copy; keep tabs/bubbles/compose FAB
- Benchmark: n/a
- Android feature: n/a
- Effort: S
- Risk: low
- Depends on: UI-004
- Acceptance criteria: unread obvious + announced in 3 themes; RTL + large font verified
- Confidence: partial (unread visuals UNVERIFIED)

### UI-029: Settings correctness
- Area: settings
- Current state: `toolbarId` points at another screen's toolbar; query is raw `16sp`; custom prefs' announcements UNVERIFIED
- Problem: copy-paste rot in the least-changed screen
- Proposal: fix `toolbarId`; scale query with `?attr`; verify/label custom prefs; keep 48dp clear + search + stock rows
- Benchmark: n/a
- Android feature: accessibility
- Effort: S
- Risk: low
- Depends on: UI-004
- Acceptance criteria: toolbar collapses correctly; query scales; custom prefs announce; 3 themes + RTL verified
- Confidence: verified

### UI-030: Two-pane large-screen
- Area: system
- Current state: `sw600dp` qualifiers + `isTablet` + column logic; no two-pane; `adaptive:1.1.0` unused
- Problem: tablets/foldables get a stretched phone
- Proposal: list/detail two-pane (feed+comments, subreddit+post) via adaptive APIs; keep single-pane under 600dp
- Benchmark: n/a
- Android feature: adaptive layouts / foldables
- Effort: L
- Risk: high (navigation is Activity-per-screen; needs fragmentization)
- Depends on: UI-015 (detail boundary)
- Acceptance criteria: two-pane on sw600dp emulator + foldable posture; single-pane unchanged; RTL + large font verified
- Confidence: partial (nav rework scope UNVERIFIED)

### UI-031: Per-app language + verification bundle
- Area: system
- Current state: 20+ locales, no `localeConfig`; RTL foundations but per-screen checks pending; themed icon + splash likely done but unverified
- Problem: users can't pick language per app; RTL/large-font/icon/splash unverified
- Proposal: add `localeConfig` + AppCompat locales; run the Pass-2 RTL/large-font checklist per screen; spot-check monochrome icon + splash in light/dark
- Benchmark: n/a
- Android feature: per-app language, RTL, dynamic font
- Effort: S
- Risk: low
- Depends on: none
- Acceptance criteria: picker works at minSdk 24 (backport); RTL + 200% font pass per screen; icon/splash screenshots attached
- Confidence: partial (backport behavior UNVERIFIED on device)

### UI-032: Cold start + jank profile
- Area: system
- Current state: `release` has `minifyEnabled false`; no baseline profiles; prefs read per activity
- Problem: slowest/cheapest wins untouched
- Proposal: enable R8 for release (keep `minifiedRelease` parity) + add Macrobenchmark baseline profiles for feed/detail/comments/media paths
- Benchmark: n/a
- Android feature: baseline profiles, R8
- Effort: M
- Risk: med (R8 keep-rules on a big Java codebase)
- Depends on: UI-013, UI-017 (profile after they land for clean numbers)
- Acceptance criteria: startup/jank numbers improve on a reference device; no behavior change; 3 themes n/a
- Confidence: verified (absence verified; gains UNVERIFIED until profiled)

### UI-033: Icon choices + per-subreddit accents [OPTIONAL]
- Area: customization
- Current state: single adaptive icon (monochrome included); no per-sub theming (favorites/ordering only)
- Problem: only if maintainer wants Apollo parity here — explicitly additive
- Proposal: ONLY with approval: launcher icon variants + opt-in per-sub accent (exported into the 87-field engine, default off)
- Benchmark: Apollo custom icons (UNSURE); per-sub accents UNSURE — do not assert parity
- Android feature: adaptive icons
- Effort: M
- Risk: med (scope creep; theme-engine surface)
- Depends on: UI-003
- Acceptance criteria: default experience unchanged; opt-in only; 3 themes + RTL + large font verified
- Confidence: UNVERIFIED (maintainer call)

### Dependency chain (risky items bold)
M1 (UI-001→002→003→004→005→006→007; UI-008 free) → M2 (UI-009→010, UI-011, UI-012, **UI-013**, UI-014) → M3 (UI-015, UI-016, **UI-017**, UI-018, UI-019) → M4 (**UI-020**, **UI-021**, UI-022, UI-023) → M5 (rest; **UI-030** last). Pull-forwards if wanted: UI-008 (S, isolated), UI-004 bug line (1-line), UI-011 (S).

## 6. Open Questions for the maintainer
1. Where is the active contributor policy (`AGENTS.md` missing)? May the audit propose touching `lint-checks/` or build files for token enforcement? — OPEN
2. Is the `ml.docilealligator.infinityforreddit` namespace freeze intentional for the fork, or may UI refactors rename it? — OPEN
3. Which post-card modes are user-facing supported vs legacy? — DECIDED: **card_3 + compact only**; card_2 and `*_legacy_controller` layouts are deprecation targets. (UI-009/UI-011/UI-013 now read "card_3 + compact"; card_2 work is removal, not redesign.)
4. Pull-forwards UI-008 + UI-004 bug line + UI-011 approved as first PRs? — **APPROVED all three**, implementation pending explicit lift of the read-only rule.
5. Is UI-033 (icon variants + per-subreddit accents) in scope? — **IN SCOPE, both** (no longer optional in practice; stays last in M5).
6. Two-pane (UI-030) implies fragmentizing Activity navigation — acceptable long-term? — **UNDECIDED**, left as L milestone.
