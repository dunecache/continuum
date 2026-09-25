# Search audit (Pass 2.3)

## Current structure (verified)
- Entry: `activities/SearchActivity.java` + `res/layout/activity_search.xml` (187 lines): toolbar-embedded query field + scope row + recent grid + autocomplete list, all in a `NestedScrollView`.
- Query field: raw `EditText` (`font_20`, `background #00000000` hardcoded transparent, `maxLines 1`, `inputType textNoSuggestions`, `imeOptions actionSearch`) + 4 icon actions (clear `ic_cancel_24dp`, link handler, incognito keyboard toggle, random subreddit) — each `wrap_content + padding 8dp + actionBarItemBackground` with proper `contentDescription`s (`delete_texts`, `handle_link`, `incognito_keyboard_off`, `random_subreddits`). Labelled toolbar icons — best in audit.
- Scope row (`RelativeLayout`, legacy `layout_toEndOf/toStartOf/alignParentEnd/centerVertical`): `search_in` accent label + subreddit name + history (`ic_history`) + delete-all (`ic_delete_all_24`) icon-only `MaterialButton`s (`strokeWidth 0dp`, no text, **no XML contentDescription**).
- Recent queries: 2-col `GridLayoutManager` of filled cards (`item_recent_search_query.xml`: radius 12dp, query + where texts `singleLine + ellipsize marquee + marquee_forever`, select-query arrow `padding 8dp`). Dedicated history screen `activity_search_history.xml`: same 2-col grid + centered `no_search_history` empty text.
- Autocomplete: `subreddit_autocomplete_recycler_view` (gone, `LinearLayoutManagerBugFixed`).
- Results: `SearchResultActivity` + `activity_search_result.xml` (tabs Posts/Subreddits/Users presumably — tab titles UNVERIFIED in this pass) + `ViewPager2` + `FAB(@null)`; dedicated `SearchSubredditsResultActivity` / `SearchUsersResultActivity` with `fragment_subreddit_listing` / `fragment_user_listing` (latter: `SwipeRefresh` + `RecyclerView` + 150dp error layout, same pattern as feed).
- Trending: `item_trending_search.xml` (elevated card radius **16dp** vs recent's filled 12dp) with image/error states + title overlay (`maxLines 2`, bold `title_font_20`, hardcoded `#FFFFFF` on `@drawable/trending_search_title_background` gradient).

## Visual hierarchy problems
- Toolbar carries **4 utility icons** next to the query — clear/link/incognito/random compete with typing; random-subreddit and link-handler are destinations, not text-field tools, and deserve placement outside the input row.
- Scope + history + delete-all share one `RelativeLayout` row — three concerns (where to search, see history, nuke history) in a single line with two icon-only buttons.
- Recent cards vs trending cards use different radii/elevations (12dp filled vs 16dp elevated 2dp) for the same "query shortcut" concept.

## Density
- Scope row insets 16dp all around; recent card texts 16/16/8/4dp. Autocomplete/history lists `wrap_content` height inside `NestedScrollView` — nested scrolling lists (grid + autocomplete `RecyclerView`s inside a scroll view) instead of a single list.

## Touch targets
- Toolbar icons ~40dp effective (24dp glyph + 8dp padding) — below 48dp, though labelled. History/delete-all and select-query arrow same size. No `minHeight` anywhere in sampled search layouts.

## States
- HAVE: `no_search_history` empty text; recent grid `gone` until data; autocomplete `gone` until input; trending image loading/error states; user/subreddit listing error layouts; `imeOptions actionSearch`.
- MISSING: no skeleton for autocomplete; no visible loading state on the entry screen itself (results carry the load UI).

## RTL / large font
- Entry toolbar uses `marginEnd 16dp` + `LinearLayout` (mirrors via layout direction) — ok. Scope row is legacy `RelativeLayout` (`toEndOf/toStartOf/alignParentEnd`) — mirrors, but `marginStart 32dp` on the subreddit name is a magic offset from the accent label (fragile at large font).
- Recent query texts are `singleLine + marquee_forever` — marquee never stops (motion + TalkBack concern); long queries scroll instead of wrapping/truncating statically.
- Query `font_20` (20sp at Normal) is the largest input text in the app — good for entry, but toolbar height is `wrap_content` so large font grows the whole app bar (UNVERIFIED clipping).

## Accessibility gaps
- Toolbar 4 icons labelled — good. History/delete-all/select-query icon-only buttons have **no XML contentDescription** (runtime UNVERIFIED). Result-screen FAB is `@null` again (`activity_search_result.xml`).
- `EditText` has `hint="@string/search"` but no `labelFor`/`autofillHints` in XML (UNVERIFIED whether code sets them).
- Marquee `singleLine` recent queries are poor for screen readers (scrolling text rarely announces fully).

## Performance red flags
- Two `RecyclerView`s (grid + autocomplete) with `wrap_content` height inside a `NestedScrollView` — all items inflate/measure on every scroll; grid is 2-col cards with marquee text (constant animation = constant invalidation).
- `marquee_forever` on every recent-query card keeps the UI thread animating while the screen is visible.
- Trending images load full-width `match_parent + adjustViewBounds` with no size cap in XML (same jump risk as post cards).
