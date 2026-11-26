# TolMol Android handoff

The Android app lives in `android/` and uses the backend in `backend/`. The root README covers setup, endpoint settings, emulator location, and build commands.

- The Compose app uses Koin, type-safe Navigation 3, Retrofit, DataStore JWT sessions, and a shared OkHttp WebSocket. Separate buyer and seller tabs use feature-scoped presentation, domain, and data packages. The Android unit tests verify the Koin module.
- Included flows are registration and sign-in, role-specific device location, nearby discovery and watchlists, live auctions, bid history, seller inventory and award, private chat, read/unread alerts, pickup inspection, settlement, and dispute cancellation and relisting.
- Use two accounts and nearby emulator locations to exercise one sale. With a different measured weight, exercise both revision confirmation and cancellation on separate deals.

Verified on the Pixel 9 Pro emulator against the local backend. Sign-in and role selection loaded nearby inventory, and a competing bid sent to the backend updated the open auction over the shared WebSocket.

Background push, maps, uploads, payment processing, offline storage, and localization are outside the Android MVP. Production requires a hosted backend with TLS. FCM can be integrated later using the backend's existing device-registration API.

## Google sign-in — current contract

- Android uses Credential Manager with GOOGLE_WEB_CLIENT_ID from ignored android/local.properties. The auth feature's dedicated GoogleSignInUseCase calls POST /auth/google with idToken, loads the profile and stores the returned TolMol JWT through the normal DataStore session path. Success opens home directly, preferring buyer discovery when available and otherwise using the account's allowed seller home. Both account forms and the welcome action are wired.
- Missing configuration displays "Google sign-in is not configured yet" without breaking email login or registration. Cancellation shows no error; no-account, connection and rejected-token failures have readable retry messages. Facebook UI, SDK dependency and callback bridge are removed.
- Backend V5 supports verified, passwordless Google accounts and a unique subject. Verified email can link an existing account once, preserving its password/profile/roles. Google-only accounts cannot use password login. The new endpoint is Redis-rate-limited.
- OAuth setup completed locally on 2026-11-26: the supplied Web client ID is configured in the ignored android/local.properties and backend/.env files, using GOOGLE_WEB_CLIENT_ID and GOOGLE_CLIENT_ID respectively. The Android OAuth client is registered in the same Google project. No client ID or client secret is committed. New checkouts still follow README's GOOGLE SIGN-IN setup steps; no Facebook or Firebase configuration is needed.
- Verification: full backend build and 31 tests passed, including signed-RSA wrong-audience/signature/issuer/expiry tests, Google account creation/linking and the IP limit. Android assembly and all 5 JVM tests passed, including Google failure/retry/role state and Koin graph verification.

## Shared UI and MVP completion

- Every app text style inherits the existing bundled Pixelify Sans font from core/ui/Typography.kt. No screen owns a font family or light theme. Native welcome text, form fields/helper text, list values, dialog text, chips and bottom-navigation labels use it. All filled/outlined actions share pixel shapes and the dark factory/lime palette.
- The welcome layout and graphics remain, with native scalable lettering over a textless original-style asset. Account forms keep their layout and behavior but use the same dark background and factory header as the welcome screen. No cream/cobalt theme remains in screen code.
- All original Android MVP milestones are implemented and audited through navigation and loading/empty/error/retry rendering. No Hilt references remain in Android source or dependency configuration. No additional frontend implementation milestone is pending.
- Current validation: assembleDebug plus all 5 Android unit tests pass (including Koin and Google retry/role state); the complete backend build and all 31 tests pass. On Pixel 9 Pro the friendly missing-ID behavior was checked on welcome and registration, Facebook absence was checked, password login reached role selection/discovery against the updated backend, and enlarged 1.3 font scaling kept native welcome text and Google reachable. The local backend is running with V5 applied and /health UP. Real Google account acceptance was completed on 2026-11-26 as recorded below.

## Live Google acceptance — 2026-11-26

