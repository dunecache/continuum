# User profile audit (Pass 2.3)

## Current structure (verified)
- `activities/ViewUserDetailActivity.java` + `res/layout/activity_view_user_detail.xml` (204 lines): same scaffold as subreddit — `CoordinatorLayout` + collapsing header (160dp `GifImageView` banner + 72dp icon + `font_18` name + follow `Chip` + 24dp save-bookmark + karma/cakeday 50/50 + gone description + barriers) + pinned `MaterialToolbar` + `TabLayout` + `ViewPager2` + `bottom_app_bar` + `FAB(@null)`.
- Tabs: **Posts / Comments** (`TabLayoutMediator`, `:919-925`) vs subreddit's Posts/About. Follow state set in code at 6 sites (`setText(R.string.follow/unfollow)`, `:596-726`); name/karma/cakeday/description colors applied in code (`:843-849`).
- Save-user bookmark (`:86-97`) is the best-documented small control in the audit: XML comment explains the default `ic_bookmark_border` src (avoids empty-box flash before Room emits), `selectableItemBackgroundBorderless`, `contentDescription="@string/save_user"`, centered on the chip so no layout shift between loading/followable states.

## Visual hierarchy problems
- Same chrome-heavy first paint as subreddit: ~300dp+ of banner + header before content. Profile adds a second chip-line action (follow + save) without strengthening the follow chip (`chipStrokeColor #00000000` hardcoded, same as subreddit `:68`).
- Karma + cakeday share one 50/50 row (`width_percent 0.5`, `width_max wrap`) — fine — but description is full-width `match_parent` with no max width (tablet line length UNVERIFIED).

## Density / touch targets
- Header `padding 16dp`; banner fixed 160dp. Follow `Chip` is `wrap_content` (likely <48dp); save bookmark is a bare 24dp `ImageView` with borderless ripple but no padding — ~24dp target, smallest labelled control in the profile.
- FAB `contentDescription="@null"` (`:202`) while visible (unlike subreddit's gone FAB) — functional button with explicitly nulled description.

## States
- Posts/Comments tabs reuse feed/comment-list states. Follow chip starts `invisible` (reserves space, `:67`) — no layout jump, good. Own-profile hides save (code). Banner/icon placeholders UNVERIFIED (same as subreddit).

## RTL / large font
- Start/end + barriers mirror-safe; `gravity end` cakeday. Name `font_18` unbounded (`wrap_content`, no maxLines) — long usernames push the save icon (end-constrained to parent, so it holds, but name + chip may collide; chip is constrained to icon, not to save — overlap UNVERIFIED at large font).

## Accessibility gaps
- Banner HAS description (shared string) — good. Save bookmark HAS description — good (only screen besides comment-search with a labelled icon action).
- FAB `@null` — gap. Follow chip announcement (follow vs unfollow) is code-set text on a `Chip` — likely announced, UNVERIFIED.

## Performance red flags
- Same full-width GIF banner cost as subreddit (`GifImageView`, 160dp, `centerCrop`). Two-tab `ViewPager2` (posts + comments) each with own list — heavier than subreddit's posts + static about.
