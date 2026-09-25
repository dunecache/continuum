# Submit / compose audit (Pass 2.4)

## Current structure (verified)
- Six activities share one scaffold: `PostText/Link/Image/Video/Gallery/PollActivity` + `SubmitCrosspostActivity` + `CommentActivity` + `EditComment/EditPostActivity` + `SendPrivateMessageActivity`. Audited fully: `activity_post_text.xml` (240 lines), `activity_comment.xml` (head 80+), `activity_send_private_message.xml` (head ~70).
- `activity_post_text.xml`: AppBar + `NestedScrollView(weight 1)` (account row → subreddit `RelativeLayout` row → `MaterialDivider` → flair/spoiler/NSFW chips → reply-notifications toggle → title `EditText` → body `EditText`) + bottom markdown-bar `RecyclerView` (horizontal, `scrollbars horizontal`).
- Account row: 24dp icon + name (`marginStart 32dp` magic offset), full-width `selectableItemBackground`. Subreddit row: 24dp icon + `choose_a_subreddit` text + text `rules` button (`font_default`), legacy `RelativeLayout` (`alignParentStart/End`, `centerVertical`, `toStartOf/toEndOf`).
- Title: transparent bg (`#00000000`), `title_font_18` **bold**; body: transparent bg, `content_font_18`. Both `textCapSentences|textMultiLine`, `padding 16dp`, `gravity top`. Markdown bar: `MarkdownBottomBarRecyclerViewAdapter` bound via `bindEditTextWithItemClickListener` (`PostTextActivity.java:378-383`, `CommentActivity.java:353-379`, horizontal + `stackFromEnd`).
- `activity_comment.xml`: same + parent title (`title_font_16`, gone) + markdown preview `RecyclerView` + 1dp divider + account row.
- `activity_send_private_message.xml`: 3 stacked `EditText`s (user/subject/content, `content_font_18`, transparent bg, 16dp paddings) separated by bare 1dp `View` dividers; subject `maxLength 100`.
- Keyboard: compose activities use `adjustResize` (manifest `:201-550` block) so the markdown bar stays above the IME; detail/search use `adjustPan`. Send menu item survives rotation mid-send (`CommentActivity.java:541` comment + `:558-604` handling).

## Visual hierarchy problems
- Progressive disclosure is good (account → destination → flags → title → body → format). Title vs body differ only by bold + title family at the same 18 step — adequate but subtle.
- Flair/spoiler/NSFW chips each carry `layout_margin 16dp` (not padding) — selecting all three inserts 32dp+ gaps between tiny 4dp-padded chips.
- `MaterialDivider` IS used here (`divider_1/2_post_text_activity`) — the only screen so far using M3 dividers instead of bare 1dp Views (inconsistent with feed/comments bare dividers).

## Density
- Rows breathe (16/8dp). Body `EditText` is `match_parent` height inside the scroll — grows with content, good.

## Touch targets
- `rules` text button is `wrap_content` (small); notification toggle row is full-width clickable with a real `ThemedMaterialSwitch` (good). Account/subreddit rows full-width clickable (good) with 24dp icons.

## States
- HAVE: destination placeholder (`choose_a_subreddit`), per-field hints (`post_title_hint`, `post_text_content_hint`, `send_message_*_hint`), subject length cap, rotation-safe send, `adjustResize` bar.
- UNVERIFIED: draft autosave/restore; upload progress for image/video/gallery posts; submit error presentation; crosspost attribution UI.

## RTL / large font
- Account/subreddit rows use `marginStart 16/32dp` magic offsets + legacy `RelativeLayout` — mirrors, but fragile at large font (icon + text + rules button may collide; `toStartOf` chain holds only if widths cooperate — UNVERIFIED on device).
- Title/body unbounded height — large font safe. `nextFocusLeft/Right` self-loops on both fields (harmless).

## Accessibility gaps
- Fields have hints but **no visible labels** (`TextInputLayout` unused; transparent `EditText`s) — hint vanishes on input; no `labelFor`/`autofillHints` in XML.
- Account/subreddit pickers are clickable layouts with no role/label in XML (code UNVERIFIED). Markdown-bar items are icon-only (labels UNVERIFIED).

## Performance red flags
- None major: one scroll + one horizontal bar; comment compose nests a markdown preview `RecyclerView` (same nested pattern as comment rows, single instance — fine).
