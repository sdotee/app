# S.EE Desktop

Native desktop clients for [S.EE](https://s.ee) - URL shortening, text sharing, and file hosting service.

## Platforms

| Platform | Status | Directory | Technology |
|----------|--------|-----------|------------|
| Linux | ✅ Available | [`linux/`](./linux) | GTK4 + libadwaita + Rust |
| macOS | 🚧 Coming Soon | `macos/` | - |
| Windows | 🚧 Coming Soon | `windows/` | - |

## Features

- **URL Shortening** - Create short links with custom aliases
- **Text Sharing** - Share code snippets, notes, and text content
- **File Uploads** - Upload and share files with drag & drop support
- **QR Code Generation** - Generate QR codes for any link
- **Local History** - Track all your created links, texts, and files
- **Multiple Domains** - Choose from available domains for each service

## Quick Start

### Linux

```bash
git clone https://github.com/sdotee/desktop.git
cd desktop/linux
cargo build --release
./target/release/see
```

See [linux/README.md](./linux/README.md) for detailed instructions.

## Getting an API Key

1. Visit [s.ee](https://s.ee) and create an account
2. Go to your dashboard
3. Generate an API key
4. Enter the key in the app's Preferences

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Links

- **Website**: [s.ee](https://s.ee)
- **API Documentation**: [S.EE API Docs](https://s.ee/docs/api)
- **Issues**: [GitHub Issues](https://github.com/sdotee/desktop/issues)
