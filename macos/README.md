# S.EE for macOS & iOS

Native SwiftUI client for [S.EE](https://s.ee) — URL shortening, text sharing, and file hosting.

## Requirements

- macOS 14.0+ / iOS 18.0+
- Xcode 16+
- An S.EE API Token ([Get one here](https://s.ee/user/developers/))

## Build

```bash
open macos/SEE.xcodeproj
```

Select your target (macOS or iOS simulator/device) and build with Cmd+R.

**SPM Dependency:** [Swift-WebP](https://github.com/ainame/Swift-WebP.git) (auto-resolved by Xcode)

## Features

### URL Shortening

- Create short links with custom slug, title, password, and expiration
- Choose from multiple domains
- View link statistics (today / month / total visits)
- Search across slug, target URL, title, and domain
- Paginated list (50 per page)
- Edit and delete existing links

### Text Sharing

- Share plain text, source code, or Markdown
- Monospaced editor for code
- Custom slug, password, and expiration support
- Domain selection and tag assignment (up to 5)

### File Upload

- **Drag & Drop** (macOS) — drop files anywhere in the upload area
- **File Picker** — browse and select files
- **Paste from Clipboard** (Cmd+V on macOS) — auto-converts images to WebP (with PNG fallback)
- **Photo Picker** (iOS) — select from Photos library
- **Camera** (iOS) — capture photos (JPG) or video (MOV to MP4 conversion)
- Real-time upload progress with percentage display
- Auto-generated thumbnails via QuickLookThumbnailing (with fallback icon)
- File metadata display: size, dimensions (images), duration (video/audio)

#### 9 Link Formats

| Format | Description |
|--------|-------------|
| Direct Link | Raw file URL |
| Share Page | File page URL |
| BBCode | `[img]` / `[video]` / `[audio]` tag |
| BBCode w/ Link | Tag wrapped in share page URL |
| BBCode w/ Direct Link | Tag wrapped in direct URL |
| HTML | `<img>` / `<video>` / `<audio>` tag |
| HTML w/ Link | Tag wrapped in share page anchor |
| HTML w/ Direct Link | Tag wrapped in direct URL anchor |
| Markdown | `![alt](url)` syntax |

Link format is file-type-aware (image, audio, video, other).

#### Batch Operations

- Multi-select files with checkboxes
- Batch copy links (all selected files in chosen format)
- Batch delete with confirmation

### Tags

- View all server-synced tags
- Refresh from toolbar (macOS) or pull-to-refresh (iOS)

### Usage Dashboard

- **Storage** — file count, used/limit (MB/GB), progress bar
- **Rate limits** for 5 categories: API Calls, Links, Text Shares, Uploads, QR Codes
- Daily and monthly counts with limits
- "Unlimited" display when limit is -1
- Color warnings at 70% (orange) and 90% (red)

### Settings

- Base URL configuration (custom server endpoint)
- API Key management with verify button
- Default domain selection for each service (short links, text, files)
- Paste image format preference (WebP or PNG)
- File upload link format preference
- Clear thumbnail cache
- Clear local history
- Sign out

## macOS Menu Bar

Quick access from the menu bar icon without opening the main window:

- **URL Shortening** — enter a URL or auto-fill from clipboard, shorten with one click
- **Paste & Shorten** — grab a URL from clipboard and shorten immediately
- **New Text Share** — jump to the text sharing sheet
- **Upload from Clipboard** — upload an image or file from clipboard
- **Upload File...** — pick a file to upload
- **Recent Items** — last 3 short links and 2 uploaded files, click to copy
- **Open S.EE** — bring the main window to front
- **Settings...** — open Preferences
- **Quit S.EE** — exit the app

## Keyboard Shortcuts

### Global

| Shortcut | Action |
|----------|--------|
| Cmd+N | New Short Link |
| Cmd+Shift+N | New Text Share |
| Cmd+, | Settings |
| Cmd+Q | Quit |
| Cmd+W | Close Window |
| Cmd+H | Hide App |

### File Upload

| Shortcut | Action |
|----------|--------|
| Cmd+V | Paste & Upload from Clipboard |

### Dialogs

| Shortcut | Action |
|----------|--------|
| Return | Confirm (default action) |
| Escape | Cancel / Dismiss |

Standard text editing shortcuts (Cmd+C, Cmd+V, Cmd+X, Cmd+A, Cmd+Z) work in all text fields.

## Architecture

```
SEE/
├── SEEApp.swift              # App entry, WindowGroup, MenuBarExtra, Settings
├── Models/
│   ├── ShortLink.swift       # SwiftData model
│   ├── TextShare.swift       # SwiftData model
│   ├── UploadedFile.swift    # SwiftData model
│   └── APIModels.swift       # Request/response types
├── Services/
│   ├── APIClient.swift       # Actor-based networking with upload progress
│   ├── KeychainService.swift # Secure API key storage
│   └── ThumbnailService.swift # QLThumbnailGenerator + memory/disk cache
├── Utilities/
│   ├── Constants.swift       # App-wide constants and UserDefaults keys
│   ├── Extensions.swift      # ClipboardService, String helpers
│   ├── ImageConverter.swift  # WebP conversion with PNG fallback
│   └── LinkFormatter.swift   # BBCode/HTML/Markdown link generation
├── ViewModels/               # @Observable view models per feature
└── Views/
    ├── ContentView.swift     # Navigation (sidebar on macOS, tabs on iOS)
    ├── Components/           # CopyButton, PaginationView, EmptyStateView, etc.
    ├── ShortLinks/           # List, Create/Edit, Stats
    ├── TextSharing/          # List, Create/Edit
    ├── Files/                # Upload, List, Batch operations
    ├── Tags/                 # Tag list
    ├── Usage/                # Usage dashboard
    └── Settings/             # Preferences

SEEMac/
└── MenuBarView.swift         # macOS menu bar popover
```

### Key Patterns

- **`#if os(macOS)` / `#if os(iOS)`** — platform-specific UI (NavigationSplitView vs TabView, NSPasteboard vs UIPasteboard, etc.)
- **`APIClient` is an `actor`** — thread-safe networking with generic `request<T>` method
- **SwiftData** — local history for links, text shares, and files
- **Keychain** — secure API key storage via Security framework
- **QLThumbnailGenerator** — local thumbnail generation for all file types with two-level cache (memory + disk)
- **LinkFormatter** — file-type-aware embed code generation (image/audio/video/other)

## License

MIT — see [LICENSE](../LICENSE) for details.
