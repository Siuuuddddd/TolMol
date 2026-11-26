# Product detail and bottom navigation QA

final result: passed

## Visual sources and scope

Compared the user's supplied item screenshots 2, 3 and 4 with the native Pixel 9 Pro implementation. The approved adaptation retains TolMol's charcoal/lime theme, Pixelify Sans and scrap auction content. Android continues to own the status bar, display cutout, keyboard and gesture bar; reference device chrome is not drawn into the app.

Implementation captures are in the chat visualization directory: tolmol-detail-redesign-top.png, tolmol-detail-redesign-actions.png, tolmol-detail-redesign-related.png and tolmol-detail-bid-sheet.png. Inspected the toolbar/gallery/seller/price region, action-card/specifics region, related-card region and the bottom tab strip separately at native screenshot resolution.

## Fidelity checks

- Typography: retained the bundled Pixelify Sans weights. Product heading is 22sp/28sp, price uses the existing large heading token, supporting rows use the existing body tokens, and tab labels are 12sp. Checked names, currency, kg and multi-line titles for clipping.
- Layout: pinned 56dp Item toolbar; edge-to-edge photo pager at 1.12 aspect ratio; overlaid photo count and watch control; seller/avatar/message row; prominent per-kg price; full-width 52dp actions; 16dp gutters and 16dp information-card corners; two-column specifics and horizontal 174dp recommendation cards. The information card was corrected to fill the available width during the second pass.
- Navigation: persistent 64dp native Material bar with 24dp original Google outline vectors. Home, account, search, bell and tag icons use consistent strokes, selected lime states and system gesture insets. Selling-to-Search changes the authorized mode and opens the intended destination directly.
- Color: existing dark surface/background and lime primary tokens are preserved; secondary copy remains grey and prices/actions retain clear contrast. No eBay branding, blue accent, phone images or invented store content was copied.
- Assets: real bundled scrap photos are used; the gallery remains swipeable and displays the selected feed image first. Source photo limitations and watermarks are retained. Original Google icon path data is converted to Android VectorDrawable; source attribution and Apache license are included.
- Copy: real auction rate, weight, seller/member date, approximate pickup distance, deadline and listing ID replace retail-specific metadata. Native description/product-detail dialogs expose the actual content. No fake shipping, checkout, watch count, sponsored status or purchase recommendation claim is shown.

## Interaction and regression checks

- Android assembleDebug and all eight unit tests passed, including Koin module verification and bid ViewModel tests.
- On an independent Pixel 9 Pro test instance, logged into demo.user09, opened the chair detail and swiped between both photos.
- Opened the native bid sheet and confirmed a demo bid: the live rate changed from 10 to 11 rupees/kg and estimated value from 280 to 308 rupees. Existing idempotent bid behavior remains in its ViewModel/use case.
- Added and removed the lot from the watchlist, then opened/dismissed its full description. Confirmed specifics and real nearby cards below the actions.
- Checked My TolMol activity links, Inbox alerts and authorized Selling/Search mode navigation. The bottom bar stays visible on detail.
- The original emulator was initially being used for another app; navigation traces identified external Back events rather than a TolMol navigation defect. Temporary tracing was removed. Final APK was installed and final screenshots captured after TolMol was again the foreground app.
- Location refresh remains mounted while viewing a detail, preserving eligibility updates and the existing retry UI.
- No backend source or business rules changed.

## Remaining findings

No open P0, P1 or P2 findings for the requested phone layout. Intentional differences from the retail reference are the TolMol theme/font, actual auction actions and existing app destinations. Tablet layouts remain outside the established MVP scope.
