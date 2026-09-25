# Home / Feed audit (Pass 2.1)

## Current structure (verified)
- `activities/MainActivity.java:160` + `res/layout/activity_main.xml`: `CustomDrawerLayout` > `app_bar_main.xml` (`CoordinatorLayout` + `AppBarLayout` + `CollapsingToolbarLayout` + `MaterialToolbar` + `TabLayout`) + `ViewPager2` + `bottom_app_bar` include. Tabs via `TabLayoutMediator` (`MainActivity.java:245,615`); drawer = `RecyclerView` in `NavigationView` (`NavigationDrawerRecyclerViewMergedAdapter`).
- Page: `fragments/PostFragment.java` (2322 lines) + `PostFragmentBase.java` (1463) + `res/layout/fragment_post.xml` (47 lines): `SwipeRefreshLayout` > `CustomToroContainer` (RecyclerView subclass for autoplay) + centered `fetch_post_info_linear_layout` (gone by default; 48dp top/bottom + 36dp horizontal margins, 150dp image + `?attr/font_default` text).
- Data: Paging 3 (`mAdapter.submitData`, `PagingData.empty()` on no-subscriptions, `withLoadStateFooter(Paging3LoadingStateAdapter ... load_more_posts_error)` — `PostFragment.java:1238-1366`). Adapter `adapters/PostRecyclerViewAdapter.java` (7457 lines) has `DiffUtil.ItemCallback<Post>` (`:203`) and per-type `getItemViewType/viewTypeFor` (`:497-560+`).
- Gestures in feed: swipe actions exist (`PostFragmentBase.java:155-280`: `AdjustableTouchSlopItemTouchHelper` + `SwipeActionPainter`, levels/threshold prefs, vibrate option). Pull-to-refresh: `SwipeRefreshLayout`, progress color from `mCustomThemeWrapper.getCircularProgressBarBackground()` (`PostFragment.java:1980`).

## Visual hierarchy problems
- Collapsing toolbar + tabs both `scroll|enterAlways` (`app_bar_main.xml`): tab row disappears on scroll — cheap screen space, but section context is lost while scrolling.
- Compact header stacks subreddit + user on **two lines** (`item_post_compact.xml:26-63`, packed vertical chain) with time pinned right of a 60%-guideline (`:87-92`) — two-line meta + title + badge flow + action row makes each row tall for "compact".
- Card_2 splits meta into **two FlowLayouts** (badges/time `:80-184`, then author `:186-215`) between title and content — scanning author requires jumping two flows.

## Density
- Compact: 112dp thumb (`@dimen/post_compact_thumbnail_size`), 8dp vertical paddings, 16dp horizontal. Reasonable.
- Card_2: outer `paddingTop 16` + card side margins 16 + `title marginTop 16` + flow `padding 16` — airy; outer LinearLayout has no bottom padding (divider carries a stray `paddingBottom 8dp` on a 1dp View, which does nothing).
- Card_3: filled card `marginTop/Bottom 8` + `marginStart/End 16`, radius 12dp.

## Touch targets
- Action row (all modes): `MaterialButton` vote/up-down `wrap_content × match_parent`, comment/save/share `wrap_content × wrap_content`, 24dp icons, 8dp side padding, `minWidth 0dp`, `backgroundTint #00000000`, `strokeWidth 0dp`. No `minHeight`/`TouchDelegate` in XML or adapter (grep empty). Row height is constraint-driven (likely ~48dp) but icon-only save/share have no guaranteed 48dp — **verify on device**.
- Score column fixed `64dp` (`comment_score_min_width` token) — stable, good.

## States
- HAVE: pull-to-refresh; Paging footer loading/error (`load_more_posts_error`); full-screen error (`showErrorView`, `PostFragment.java:1862-1874`: `error_image` drawable + message; distinct 403-anonymous vs reason vs generic strings).
- PARTIAL: empty/no-subscriptions submits `PagingData.empty()` (`:1239`) — whether a designed empty illustration/copy shows vs blank list is UNVERIFIED visually.
- MISSING: no skeleton/shimmer placeholders; loading = spinner only.

## RTL / large font
- `marginStart/End`, `paddingStart/End` used throughout sampled layouts; time `gravity="end"`; 60% guideline mirrors automatically. `FlowLayout` row direction under RTL UNVERIFIED.
- Font scale respected via `?attr/` sizes; meta `maxLines 1 + ellipsize end`; content `maxLines 4 + ellipsize end`. Titles (`title_font_18`) have **no maxLines** in any sampled card — long titles push actions arbitrarily far down.

## Accessibility gaps
- **Zero `android:contentDescription` in `fragment_post.xml`, `item_post_compact.xml`, `item_post_card_2_with_preview.xml`, `item_post_card_3_with_preview.xml`** (grep count 0). Icon-only vote/save/share/comment buttons have no XML description — runtime assignment in adapter UNVERIFIED.
- Badge chips (`libRG.CustomTextView`, `font_10` ≈ 10sp at Normal) are small; color-contrast vs theme UNVERIFIED.
- Divider is a bare 1dp `View` with no XML color (color applied in code: `divider.setBackgroundColor(mDividerColor)`, adapter `:6283`) — fine at runtime, invisible in preview.

## Performance red flags
- `PostRecyclerViewAdapter` 7457 lines; `onBindViewHolder` at `:828` with **79 Glide/`into` refs** and **~100 programmatic color calls** per bind path — heavy bind, theme application per row instead of via styles.
- `android:animateLayoutChanges="true"` on **14 post layouts** (all compact/card variants) — layout transitions animate on every bind/recycle.
- Nesting (compact): `LinearLayout > ConstraintLayout(header) + ConstraintLayout(body: RelativeLayout > FrameLayout > Image + play + indicator + LoadingIndicator) + ConstraintLayout(actions) + divider` — 5–6 deep with `RelativeLayout` + `FrameLayout` + `Barrier` + `Guideline` + `FlowLayout` per row.
- Positives: `DiffUtil` + Paging 3 (no `notifyDataSetChanged` in sampled paths), `CompactThumbnailPreloader` sizes previews to the 112dp token.