- Loaded the supplied Web client ID from the owner-downloaded OAuth JSON into both ignored local settings; the client secret was neither copied into the project nor used by the ID-token flow.
- Restarted the backend with the new configuration, rebuilt Android and passed all 5 unit tests, installed the configured APK on Pixel 9 Pro, and confirmed /health UP. An untrusted Google token is rejected with HTTP 401 instead of the previous missing-configuration response.
- Tapping the Google icon opened Google's native account chooser for TolMol. Continuing with the existing emulator account and its basic identity consent completed real Google token verification, created/restored a TolMol JWT in DataStore, and displayed buyer/seller role selection. The missing-client-ID block is resolved. No remaining credential-dependent frontend implementation item is blocked for this configured local checkout.
- Selecting Buyer mode completed location sync and loaded the authenticated discovery home, showing the expected empty nearby-lots state. The configured app is installed and the local backend is running.
- Font legibility: registered the bundled variable font's real 400/500/600/700 weights and disabled synthetic bold. Medium card titles and slightly wider tracking keep scrap readable. Android assembly passed; the updated APK is installed and visually verified on Pixel 9 Pro.

## Direct home after sign-in — 2026-11-26

- Removed the post-login role-selection screen and its unused composables. Password/Google sign-in, registration and session restore now select an authorized initial role atomically with the account and render home directly. Buyer-capable accounts start in nearby discovery; seller-only accounts start in listings.
- Accounts with both roles use Switch to selling / Switch to buying on home; switching changes the home directly and resets that mode's navigation stack. A role not granted by the backend cannot be selected. Single-role accounts do not display a switch action.
- assembleDebug and all 5 Android JVM tests pass. The auth test covers direct entry after password/Google login and registration, seller-only restore, failure/retry and role authorization. Pixel 9 Pro checks confirmed authenticated app restore goes directly home, a fresh Google login goes directly home, and the mode switch opens seller inventory and returns to discovery. The updated APK is installed; backend behavior is unchanged.


## Discovery feed and supplied photos — 2026-11-26

Discovery now has pinned search, category chips, vertical sections of horizontal cards, See all grids, optimistic watch hearts with rollback/retry, recent views, countdowns and ending-soon badges. It uses the existing dark pixel typography and bottom navigation. Loading skeletons, empty states, retry and pull refresh are included. Detail loads public seller/date, description, image and approximate distance, preserving live auction bidding. Publish supports category/title/description.

Seventeen supplied photos were copied to scrap_photos/ and imported into drawable-nodpi with stable image keys: 2 paper, 2 cardboard, 1 metal, 3 paint, 5 electronics and 4 other. Plastic/furniture and unknown image keys use local vector placeholders. All optimized photos are <=800px longest edge and <=120KB; no network image loader or eBay images are bundled. Originals have no recorded license; confirm usage rights before public release.

See README DEMO DATA for optional startup, reset and local accounts. Emulator location must match the configured seed center (default 19.0760,72.8777). Demo email sign-in is available through Create an account → Already have an account? Sign in. Google sign-in remains a separate configured provider. Search is client-side and recent views are session-only.

Validation: assembleDebug and all 8 Android unit tests pass, including Koin module verification and the new feed/ViewModel tests. Full backend build passes with 33 tests. Installed on Pixel 9 Pro emulator: demo email login, supplied photos, watch toggle, section grid, public detail, live highest-bid update (15 to 16 per kg), six conversation rows with messages/composer, and the three deal states were checked. Detail loading effects now key on both route and ViewModel to survive Nav3 owner changes. Import rerun preserved all 17 keys and files; dimensions/size checks found zero violations.

### Additional metal product photos — 2026-11-26

Imported six supplied photos (two iron rods, two copper wire, two aluminium sheets), bringing the bundled catalog to 23 photos. Stable keys scrap_metal_02/03 map to iron rods, 04/05 to copper wire, and 06/07 to aluminium sheet offcuts. Each pair is used across the two corresponding demo lots. Demo startup refreshes their image keys on existing OPEN lots without resetting bids, chats or deals. Original files and watermarks are retained; optimized images keep the existing 800px/120KB limits.

### Utensil and brass fitting photos — 2026-11-26

