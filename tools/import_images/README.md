# Scrap photo import
Run from repository root:
```powershell
pwsh -File tools/import_images/import.ps1
```
Place originals in scrap_photos/. Name them with a category prefix (metal_, paper_, cardboard_, plastic_, paint_, electronics_, furniture_, other_). Unknown prefixes map to OTHER. The manifest tracks SHA-256 hashes and stable image keys; reruns skip unchanged imported images and preserve earlier names. This Windows script uses the native System.Drawing JPEG encoder because WebP encoding is unavailable, constrains the longest edge to 800px and each output to 120KB, and never changes the originals. No external downloads or network image loaders are used.
The eBay screenshots are layout references only and are excluded. Photo rights/licenses have not been recorded; confirm permission before public release.
The importer also regenerates core/ui/ImportedScrapPhotos.kt, so newly imported image keys become available after rebuilding the app without hand-editing the image map.
