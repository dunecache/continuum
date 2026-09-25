# Post card (all layout modes) audit (Pass 2.1)

## Mode inventory (verified file names)
- Compact: `item_post_compact.xml` (408 lines, audited fully), `item_post_compact_right_thumbnail.xml`, `item_post_compact_2.xml`, `item_post_compact_2_right_thumbnail.xml` — right-thumbnail variants are ID-renamed copies (`diff` shows only `..._right_thumbnail` IDs + bias lines differ).
- Card_2: `item_post_card_2_compact_link.xml` (+ `_right_thumbnail`), `..._gallery_type.xml`, `..._text.xml`, `..._video_autoplay.xml` (+ `_legacy_controller`), `..._with_preview.xml` (335 lines, audited fully).
- Card_3: `..._gallery_type.xml`, `..._text.xml`, `..._video_type_autoplay.xml` (+ `_legacy_controller`), `..._with_preview.xml` (372 lines, audited fully).
- Routing: `PostRecyclerViewAdapter.viewTypeFor()` branches on `POST_LAYOUT_CARD/CARD_2/CARD_3/GALLERY/COMPACT/COMPACT_2` × post type (video/gif/image/gallery/link/no-preview/text) × autoplay × NSFW/spoiler (`PostRecyclerViewAdapter.java:497-560+`).

## Structure per mode
- Compact (`item_post_compact.xml`): header `ConstraintLayout` (24dp icon + 2-line subreddit/user + stickied icon + time) → body `ConstraintLayout` (112dp thumb `RelativeLayout` OR no-preview link `FrameLayout`, title `title_font_18`, badge `FlowLayout`, link `font_12`, `Barrier`) → action `ConstraintLayout` (up/score/down/comments/save/share) → 1dp divider.
- Card_2 (`item_post_card_2_with_preview.xml`): outer `LinearLayout` (`paddingTop 16`, `selectableItemBackground`) > elevated `MaterialCardView` (margins 16/16, `cardElevation 2dp`, `cardCornerRadius 8dp`, `materialCardViewElevatedStyle`) for media only → title → badge/time `FlowLayout` → author `FlowLayout` → 4-line content → action row → divider.
- Card_3 (`item_post_card_3_with_preview.xml`): whole row is `TouchInterceptableMaterialCardView` (margins 8/16/16/16, `cardCornerRadius 12dp`, `materialCardViewFilledStyle`) > `LinearLayout` > media frame → link → header `ConstraintLayout` (icon + 2-line names + time, same 60% guideline) → title → content → action row. No outer divider (card is the separator).

## Visual hierarchy problems
- Meta placement is inconsistent across modes: compact + card_3 use a 2-line header above the title; card_2 puts title FIRST, then badges/time, then author. Switching modes reorders the same information.
- Card_2 media-first + title-below matches a reader's scan, but the double meta flow (badges/time, then author) breaks it.
- Card_3 flair/award `FlowLayout` is **commented out** (`:179-269`) with malformed XML inside the comment (stray `android:layout_width/height` lines `:257-258` outside any tag) — dead code shipping in the layout.
- Card_3 hardcodes `app:cardBackgroundColor="#FBEEFC"` (`:12`) — a light pink baked into XML; dark/AMOLED override happens in code if at all (UNVERIFIED visually). Card_2 media card has no explicit background (theme default); compact is flat + divider. Three modes, three surface stories.

## Density
- Compact is the densest but still tall (header + body + actions ≈ 200dp+ with thumb). Card_2 paddings stack (16 outer + 16 title margin + 16 flow padding). Card_3 header `padding 16` + title/content `paddingStart/End 16` + actions `paddingTop 8 / Start/End 4` — comfortable but badge flow removal leaves a gap where flair used to be.

## Touch targets
- Same action row copy-pasted in all three audited layouts (identical `MaterialButton` specs, 24dp icons, `minWidth 0dp`, `#00000000` tint). Same <48dp concern as feed note. Play overlay is 36dp (`video_or_gif_indicator`, margin 16) — small for a primary media affordance. Type indicator 24dp.

## States (per card)
- HAVE: image error `TextView` (`drawableTop error icon` + `@string/error_loading_image_tap_to_retry`, `font_default`, centered) + `LoadingIndicator` + play/type overlays + no-preview link fallback (150dp, `ic_link_day_night_24dp`).
- Gap: no-preview fallback tints with `app:tint="@android:color/tab_indicator_text"` (`item_post_compact.xml:167`) — a **system color, not a theme attr**, so it won't follow custom themes.
- Badge chips repeat `padding 4dp + radius 6dp + font_10/12` per file instead of a shared style (see DS-7).

## RTL / large font
- Same start/end discipline as feed; `Barrier`/`Guideline` mirror-safe. Title unbounded (`title_font_18`, no maxLines) in all three; content capped at 4 lines. Badge `FlowLayout` wrapping under large font UNVERIFIED on device.

## Accessibility gaps
- No `contentDescription` in any of the three layouts (same 0-count as feed). `stickied` icon in card_2 even carries `tools:visibility="visible"` with no description. Archived/locked/crosspost 24dp status icons are image-only.
- `font_10` badges (~10sp) are below comfortable minimums for secondary text; contrast vs card fill (`#FBEEFC` card_3) UNVERIFIED.

## Performance red flags
- View-type explosion: one adapter serves 6 layout prefs × 6+ post types × autoplay variants; `onCreateViewHolder` (`:740`) inflates ~18 distinct row layouts.
- Thumb path uses the `post_compact_thumbnail_size` token + `CompactThumbnailPreloader` (good); full-bleed card_2/3 media uses `match_parent + adjustViewBounds + fitStart` with no size cap in XML — large Reddit previews decoded at row width (height unbounded until loaded → possible layout jump; `animateLayoutChanges` amplifies it).
- `animateLayoutChanges="true"` on all compact/card roots (14 files incl. these three).
