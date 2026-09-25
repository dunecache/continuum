# Onboarding / login audit (Pass 2.6)

## Current structure (verified)
- `activities/LoginActivity.java` + `activity_login.xml`: `CoordinatorLayout` + toolbar + 2FA prompt (`font_default`, `drawablePadding 32dp`) + full-size `LollipopBugFixedWebView` (Reddit OAuth in WebView) + centered offline layout (gone: `font_18` error text + `retry` button) + `FAB` (alternative login method, labelled `content_description_alternative_login_method`, `ic_login_24dp`).
- Parallel paths: `LoginChromeCustomTabActivity` (Custom Tab OAuth, redirect intent-filters for continuum/infinity/slide/RIF/Relay/Boost/BaconReader/Joey schemes in manifest), `AppAuthLoginActivity.kt` (Compose, `AppTheme.ComposeActivity` translucent).
- `LockScreenActivity` (`activity_lock_screen.xml`): centered `LottieAnimationView` (`@raw/lock_screen`, autoPlay + loop) + `font_20` label + `unlock` button.

## Visual hierarchy problems
- Login is a bare WebView under a thin 2FA line — Reddit's web OAuth UI (unowned) carries the whole first-run impression; no branded onboarding, no value explanation, no permissions primer. First-run experience = WebView chrome.
- 2FA prompt sits above the WebView permanently (not contextual to the 2FA step) — noise for the 90% non-2FA flow.

## Density / touch targets
- Offline retry + unlock are `wrap_content` M3 buttons (system minima, fine). FAB standard size. WebView content is Reddit's (untouchable by this audit).

## States
- HAVE: offline error + retry (login), `internet_disconnected` string, alternative-method FAB, lock-screen Lottie loop.
- UNVERIFIED: OAuth grant-error presentation, redirect-failure recovery (stranded-browser case the manifest comments describe), session-expiry re-login, biometric/app-lock integration depth (biometric dep exists: `androidx.biometric:1.2.0-alpha05`).

## RTL / large font
- Layouts are `match_parent` containers around a WebView — mirror-safe trivially. 2FA prompt `padding 16dp` + `drawablePadding 32dp` holds. Lock screen is center-constrained — safe.

## Accessibility gaps
- WebView OAuth content is Reddit's DOM (no app-side labels possible; Custom Tab path is better for password managersatura — worth preferring). App-side controls (retry, unlock, FAB) are labelled (FAB explicitly). Lottie has no `contentDescription` in XML (decorative — should be `importantForAccessibility no`, currently UNVERIFIED).

## Performance red flags
- WebView cold start on first run (process + page load); Lottie raw-asset loop on lock screen (plays even when idle — battery/animation cost; honors system animation settings UNVERIFIED).
