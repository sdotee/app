import SwiftUI
import SwiftData

struct SettingsView: View {
    @Environment(\.modelContext) private var modelContext
    @Binding var hasAPIKey: Bool
    @State private var baseURL = UserDefaults.standard.string(forKey: Constants.baseURLKey) ?? Constants.defaultBaseURL
    @State private var apiKey = KeychainService.getAPIKey() ?? ""
    @State private var isValidating = false
    @State private var validationResult: ValidationResult?
    @State private var shortLinkDomains: [String] = []
    @State private var textDomains: [String] = []
    @State private var fileDomains: [String] = []
    @State private var defaultShortLinkDomain = UserDefaults.standard.string(forKey: Constants.defaultShortLinkDomainKey) ?? ""
    @State private var defaultTextDomain = UserDefaults.standard.string(forKey: Constants.defaultTextDomainKey) ?? ""
    @State private var defaultFileDomain = UserDefaults.standard.string(forKey: Constants.defaultFileDomainKey) ?? ""
    @State private var isLoadingDomains = false
    @State private var defaultFileLinkDisplay: LinkDisplayType = {
        if let saved = UserDefaults.standard.string(forKey: Constants.defaultFileLinkDisplayKey),
           let type = LinkDisplayType(rawValue: saved) {
            return type
        }
        return .sharePage
    }()
    @State private var pasteImageFormat: PasteImageFormat = {
        if let saved = UserDefaults.standard.string(forKey: Constants.pasteImageFormatKey),
           let fmt = PasteImageFormat(rawValue: saved) {
            return fmt
        }
        return .webp
    }()
    @State private var isClearingCache = false
    @State private var showClearHistoryAlert = false
    @State private var cacheCleared = false

    enum ValidationResult {
        case success
        case failure(String)
    }

    init(hasAPIKey: Binding<Bool>? = nil) {
        _hasAPIKey = hasAPIKey ?? .constant(true)
    }

