# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

S.EE Desktop is a collection of native desktop clients for the S.EE URL shortening, text sharing, and file hosting service. This is a monorepo with platform-specific implementations in separate directories.

## Repository Structure

```
desktop/
├── android/        # Jetpack Compose + Material 3 (Android)
├── linux/          # GTK4 + libadwaita + Rust
├── macos/          # SwiftUI + SwiftData (macOS & iOS)
├── windows/        # Coming soon
└── .github/
    └── workflows/
        └── release-linux.yml   # Automated Linux releases
```

## Platform-Specific Guidance

### macOS / iOS

Native SwiftUI app targeting macOS and iOS. Uses SwiftData for local history, Keychain for API key storage, and an actor-based API client.

**Project structure:**
```
macos/
├── SEE.xcodeproj/
├── SEE/
│   ├── SEEApp.swift              # App entry point
│   ├── Models/                   # SwiftData models (ShortLink, TextShare, UploadedFile, APIModels)
│   ├── Services/                 # APIClient (actor), KeychainService, ThumbnailService
│   ├── Utilities/                # Constants, Extensions, ImageConverter (WebP), LinkFormatter
│   ├── ViewModels/               # @Observable view models per feature
│   └── Views/
│       ├── ContentView.swift     # Main navigation (sidebar)
│       ├── Components/           # Reusable views (CopyButton, PaginationView, EmptyStateView, etc.)
│       ├── ShortLinks/           # Short URL CRUD + stats
│       ├── TextSharing/          # Text/code/markdown sharing
│       ├── Files/                # File upload with drag & drop, paste, batch copy
│       ├── Tags/                 # Tag management
│       ├── Usage/                # API usage stats
│       └── Settings/             # API config, default domains, preferences
└── SEEMac/
    └── MenuBarView.swift         # macOS menu bar extras
```

**Key architecture:**
- `APIClient` is an `actor` with generic `request<T>` and `requestNoBody` methods
- `#if os(macOS)` / `#if os(iOS)` for platform-specific UI (NSPasteboard vs UIPasteboard, etc.)
- Image paste upload converts to WebP lossless via [Swift-WebP](https://github.com/ainame/Swift-WebP) library with PNG fallback
- Local thumbnail generation via `QLThumbnailGenerator` (QuickLookThumbnailing) with memory + disk cache
- `LinkFormatter` generates BBCode, HTML, Markdown embed codes with file type awareness (image/audio/video/other)
- Pagination (50 items/page) on all list views
- Local history stored in SwiftData, clearable from Settings

**iOS-specific features:**
- Camera photo capture with HEIC → JPG conversion
- Camera video capture with MOV → MP4 conversion (AVFoundation)
- PhotosPicker integration for photo library access
- iOS 18+ Tab API for tab navigation
- `.presentationDetents` for adaptive sheet sizing
- Batch file selection, copy links, and delete operations

**Build:** Open `macos/SEE.xcodeproj` in Xcode. Requires Swift-WebP SPM dependency (`https://github.com/ainame/Swift-WebP.git`). Supports macOS 14+ and iOS 18+.

### Android

Native Android app built with Jetpack Compose and Material 3. Uses Room for local history, EncryptedSharedPreferences for API key storage, and Hilt for dependency injection.

**Project structure:**
```
android/
├── app/
│   └── src/main/java/s/how/see/
│       ├── SEEApplication.kt              # Hilt application entry point
│       ├── MainActivity.kt                # Single-activity Compose host
│       ├── data/
│       │   ├── remote/
│       │   │   ├── api/SEEApiService.kt   # Retrofit API interface
│       │   │   ├── api/AuthInterceptor.kt # OkHttp auth header injection
│       │   │   ├── DynamicBaseUrlInterceptor.kt # Runtime base URL switching
│       │   │   ├── ProgressRequestBody.kt # Upload progress tracking
│       │   │   └── model/                 # Request/response DTOs (kotlinx.serialization)
│       │   ├── local/
│       │   │   ├── db/                    # Room database, DAOs, entities
│       │   │   └── preferences/           # DataStore + EncryptedSharedPreferences
│       │   └── repository/                # Repository layer (5 repositories)
│       ├── di/                            # Hilt modules (App, Network, Database)
│       ├── ui/
│       │   ├── navigation/               # Navigation Compose routes + bottom nav
│       │   ├── theme/                     # Material 3 theme + dynamic color
│       │   ├── shortlinks/               # Short URL CRUD + stats
│       │   ├── textsharing/              # Text sharing CRUD
│       │   ├── files/                    # File upload + management
│       │   ├── tags/                     # Tag list
│       │   ├── usage/                    # Usage dashboard
│       │   ├── settings/                 # App preferences
│       │   └── components/               # Reusable composables
│       ├── util/                          # ClipboardUtil, DateTimeUtil, LinkFormatter, UrlValidator
│       └── widget/                        # Glance widget
├── gradle/libs.versions.toml             # Version catalog
└── build.gradle.kts
```

**Key architecture:**
- MVVM with `ViewModel` + `StateFlow` + `collectAsStateWithLifecycle`
- Retrofit 3.0 + OkHttp 5 with `DynamicBaseUrlInterceptor` for runtime base URL switching
- `AuthInterceptor` auto-injects API key from `EncryptedSharedPreferences`
- kotlinx.serialization for JSON parsing
- Room database for local history (links, text shares, uploaded files)
- `LinkFormatter` with 9 display types (Direct Link, BBCode, HTML, Markdown, etc.)
- Pagination (50 items/page) on all list views
- Batch selection with long-press or toolbar button, batch copy links and sequential batch delete
- Adaptive icons with gradient background, white star foreground, and monochrome layer

**Build:**
```bash
cd android
./gradlew assembleDebug      # Debug build
./gradlew assembleRelease    # Release build
./gradlew installDebug       # Install on connected device
```

Requires Android SDK with API 36. Minimum deployment target is API 29 (Android 10).

### Linux

See [linux/CLAUDE.md](./linux/CLAUDE.md) for detailed guidance including:
- Build commands (`cargo build`, `cargo run`)
- Architecture (async bridge pattern, local history storage)
- API reference and SDK usage
- UI patterns with GTK4/libadwaita

**Quick reference:**
```bash
cd linux
cargo build --release    # Build
cargo run --release      # Run
cargo check              # Check for errors
cargo clippy             # Lint
cargo fmt                # Format
```

## CI/CD

### Linux Release Workflow

Triggered by version tags (`v*`) or manual dispatch. Builds:
- Flatpak (x86_64 only - no ARM64 container available)
- deb packages (x86_64, ARM64)
- rpm packages (x86_64, ARM64)

**Testing releases:**
```bash
git tag v0.1.0-test1
git push origin v0.1.0-test1
```

**Creating official release:**
```bash
git tag v0.1.0
git push origin v0.1.0
```

## Packaging

Linux packaging files are in `linux/packaging/`:
- `PKGBUILD` - Arch Linux (build from source)
- `PKGBUILD-bin` - Arch Linux (prebuilt binary)
- `flatpak/ee.s.app.yml` - Flatpak manifest
- `deb/postinst` - Debian post-install script
- `rpm/post_install.sh` - RPM post-install script

Package metadata is in `linux/Cargo.toml` under `[package.metadata.deb]` and `[package.metadata.generate-rpm]`.

## AUR Packages

Published as `see-desktop` and `see-desktop-bin` on AUR (name `see` was taken).
