# Inbox audit (Pass 2.4)

## Current structure (verified)
- `activities/InboxActivity.java` + `res/layout/activity_inbox.xml` (64 lines): `CoordinatorLayout` + collapsing toolbar + `TabLayout` (**Notifications / Messages** with custom-font tabs, `:298-304`) + `ViewPager2` + compose `FAB` (`contentDescription="@string/content_description_compose_message"`, `ic_add_day_night_24dp`) — the only FAB in the audit with a real description.
- Page: `fragments/InboxFragment.java` + `fragment_inbox.xml` (46 lines): `FrameLayout` + `SwipeRefreshLayout` + `RecyclerView` + centered 150dp error layout (same pattern as feed).
- Rows: `item_message.xml` (flat `LinearLayout`, `padding 16dp`, `cardViewBackgroundColor`: author `font_default` + subject **`font_16`** + title `font_default/title_font_family` + Markwon content, each `marginBottom 8dp`); `item_private_message_received.xml` (audited head-60: 36dp avatar + 12dp-padded bubble `content_font_16`, `width_max wrap + width_percent 0.7 + bias 0`, gone time + gone 16dp copy icon) + sent twin.

## Visual hierarchy problems
- Notification row stacks **4 text blocks** (author/subject/title/body) with identical 8dp gaps and no dividers — subject (`font_16`) vs title (`font_default` in title family) compete; unread vs read distinction is code-driven (UNVERIFIED in XML).
- Private-message bubbles cap at 70% width with 12dp internal padding — good chat pattern; time/copy hidden by default keeps rows clean.

## Density / touch targets
- Message rows breathe (16dp padding, 8dp gaps). Copy affordance is a 16dp glyph + 8dp padding (~32dp) — small; appears on demand (gone). Row tap behavior (open/reply) is code-side, UNVERIFIED.

## States
- Same 150dp error layout + `SwipeRefresh` as feed. Tabs separate notifications from messages. Mark-as-read flows live in menus/toasts (`read_all_messages_*` strings, `:337-359`).

## RTL / large font
- Start/end + bias-based bubbles mirror (sent twin presumably bias 1 — UNVERIFIED beyond received). Bubble `width_max wrap + percent 0.7` survives large font; time `gone` by default avoids wrap fights.

## Accessibility gaps
- FAB labelled — good (keep as pattern for the `@null` FABs elsewhere).
- Copy icon and message rows carry **no XML `contentDescription`** (runtime UNVERIFIED). Unread state announcement UNVERIFIED.

## Performance red flags
- None structural: flat rows, standard lists. Markwon content per row (same per-row parse cost as comments, smaller bodies).
