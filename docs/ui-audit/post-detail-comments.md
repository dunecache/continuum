# Post detail + comments audit (Pass 2.2)

## Current structure (verified)
- `activities/ViewPostDetailActivity.java` + `res/layout/activity_view_post_detail.xml` (127 lines): `CoordinatorLayout` + `AppBarLayout/CollapsingToolbarLayout(scroll|enterAlways, title disabled)` + `ViewPager2` (multi-post swipe) + `MovableFloatingActionButton` (bottom|end, `fab_margin`) + bottom comment-search panel (`MaterialCardView`, `cardElevation 16dp`, gone) with `TextInputLayout.OutlinedBox` + prev/next/close `ImageView`s.
- Body: `fragments/ViewPostDetailFragmentNew.java` (2207 lines) + `adapters/CommentsRecyclerViewAdapterNew.java` (1856) + `adapters/CommentsListingRecyclerViewAdapter.java` (1017). Detail headers: 8 `item_post_detail_*.xml` variants (text audited head-80: same 2-line meta header as cards on `?attr/cardViewBackgroundColor` full-bleed).
- Comment row `res/layout/item_comment.xml` (313 lines): `CommentIndentationView` + `LinearLayout(animateLayoutChanges)` > header `ConstraintLayout` (icon/author/flair/edited/child-badge/score/time + 2 Barriers) + markdown `RecyclerView` (`nestedScrollingEnabled=false`, margins 8/8) + custom `CommentToolbar` (vote/score/downvote/child-badge/placeholder/expand/save/reply/more) + 1dp divider (gone).
- Collapsed row `item_comment_fully_collapsed.xml`: `CommentIndentationView` + `CollapsedCommentHeader` (icon/name/child-badge/score/time) + divider; code comments document pixel-stable badge/score positions across collapse (issues #219/#385).
- Placeholders: `item_no_comment_placeholder.xml` (36dp margins, `@string/no_comments_yet`), `item_load_comments.xml`, `item_load_more_comments_placeholder.xml`, `item_comment_footer_loading/error.xml`, `item_view_all_comments.xml`.

## Visual hierarchy problems
- Detail header repeats the feed's 2-line meta + title + badges + actions at full width with no elevation change from comments — post vs first comment boundary is weak (background is flat `cardViewBackgroundColor`).
- Comment header packs **7 elements** (icon, author, flair ≤2 lines, edited, child badge, score, time) into one ConstraintLayout with 2 barriers — informative but noisy; score/time/edited/badge compete on the right edge.
- `CommentToolbar` holds **8 controls**; compaction via `CommentToolbar.requiredWidth()` + `comment_score_min_width` (per `dimens.xml` comment) is good engineering, but the default visible set (vote/score/down/reply/save/more) is crowded at 360dp.

## Density
- Comment vertical rhythm is tight (`marginTop 0dp` on body/markdown, header `paddingStart/End 16dp` only) — good for threads. Collapsed row adds `paddingBottom 12dp` + `paddingEnd 16dp` on time — slightly airier, intentional.

## Touch targets
- Same 24dp-icon `MaterialButton` pattern as feed (`minWidth 0dp`, 8dp side padding, transparent tint). Comment toolbar adds a `TextView` expand affordance (`paddingStart/End 8dp`, `actionBarItemBackground`, gone by default) — small.
- Search panel prev/next/close are `wrap_content + padding 8dp` ImageViews — ~40dp effective, below 48dp.
- `MovableFloatingActionButton` is the exception: full FAB size, movable with resettable coordinates (`action_reset_fab_position`, `:875-876`) — good.

## Navigation / behavior (positives with evidence)
- HAVE jump-to-next/previous-top-level-comment: FAB tap → `scrollToNextParentComment()`, long-press → `scrollToPreviousParentComment()` (`ViewPostDetailActivity.java:322-326,488-501`); also menu items (`:879-882`) and volume-key nav (`:991-994`).
- HAVE in-comment search: bottom panel with hint `@string/search_comments`, `maxLines 1`, prev/next/close — the only screen with labelled search-nav controls.
- Collapse: header tap toggles; `FULLY_COLLAPSE_COMMENT` pref switches to the fully-collapsed row; child-count badges top + inline; `CommentToolbar` hides on click per prefs (`COMMENT_TOOLBAR_HIDDEN/HIDE_ON_CLICK`, adapter `:366-374`).

## States
- HAVE: fragment error (`error_image` + `load_post_error[_with_reason]`, `:1820-1829`), themed `SwipeRefresh` (`:2123`), comment loading/footer-error/no-comment placeholders.
- MISSING: no skeleton for post body or comments; markdown body pops in after parse.

## RTL / large font
- Start/end + barriers/guideline mirror-safe (same pattern as cards). Collapsed row uses `layout_weight 1` name + `gravity end` time — safe.
- Header `maxLines 1/2 + ellipsize` holds; markdown body scaling via Markwon + `content_font_*` — table overflow at large font UNVERIFIED.

## Accessibility gaps
- Search prev/next/close HAVE `contentDescription` (`@string/content_description_*`, layout `:97-121`) — best-labelled controls seen so far.
- Comment vote/reply/save/more icon buttons have **no XML `contentDescription`** (same gap as feed; runtime assignment UNVERIFIED).
- `MovableFloatingActionButton` has **no `contentDescription` found** in activity or custom view (grep empty) — TalkBack user gets no "next top-level comment" announcement.
- Depth guides are purely visual (`CommentIndentationView.onDraw` lines) — no semantic depth exposed; collapsed state announcement UNVERIFIED.

## Performance red flags
- **Nested `RecyclerView` per comment** for markdown (`comment_markdown_view_item_post_comment`, scrolling disabled) — N rows × M markdown blocks, each with its own layout pass; plus `animateLayoutChanges="true"` on the comment root.
- Adapter rebind complexity: holder-reuse clearing for flair colors/badges on every collapse/expand/load-more (adapter comments `:472-525`) — correct but indicates heavy bind.
- Indentation: `CommentIndentationView` draws 2dp lines at 12dp spacing (`pathWidth*6`), up to 7 colors; width grows per level (`startXs`) so **deep threads squeeze the text column** — cap/compact behavior UNVERIFIED.