Added four supplied originals and optimized bundled JPEGs, bringing the catalog to 27 photos. scrap_metal_08/09 are assigned to the kitchen utensil lots; scrap_metal_10/11 are assigned to the brass fitting lots. Each supplied photo is used by one corresponding repeated demo lot. Existing OPEN demo lots receive the image keys on startup without a data reset. The importer preserves originals and existing keys, and keeps optimized files within 800px and 120KB.

### Plastic, appliance and furniture photos — 2026-11-26

Added all 18 supplied photos: 4 plastic, 6 electronics and 8 furniture, bringing the bundled catalog to 45 photos. Bottle/crate pairs use plastic keys 01–04; refrigerator/fan/mixer pairs use electronics 06–11; chair/wardrobe/table/sofa pairs use furniture 01–08. All originals are preserved and all optimized images follow the 800px/120KB limits.

Some products have only one open demo lot, so detail now includes a swipeable local two-photo gallery with a page count. The selected feed thumbnail stays first. This uses bundled related-photo keys and needs no image service or backend schema change. Fans and sofa labels were made generic to fit both supplied photos; existing OPEN demo records refresh the seeded content and image keys on startup without resetting bids or deals.

Validation: Android assembleDebug and all 8 unit tests pass; full backend build and 33 tests pass. Checked all new title/image-key mappings in the running database and swiped the sofa detail gallery from Photo 1 of 2 to Photo 2 of 2 on Pixel 9 Pro. All 45 optimized photos meet the import size limits, and rerunning the importer preserves the catalog.


### Google login after Docker restart — 2026-11-26

Investigated the generic Google verification error: port 8080 belonged to a Docker app container using the November 21 image and no GOOGLE_CLIENT_ID environment value. The saved backend and Android OAuth client IDs matched; the running container had never reloaded that configuration. Rebuilt bootJar and recreated only the app service with --build --force-recreate, then verified the runtime audience configuration matched Android and health returned UP. Retrying Google sign-in on the emulator successfully opened Discovery.

README Google troubleshooting now distinguishes host bootRun from container startup and includes the exact rebuild/recreation commands. Docker Desktop/container restart alone retains old image/environment settings; no OAuth secrets or IDs were added to tracked files.

### Product detail and native navigation redesign — 2026-11-26

Adapted the supplied item-page layout to TolMol: pinned Item toolbar, edge-to-edge local photo pager with count/watch overlays, title then seller/avatar/message row, prominent real per-kg price, estimated lot value, collection/weight rows, full-width bid/watch/message actions, a live auction card, item specifics, description/details dialogs and horizontal real nearby-lot cards. The current dark/lime palette and Pixelify Sans remain unchanged. Bid entry uses a native modal sheet and retains the existing ViewModel/use-case/idempotency flow; no backend rules changed.

Use five native tabs (Home, My TolMol, Search, Inbox, Selling), including on detail screens. My TolMol exposes bids, watches, messages and deals; Inbox opens existing alerts. Selling/Search change authorized roles and preserve the intended destination. The bar uses Google's original outlined VectorDrawables, 24dp icons, a 64dp content height and system-owned gesture insets. Source and Apache license are in android/UI-ICONS.md and android/GOOGLE-MATERIAL-ICONS-LICENSE.txt.

Reference commerce controls map to genuine TolMol actions. No basket, instant purchase, shipping promise, sponsored recommendations, synthetic popularity/watch counts or payment flow was invented. Nearby cards and prices are existing backend data; seller initials are generated from the actual display name, as in the supplied initial-avatar reference.

Validation: final assembleDebug and all eight Android unit tests pass. Native Pixel 9 Pro checks cover the gallery, bid sheet/live demo bid, watch/unwatch, description dialog, recommendation cards and five-tab navigation. Final screenshots are in the chat visualization folder; design-qa.md records the comparison and intentional auction-specific adaptations. No backend changes were required.

### Cream brick design system — 2026-11-27

The new request supersedes the dark/lime theme and single-font rule. The attachment folder contains only the written specification; the two new images were not supplied, so exact color sampling is unavailable. design_reference/README.md records this limitation. Use the supplied fallback palette: sky #0B3FA8–#2E86DE, navy #0B1630, red #E8212A, blue #1F55D6, yellow #FBBF1E, cream #FBF5E8, steel #8A93A0 and dark tile #1A1F2B.

