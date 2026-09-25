# Navigation drawer / bottom nav audit (Pass 2.6)

## Current structure (verified)
- Drawer: `CustomDrawerLayout` (`activity_main.xml`) + `NavigationView` hosting a `RecyclerView` driven by `adapters/navigationdrawer/NavigationDrawerRecyclerViewMergedAdapter.java` merging ~9 section adapters (Account, AccountManagement, Header, Post, Reddit, SubscribedSubreddits, FavoriteSubscribed, Preference, PostFilterUsageEmbedded).
- Header `nav_header_main.xml`: fixed `@dimen/nav_header_height` (176dp) `RelativeLayout` (legacy `alignParent/below/toStartOf`): full-bleed banner `ImageView` (no contentDescription) + 64dp avatar (`marginTop 40dp` + 16dp sides) + name + karma (`font_default`, `toStartOf` account-switcher drop-down arrow).
- Rows: `item_nav_drawer_menu_item.xml` (full-width `padding 16dp`, icon `wrap_content + marginEnd 32dp` + text), `item_nav_drawer_subscribed_thing.xml` (same + 24dp icon + weight-1 name), `item_nav_drawer_account.xml` (24dp avatar + name), plus divider + group-title layouts.
- Bottom: `bottom_app_bar.xml` (`BottomAppBar`, fixed **48dp height**, `visibility gone`, `fabAlignmentMode center`, center spacer for FAB): 4 `ImageView` options (`weight 1`, `paddingTop/Bottom 8dp`, borderless ripple, **all `contentDescription="@null"` in XML**). Runtime labeling exists: `MainActivity.setBottomAppBarContentDescription` (`:1438`, called `:876-925`) maps each slot to its action.
- Rail: `menu/navigation_rail_menu.xml` with a candid XML comment — titles are placeholders ("Option 1"…), rail is always `labelVisibilityMode="unlabeled"`, icons are set at runtime **without titles**, so **TalkBack reads "Option 1/2/3/4"**. The comment names the two fix sites (`NavigationWrapper.setOtherActivitiesContentDescription`, `MainActivity.setBottomAppBarContentDescription`).

## Visual hierarchy problems
- Header stacks banner + avatar + name + karma + switcher in 176dp — dense; avatar `marginTop 40dp` hardcodes status-bar clearance instead of insets (edge-to-edge overlap risk on tall-status devices).
- Drawer rows use generous 16dp padding + 32dp icon gaps — readable, but subscribed rows and menu rows share identical styling (no visual distinction between navigation and content).
- Bottom bar is 48dp fixed with 8dp vertical icon padding — compact; center FAB cutout reserves space even when no FAB is attached (dead gap in 4-option mode).

## Density / touch targets
- Drawer rows are full-width 16dp-padded (≈56dp) — good, the largest list targets in the app.
- Bottom-bar options: 48dp bar minus 16dp total padding ≈ 32dp + glyph — below 48dp; bar itself is exactly 48dp so the row meets the minimum only as a whole, not per option.

## States
- Drawer sections show/hide per account state (anonymous vs signed-in) via merged adapters; account switcher drop-down swaps `AccountSection` content. Empty-subscriptions drawer state UNVERIFIED visually.

## RTL / large font
- Header is legacy `RelativeLayout` (`alignParentStart/End`, `toStartOf`) — mirrors. Drawer rows use `marginEnd`/`padding` symmetric — safe. Name/karma `toStartOf` switcher arrow holds at large font (arrow pinned to parent end).
- `labelVisibilityMode unlabeled` means large font never affects rail/bottom labels (there are none) — icons only, no text to scale.

## Accessibility gaps
- **Rail TalkBack bug (documented in-repo):** announces placeholder "Option N" instead of the bound action. Bottom-bar XML `@null` descriptions are rescued at runtime (verify one path) but rail titles are not.
- Header banner/avatar/switcher have no descriptions in XML (runtime UNVERIFIED). Drawer rows are plain `LinearLayout` clickable — role/label announcement UNVERIFIED.

## Performance red flags
- Merged adapter with ~9 child adapters + `RecyclerView` inside `NavigationView` — fine. Banner + avatar Glide loads per account switch; no overdraw mitigation beyond `clipToPadding false`.