    var body: some View {
        Form {
            Section {
                #if os(macOS)
                VStack(alignment: .leading, spacing: 8) {
                    Text(String(localized: "Base URL"))
                        .font(.subheadline.weight(.medium))
                    TextField("https://s.ee/api/v1/", text: $baseURL)
                        .textFieldStyle(.roundedBorder)
                        .onChange(of: baseURL) {
                            UserDefaults.standard.set(baseURL, forKey: Constants.baseURLKey)
                        }
                }
                .padding(.vertical, 4)

                VStack(alignment: .leading, spacing: 8) {
                    Text(String(localized: "API Key"))
                        .font(.subheadline.weight(.medium))
                    SecureField(String(localized: "Enter your API key"), text: $apiKey)
                        .textFieldStyle(.roundedBorder)
                    HStack(spacing: 8) {
                        Button(String(localized: "Paste from Clipboard")) {
                            if let clipboard = ClipboardService.getString() {
                                apiKey = clipboard
                            }
                        }

                        Button(action: validateKey) {
                            if isValidating {
                                ProgressView()
                                    .controlSize(.small)
                            } else {
                                Text(String(localized: "Verify API Key"))
                            }
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(apiKey.isEmpty || isValidating)

                        Spacer()
                    }
                }
                .padding(.vertical, 4)
                #else
                TextField(String(localized: "Base URL"), text: $baseURL)
                    .keyboardType(.URL)
                    .textInputAutocapitalization(.never)
                    .onChange(of: baseURL) {
                        UserDefaults.standard.set(baseURL, forKey: Constants.baseURLKey)
                    }

                SecureField(String(localized: "API Key"), text: $apiKey)

                Button(String(localized: "Paste from Clipboard")) {
                    if let clipboard = ClipboardService.getString() {
                        apiKey = clipboard
                    }
                }

                Button(action: validateKey) {
                    HStack {
                        Text(String(localized: "Verify API Key"))
                        Spacer()
                        if isValidating {
                            ProgressView()
                                .controlSize(.small)
                        }
                    }
                }
                .disabled(apiKey.isEmpty || isValidating)
                #endif

                if let validationResult {
                    switch validationResult {
                    case .success:
                        Label(
                            String(localized: "API key verified successfully!"),
                            systemImage: "checkmark.circle.fill"
                        )
                        .foregroundStyle(.green)
                    case .failure(let message):
                        Label(message, systemImage: "xmark.circle.fill")
                            .foregroundStyle(.red)
                    }
                }
            } header: {
                Text(String(localized: "API Configuration"))
            }

            Section {
                if isLoadingDomains {
                    HStack {
                        ProgressView()
                            .controlSize(.small)
                        Text(String(localized: "Loading domains..."))
                            .foregroundStyle(.secondary)
                    }
                } else if shortLinkDomains.isEmpty && textDomains.isEmpty && fileDomains.isEmpty {
                    Text(String(localized: "No domains available. Verify your API key first."))
                        .foregroundStyle(.secondary)
                } else {
                    if !shortLinkDomains.isEmpty {
                        DomainPicker(
                            title: String(localized: "Short Links"),
                            selection: $defaultShortLinkDomain,
                            domains: shortLinkDomains
                        )
                        .onChange(of: defaultShortLinkDomain) {
                            UserDefaults.standard.set(defaultShortLinkDomain, forKey: Constants.defaultShortLinkDomainKey)
                        }
                    }

                    if !textDomains.isEmpty {
                        DomainPicker(
                            title: String(localized: "Text Sharing"),
                            selection: $defaultTextDomain,
                            domains: textDomains
                        )
                        .onChange(of: defaultTextDomain) {
                            UserDefaults.standard.set(defaultTextDomain, forKey: Constants.defaultTextDomainKey)
                        }
                    }

                    if !fileDomains.isEmpty {
                        DomainPicker(
                            title: String(localized: "File Upload"),
                            selection: $defaultFileDomain,
                            domains: fileDomains
                        )
                        .onChange(of: defaultFileDomain) {
                            UserDefaults.standard.set(defaultFileDomain, forKey: Constants.defaultFileDomainKey)
                        }
                    }
                }
            } header: {
                Text(String(localized: "Default Domains"))
            }

            Section {
                Picker(String(localized: "File Upload Link Format"), selection: $defaultFileLinkDisplay) {
                    ForEach(LinkDisplayType.allCases) { type in
                        Text(type.rawValue).tag(type)
                    }
                }
                .onChange(of: defaultFileLinkDisplay) {
                    UserDefaults.standard.set(defaultFileLinkDisplay.rawValue, forKey: Constants.defaultFileLinkDisplayKey)
                }

                Picker(String(localized: "Paste Image Format"), selection: $pasteImageFormat) {
                    ForEach(PasteImageFormat.allCases) { fmt in
                        Text(fmt.rawValue).tag(fmt)
                    }
                }
                .onChange(of: pasteImageFormat) {
                    UserDefaults.standard.set(pasteImageFormat.rawValue, forKey: Constants.pasteImageFormatKey)
                }
            } header: {
                Text(String(localized: "File Upload"))
            }

            Section {
                LabeledContent(String(localized: "Version")) {
                    Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0")
                }

                Link(destination: Constants.websiteURL) {
                    LabeledContent(String(localized: "Website")) {
                        Text("s.ee")
                            .foregroundStyle(Color.accentColor)
                    }
                }
            } header: {
                Text(String(localized: "About"))
            }

            Section {
                Button(action: clearThumbnailCache) {
                    HStack {
                        Text(String(localized: "Clear Thumbnail Cache"))
                        Spacer()
                        if isClearingCache {
                            ProgressView()
                                .controlSize(.small)
                        } else if cacheCleared {
                            Image(systemName: "checkmark")
                                .foregroundStyle(.green)
                        }
                    }
                }
                .disabled(isClearingCache)
            } header: {
                Text(String(localized: "Cache"))
            }

            Section {
                Button(role: .destructive, action: { showClearHistoryAlert = true }) {
                    Text(String(localized: "Clear Local History"))
                }
            } header: {
                Text(String(localized: "Data"))
            } footer: {
                Text(String(localized: "Local history only stores records on this device."))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Section {
                Button(String(localized: "Sign Out"), role: .destructive) {
                    KeychainService.setAPIKey(nil)
                    hasAPIKey = false
                }
            }
        }
        .formStyle(.grouped)
        .navigationTitle(String(localized: "Settings"))
        .alert(String(localized: "Clear Local History?"), isPresented: $showClearHistoryAlert) {
            Button(String(localized: "Cancel"), role: .cancel) {}
            Button(String(localized: "Clear"), role: .destructive) {
                clearLocalHistory()
            }
        } message: {
            Text(String(localized: "This will only remove records stored on this device. Your files, links, and text shares on the server will not be affected. To delete data from the server, please visit s.ee."))
        }
        .task {
            await loadAllDomains()
        }
    }

    private func clearLocalHistory() {
        do {
            try modelContext.delete(model: ShortLink.self)
            try modelContext.delete(model: TextShare.self)
            try modelContext.delete(model: UploadedFile.self)
            try modelContext.save()
        } catch {
            // Silently handle
        }
    }

    private func loadAllDomains() async {
        guard KeychainService.getAPIKey() != nil else { return }
        isLoadingDomains = true
        defer { isLoadingDomains = false }

        // Fetch all three domain types
        do {
            let response: APIResponse<DomainsResponse> = try await APIClient.shared.request(.getDomains)
            shortLinkDomains = response.data?.domains ?? []
            if defaultShortLinkDomain.isEmpty, let first = shortLinkDomains.first {
                defaultShortLinkDomain = first
            }
        } catch { }

        do {
            let response: APIResponse<DomainsResponse> = try await APIClient.shared.request(.getTextDomains)
            textDomains = response.data?.domains ?? []
            if defaultTextDomain.isEmpty, let first = textDomains.first {
                defaultTextDomain = first
            }
        } catch { }

        do {
            let response: APIResponse<DomainsResponse> = try await APIClient.shared.request(.getFileDomains)
            fileDomains = response.data?.domains ?? []
            if defaultFileDomain.isEmpty, let first = fileDomains.first {
                defaultFileDomain = first
            }
        } catch { }
    }

    private func clearThumbnailCache() {
        isClearingCache = true
        cacheCleared = false
        Task {
            await ThumbnailService.shared.clearCache()
            isClearingCache = false
            cacheCleared = true
            // Reset checkmark after 2 seconds
            try? await Task.sleep(for: .seconds(2))
            cacheCleared = false
        }
    }

    private func validateKey() {
        isValidating = true
        validationResult = nil

        UserDefaults.standard.set(baseURL, forKey: Constants.baseURLKey)
        KeychainService.setAPIKey(apiKey)

        Task {
            do {
                let _ = try await APIClient.shared.validateAPIKey()
                validationResult = .success
                await loadAllDomains()
            } catch {
                validationResult = .failure(error.localizedDescription)
            }
            isValidating = false
        }
    }
}
