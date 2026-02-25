# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

S.EE Desktop is a collection of native desktop clients for the S.EE URL shortening, text sharing, and file hosting service. This is a monorepo with platform-specific implementations in separate directories.

## Repository Structure

```
desktop/
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
- CDN thumbnail caching via `ThumbnailService` (memory + disk two-level cache)
- `LinkFormatter` generates BBCode, HTML, Markdown embed codes with file type awareness (image/audio/video/other)
- Pagination (50 items/page) on all list views
- Local history stored in SwiftData, clearable from Settings

**Build:** Open `macos/SEE.xcodeproj` in Xcode. Requires Swift-WebP SPM dependency (`https://github.com/ainame/Swift-WebP.git`).

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
