# S.EE Desktop

Native desktop clients for [S.EE](https://s.ee) - URL shortening, text sharing, and file hosting service.

## Platforms

| Platform | Status | Directory | Technology |
|----------|--------|-----------|------------|
| macOS | ✅ Available | [`macos/`](./macos) | SwiftUI + SwiftData |
| iOS | ✅ Available | [`macos/`](./macos) | SwiftUI + SwiftData |
| Linux | ✅ Available | [`linux/`](./linux) | GTK4 + libadwaita + Rust |
| Windows | 🚧 Coming Soon | `windows/` | - |

## Features

- **URL Shortening** - Create short links with custom aliases
- **Text Sharing** - Share code snippets, notes, and text content
- **File Uploads** - Upload and share files with drag & drop (macOS) or camera/photo picker (iOS)
- **Batch Operations** - Select multiple files for batch copy links or delete
- **Local Thumbnails** - Auto-generated file thumbnails via QuickLook
- **QR Code Generation** - Generate QR codes for any link
- **Local History** - Track all your created links, texts, and files
- **Multiple Domains** - Choose from available domains for each service
- **Menu Bar** - Quick access from macOS menu bar

## Installation

### macOS / iOS

Open `macos/SEE.xcodeproj` in Xcode and build for your target device. Requires macOS 14+ or iOS 18+.

### Linux

**Arch Linux (AUR):**
```bash
yay -S see-desktop-bin
```

**Ubuntu/Debian:**
```bash
wget https://github.com/sdotee/desktop/releases/download/v0.1.0/see_0.1.0_amd64.deb
sudo apt install ./see_0.1.0_amd64.deb
```

**Fedora:**
```bash
wget https://github.com/sdotee/desktop/releases/download/v0.1.0/see-0.1.0-1.x86_64.rpm
sudo dnf install ./see-0.1.0-1.x86_64.rpm
```

**Flatpak:**
```bash
wget https://github.com/sdotee/desktop/releases/download/v0.1.0/see-0.1.0-x86_64.flatpak
flatpak install ./see-0.1.0-x86_64.flatpak
```

See [linux/README.md](./linux/README.md) for more options (ARM64, build from source).

## Getting an API Key

1. Visit [s.ee](https://s.ee) and create an account
2. Go to your dashboard
3. Generate an API key
4. Enter the key in the app's Preferences

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Links

- **Website**: [s.ee](https://s.ee)
- **API Documentation**: [S.EE API Docs](https://s.ee/docs/developers/api/)
- **Issues**: [GitHub Issues](https://github.com/sdotee/desktop/issues)
