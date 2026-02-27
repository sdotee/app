# S.EE for Android

Native Android client for [S.EE](https://s.ee) — URL shortening, text sharing, and file hosting.

## Requirements

- Android 10+ (API 29)
- Android Studio Meerkat or later
- An S.EE API Token ([Get one here](https://s.ee/user/developers/))

## Build

```bash
cd android
./gradlew assembleDebug        # Debug APK
./gradlew assembleRelease      # Release APK
./gradlew installDebug         # Build & install on connected device
```

Or open `android/` in Android Studio and run with Shift+F10.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 (Material You) |
| Architecture | MVVM + Repository pattern |
| Networking | Retrofit 3.0 + OkHttp 5 + kotlinx.serialization |
| DI | Hilt (Dagger) |
| Local DB | Room |
| Secure Storage | EncryptedSharedPreferences |
| Preferences | DataStore |
| Image Loading | Coil 3 (with video frame decoder) |
| Navigation | Navigation Compose |

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
- Monospaced editor for source code
- Custom slug, password, and expiration support
- Domain selection and tag assignment (up to 5)

### File Upload

- **File Picker** — browse and select any file type
- **Photo Picker** — select images/videos from gallery (Android Photo Picker)
- **Camera** — capture photos (JPG) or record videos (MP4)
- Real-time upload progress with progress bar
- Thumbnails for images and video files via Coil
- Fallback icons for audio and other file types

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

Link format is file-type-aware (image, audio, video, other) and configurable in Settings.

#### Batch Operations

- Enter selection mode via toolbar button or long-press
- Multi-select with checkboxes
- Batch copy links (all selected items)
- Batch delete with confirmation (sequential execution to avoid API rate limits)
- Available on all three list screens (Links, Text, Files)

### Tags

- View all server-synced tags

### Usage Dashboard

- **Storage** — file count, used/limit (MB/GB), progress bar
- **Rate limits** for 5 categories: API Calls, Links, Text Shares, Uploads, QR Codes
- Daily and monthly counts with limits
- "Unlimited" display when limit is -1

### Settings

- Base URL configuration (for self-hosted instances)
- API Key management with verify button
- Default domain selection for each service (short links, text, files)
- File upload link format preference
- Theme: System / Light / Dark
- Dynamic Color (Material You wallpaper colors)
- Clear local history
- Privacy Policy, Terms of Service, Acceptable Use Policy links

### Android-Specific Features

- **Share Target** — receive shared text/URLs/files from other apps
- **App Shortcuts** — long-press app icon for quick actions (Shorten URL, New Text, Upload File)
- **Edge-to-Edge** — full edge-to-edge drawing with system bar insets
- **Predictive Back** — Android 14+ predictive back gesture support
- **Adaptive Icon** — gradient background + star foreground + monochrome layer for themed icons

## Architecture

```
app/src/main/java/s/how/see/
├── SEEApplication.kt              # Hilt @HiltAndroidApp
├── MainActivity.kt                # Single-activity Compose host
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   ├── SEEApiService.kt   # Retrofit interface (all API endpoints)
│   │   │   └── AuthInterceptor.kt # Auto-inject Authorization header
│   │   ├── DynamicBaseUrlInterceptor.kt  # Runtime base URL switching
│   │   ├── ProgressRequestBody.kt        # Upload progress callback
│   │   └── model/                        # DTOs with @Serializable
│   ├── local/
│   │   ├── db/                    # Room: SEEDatabase, DAOs, Entities
│   │   └── preferences/           # DataStore (AppPreferences) + EncryptedSharedPreferences (SecureStorage)
│   └── repository/                # ShortLink, TextShare, File, Tag, Usage repositories
├── di/                            # Hilt modules: AppModule, NetworkModule, DatabaseModule
├── ui/
│   ├── SEEApp.kt                  # Root composable with theme + navigation
│   ├── navigation/                # SEENavHost + TopLevelDestination (bottom nav)
│   ├── theme/                     # Material 3 theme, colors, typography
│   ├── shortlinks/                # List, Create/Edit, Stats screens + ViewModel
│   ├── textsharing/               # List, Create/Edit screens + ViewModel
│   ├── files/                     # Upload + file list screen + ViewModel
│   ├── tags/                      # Tag list screen + ViewModel
│   ├── usage/                     # Usage dashboard screen + ViewModel
│   ├── settings/                  # Settings screen + ViewModel
│   ├── more/                      # More tab (links to Tags, Usage, Settings)
│   └── components/                # DomainSelector, TagChipGroup, PaginationBar, EmptyStateView, LoadingOverlay
├── util/                          # ClipboardUtil, DateTimeUtil, LinkFormatter, UrlValidator
└── widget/                        # Glance widget (SEEWidget + SEEWidgetReceiver)
```

### Key Patterns

- **MVVM** — ViewModels expose `StateFlow`, Screens collect with `collectAsStateWithLifecycle`
- **DynamicBaseUrlInterceptor** — OkHttp interceptor replaces request URL at runtime based on DataStore preference
- **AuthInterceptor** — reads API key from `EncryptedSharedPreferences` and injects `Authorization` header
- **Room** — local-only history (no server sync); tracks created links, text shares, and uploaded files
- **Pagination** — ViewModel holds full list from Room Flow, exposes paged subset via `combine`
- **Batch operations** — selection state in ViewModel (`selectedIds` Set), sequential delete with 500ms delay

## License

MIT — see [LICENSE](../LICENSE) for details.
