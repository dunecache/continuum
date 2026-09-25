# Subreddit audit (Pass 2.2)

## Current structure (verified)
- `activities/ViewSubredditDetailActivity.java` + `res/layout/activity_view_subreddit_detail.xml` (177 lines): `CoordinatorLayout` + `AppBarLayout` > `CollapsingToolbarLayout(scroll|enterAlways|enterAlwaysCollapsed, title disabled)` containing a `LinearLayout` (160dp `GifImageView` banner + header `ConstraintLayout`) + pinned `MaterialToolbar` + `TabLayout` (fixed/fill, indicator 3dp) + `ViewPager2` + `bottom_app_bar` include + `FloatingActionButton` (gone).
- Tabs (verified `TabLayoutMediator`, `:1287-1294`): **Posts** (reuses `PostFragment` feed) / **About** (`fragments/SidebarFragment.java` + `fragment_sidebar.xml`: `SwipeRefreshLayout` > markdown `RecyclerView`, padding 8/8/8/144, `clipToPadding false`). Double-tap tabs (scroll-to-top presumably) + `fixViewPager2Sensitivity`.
- Tab theming is dual-state (expanded vs collapsed toolbar colors/indicator/background, `:707-721`) — most theme-aware tab styling seen so far.
- FAB doubles as sort control (`floatingActionButton.setImageResource(ic_sort_toolbar_24dp)`, `:1052`) while declared `visibility gone` in XML.

## Visual hierarchy problems
- Banner (160dp full-bleed GIF, `centerCrop`) + 72dp icon + `font_18` name + subscribe `Chip` + counts row + description all live **inside the collapsing area** — opening a subreddit shows chrome, not posts; the header consumes ~300dp+ before the first row.
- Subscribe affordance is a borderless `Chip` (`chipStrokeColor #00000000` hardcoded transparent, `:67`) next to the name — low visual weight for the primary action; selected vs unselected state is code-driven (UNVERIFIED visually).
- Counts row splits subscriber/since 50/50 (`constraintWidth_percent 0.5` + `width_max wrap`) — balanced, but description below is full-width `match_parent` with no max width (wide-tablet line length UNVERIFIED).

## Density
- Header `padding 16dp` throughout; name `paddingTop 8dp`; counts `marginTop 16dp`. Comfortable. Banner fixed 160dp regardless of device (no adaptive height).

## Touch targets
- Subscribe `Chip` is `wrap_content × wrap_content` — likely under 48dp height; `TabLayout` tabs are system-height (fine). FAB hidden by default, so sort is reachable only via toolbar/menu until shown.

## States
- Posts tab inherits feed states (pull-to-refresh, Paging footer, error). About tab has its own `SwipeRefreshLayout`.
- Banner/icon load placeholders: `GifImageView` targets with no XML placeholder/error — loading/failure appearance UNVERIFIED (Glide targets in activity, not audited).

## RTL / large font
- `marginStart/End`, barriers (`barrier5/6`), 50/50 constraints mirror-safe. Name/counts unbounded horizontally (`wrap_content` + `width_max wrap`) — long counts wrap instead of clipping (acceptable). Banner fixed height is font-independent.

## Accessibility gaps
- Banner HAS `contentDescription` (`@string/content_description_banner_imageview`, `:32`) — good.
- Subreddit FAB declares **`android:contentDescription="@null"`** (`:175`) while doubling as the sort button — TalkBack gets nothing for a functional control.
- Subscribe chip label/state announcement is code-set (UNVERIFIED).

## Performance red flags
- Full-width **GIF banner** (`pl.droidsonroids.gif.GifImageView`, 160dp × match_parent, `centerCrop`) decoded on every entry — most expensive above-the-fold asset on this screen; no `thumbnail`/`placeholder` in XML.
- `CollapsingToolbarLayout` with `enterAlways|enterAlwaysCollapsed` + nested `ViewPager2` + `bottom_app_bar` — nested-scroll coordination cost; `fixViewPager2Sensitivity` suggests prior swipe conflicts.
- Sidebar markdown `RecyclerView` carries `paddingBottom 144dp` to clear FAB/bottom bar — works but is a magic number compensating for overlay chrome.
