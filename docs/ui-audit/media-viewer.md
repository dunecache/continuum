# Media viewer audit (Pass 2.5)

## Current structure (verified)
- Five viewers, one per source: `ViewImageOrGifActivity` (`activity_view_image_or_gif.xml`, 143 lines: black root + `BigImageView` + APNG `GifImageView` + error layout + bottom `BottomAppBar` toolbar), `ViewVideoActivity` (`activity_view_video.xml`, 67 lines: black root + `HaulerView > LockableNestedScrollView > FrameLayout(PlayerView + spinner + transparent AppBar)`), `ViewImgurMediaActivity` (same Hauler scaffold + `ViewPagerBugFixed`), `ViewRedditGalleryActivity` (same + gallery pager), `shadowbox/ShadowboxActivity` (`activity_shadowbox.xml`: black `FrameLayout` + `ViewPager2` only; pages share `shadowbox_info_panel.xml`, documented as pixel-stable across page types).
- Controls: image bottom bar (title `maxLines 2 + ellipsize`, 5 weight-1 actions: rotate L/R, download, share, wallpaper); video `PlayerView` with custom `exo_playback_control_view.xml` (title + mute + transport group + seek); gallery/imgur pagers + transparent toolbars.

## Gestures (verified in code/layout)
- HAVE drag-to-dismiss on video/gallery/imgur (`HaulerView`, `dragUpEnabled true`; video wires `setOnDragDismissedListener → overridePendingTransition(0, slide)`, `:480-484`).
- HAVE vertical swipe-back on images via vendored Slidr (`Slidr.attach(VERTICAL, distanceThreshold 0.125)`, `ViewImageOrGifActivity.java:162`).
- HAVE pinch-zoom images (`BigImageView` + `SubsamplingScaleImageView` double-tap config `240dpi / ZOOM_FOCUS_FIXED`, `:301-302`); HAVE pinch/pan on video (custom handler ported from Slide: pinch scales `videoFrame`, swipe-to-dismiss auto-disabled while `scaleFactor > 1`, tap toggles controls, `:225-230,1091-1235`).
- MISSING/UNVERIFIED: shared-element / container-transform transitions between feed and viewer (none found — dismiss uses `overridePendingTransition` slide); double-tap behavior on video UNVERIFIED; gallery page indicator style UNVERIFIED.

## Visual hierarchy problems
- Image bottom bar mixes title + 5 actions in one `BottomAppBar` (`backgroundTint #80000000`, `minHeight 0dp`, gone until tap) — title (`font_20`, `#FFFFFF` hardcoded) + actions share the scrim; hierarchy is fine over media.
- Video overlays transparent toolbar + controller on black — standard. Shadowbox info panel (`background #80000000`, texts white/`#CCFFFFFF` hardcoded) is documented as intentional over-media styling — legit hardcodes, but unthemed.
- Exo controls carry hardcoded `#FFFFFF` title + `#444141` mute background (`exo_playback_control_view.xml`) — same over-media justification, still unthemed.

## Density / touch targets
- Image bar actions are `0dp weight-1 + padding 12dp` (≈48dp+ targets, borderless ripple, ALL labelled — best action row in the audit).
- Video `PlayerView` transport buttons are M3 `MaterialButton`s in a constraint chain — sizes UNVERIFIED in XML excerpt. Toolbar `minHeight actionBarSize` holds.

## States
- HAVE: spinner + tap-to-retry error (`ic_error_outline_white`, white text on black) for images; progress + error layouts for imgur/gallery; `optimizeDisplay + tapToRetry false` on `BigImageView` (retry via error tap, not image tap).
- MISSING: no download/share progress indication in the bar (actions fire and leave); wallpaper result UNVERIFIED.

## RTL / large font
- Image bar is weighted linear (mirrors order; icons symmetric except rotate L/R which correctly swap meaning — labels distinguish them). Title `maxLines 2` holds at large font. Video controller chain is bias-centered — mirroring UNVERIFIED for seek direction.

## Accessibility gaps
- Image bar: all 5 actions HAVE `contentDescription` (rotate/share/download/wallpaper) — exemplar, keep.
- Shadowbox panel icon is explicitly `@null` (`shadowbox_info_panel.xml`) while avatar conveys source — gap. Video controller descriptions UNVERIFIED (custom layout excerpt shows no `contentDescription` on mute/transport).
- Drag-to-dismiss has no announced alternative (done button UNVERIFIED — toolbar back presumably).

## Performance red flags
- `BigImageView` (subsampling) + Glide `cancelAll` on destroy (`:704`) — good citizenship. APNG path holds a second full-size `GifImageView` (gone) per viewer.
- Video: `texture_view` surface + `animateLayoutChanges` on both `PlayerView` and `AppBarLayout` — layout transitions during playback/rotation. Custom touch pipeline (pinch + pan + scrub + dismiss arbitration, ~150 lines) is hand-rolled — powerful but high-maintenance vs `ZoomLayout` (which is a dependency but unused here — UNVERIFIED where `zoomlayout` is used).
