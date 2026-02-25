import SwiftUI
import SwiftData

#if os(macOS)
struct MenuBarView: View {
    @Environment(\.modelContext) private var modelContext
    @State private var targetURL = ""
    @State private var isLoading = false
    @State private var resultURL: String?
    @State private var errorMessage: String?

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Image(systemName: "link")
                    .foregroundStyle(Color.accentColor)
                Text("S.EE")
                    .font(.headline)
                Spacer()
            }

            HStack {
                TextField(String(localized: "Enter URL to shorten"), text: $targetURL)
                    .textFieldStyle(.roundedBorder)
                    .onSubmit { shorten() }

                Button(action: shorten) {
                    if isLoading {
                        ProgressView()
                            .controlSize(.small)
                    } else {
                        Image(systemName: "arrow.right.circle.fill")
                    }
                }
                .disabled(targetURL.isEmpty || isLoading)
                .buttonStyle(.borderless)
            }

            if let resultURL {
                HStack {
                    Text(resultURL)
                        .font(.caption)
                        .foregroundStyle(Color.accentColor)
                        .lineLimit(1)
                    Spacer()
                    Button(String(localized: "Copy")) {
                        ClipboardService.copy(resultURL)
                    }
                    .controlSize(.small)
                }
                .padding(8)
                .background(Color.green.opacity(0.1), in: RoundedRectangle(cornerRadius: 6))
            }

            if let errorMessage {
                Text(errorMessage)
                    .font(.caption)
                    .foregroundStyle(.red)
            }

            Divider()

            Button(String(localized: "Paste from Clipboard")) {
                if let clipboard = ClipboardService.getString() {
                    targetURL = clipboard
                }
            }
            .controlSize(.small)
            .frame(maxWidth: .infinity, alignment: .leading)

            Button(String(localized: "Open S.EE")) {
                if let url = URL(string: "see://") {
                    NSWorkspace.shared.open(url)
                }
            }
            .controlSize(.small)
            .frame(maxWidth: .infinity, alignment: .leading)

            Button(String(localized: "Quit S.EE")) {
                NSApp.terminate(nil)
            }
            .controlSize(.small)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding()
        .frame(width: 320)
        .onAppear {
            // Auto-fill from clipboard if it's a URL
            if let clipboard = ClipboardService.getString(), clipboard.isValidURL {
                targetURL = clipboard
            }
        }
    }

    private func shorten() {
        guard !targetURL.isEmpty else { return }
        guard targetURL.isValidURL else {
            errorMessage = String(localized: "Please enter a valid URL")
            return
        }

        isLoading = true
        errorMessage = nil
        resultURL = nil

        let defaultDomain = UserDefaults.standard.string(forKey: Constants.defaultShortLinkDomainKey) ?? "s.ee"

        Task {
            do {
                let request = CreateShortURLRequest(
                    targetURL: targetURL,
                    domain: defaultDomain
                )
                let response: APIResponse<ShortURLResponse> = try await APIClient.shared.request(.createShortURL(request))
                if let data = response.data {
                    resultURL = data.shortURL
                    ClipboardService.copy(data.shortURL)

                    // Save to local history
                    let link = ShortLink(
                        slug: data.slug,
                        domain: defaultDomain,
                        targetURL: targetURL,
                        shortURL: data.shortURL,
                        customSlug: data.customSlug
                    )
                    modelContext.insert(link)

                    targetURL = ""
                }
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }
}
#endif