The shared source of truth is core/ui/theme (Color, Shape/Dimens, Typography, TolMolTheme). Pixelify Sans 700 remains the closest bundled open-license pixel display face; Nunito Sans variable 400–900 is bundled for readable geometric body/labels/prices. Both are SIL OFL. Every Material text style and light color slot is explicit; dynamic color and system dark mode are not used.

Core controls live in core/ui/components: BrickButton/secondary/card/chip, OutlinedPixelText, TolMolTextField, PixelDialog/Snackbar, SkeletonBrick, EmptyState, yellow-underlined links, SocialSignInButton, hanging SignBoard, SteelTopBar/BrickBottomBar, hazard/stone footers and lightweight Canvas art. Tokens use 20dp page gutters, 32dp auth gutters, 12dp gaps, 56dp fields, 64dp actions, 2.5dp outlines, 5dp hard shadows, 12dp studs with 24dp gaps and 8dp control corners. All main components have Android Studio previews. Legacy PixelButton names delegate to the new controls for compatibility.

Auth/onboarding now use the shared sky/cream scenes, tile sign, traffic lights, rail cart, cardboard art, studs, hazard strips and stone footer. Password visibility uses the official native eye icons; Google uses the shared white/navy pill. The existing name/email/password state, validation, handlers and Google flow are unchanged. Role cards remain informational; no role picker or navigation step was introduced. SignInPreview, CreateAccountPreview and RoleSelectionPreview are available in Android Studio. Removed three obsolete full-screen auth artwork bitmaps (about 4.9MB); full reference screenshots are not shipped. Auth build and all Android unit tests pass.

### Reusing the final brick UI

New screens should use core/ui/Screen for the cream scroll surface, outlined heading, spacing and footer; RowCard for clickable rows; BrickCard for non-clickable groups; ScreenFeedback for skeleton/error/retry UI. Use the core/ui/components controls instead of raw Material buttons, chips, fields and dialogs. BrickButton defaults red, BrickSecondaryButton blue, and BrickTone.Yellow uses navy text. Pass fillMaxWidth only for full-width actions. Content density can use BrickCard's contentPadding parameter. Use Typography styles for all text, GameDimens for sizes and GameColors for color; never assign feature-local fonts or color literals. Keep decorative scenes out of scrolling content; product photos stay ordinary optimized images.

All current routes are migrated, including dialogs, bottom sheet, category dropdown, location feedback and read-only chat. Material defaults inherit the explicit light scheme for native selection, progress and switches. SteelSystemBars controls native icon contrast without changing navigation. ComponentPreviews includes the snackbar, dialog, buttons, fields, cards/chips, empty/loading, scene/sign and native bar previews. AuthSceneLayouts and WelcomeScreen contain the screen previews.

Final validation on 2026-11-27: assembleDebug + all 8 Android unit tests pass; diff whitespace check passes. Shared ViewModel bodies and backend/domain/data/DI are unchanged. Emulator inspection covered auth/onboarding, feed, detail/actions/bid sheet, account/bids/deal, alerts, empty listings, publishing/dropdown and conversations/read-only thread; no transaction or message was submitted. QA screenshots are in the chat visualization folder with brick-* filenames. design-qa.md records limitations. The latest debug APK is android/app/build/outputs/apk/debug/app-debug.apk; run the app from Android Studio to install it on your own emulator/device. Real Google sign-in still needs a Google account on that device.

Reference limitation: the latest attachment supplied the written brief but neither of the two new reference images. No exact sampling was possible; fallback palette, closest OFL fonts and original Canvas art implement the brief. No reference screenshots are embedded in the app. All remaining drawables are under 150KB; four obsolete full-screen artwork assets were removed across this rollout.

Product-detail layout fix (2026-11-27): BrickButton now wraps content naturally and propagates explicit width constraints; do not add intrinsic width or an unconditional inner fillMaxWidth. This fixes inline Message/See all controls squeezing sibling text. Full-width action modifiers still work. Verified seller and related-product rows on 320dp/426dp native layouts; build + 8 unit tests pass. Corrected APK installed on the running emulator; its viewport was restored.
