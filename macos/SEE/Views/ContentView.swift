import SwiftUI
import SwiftData

enum SidebarItem: String, CaseIterable, Identifiable {
    case shortLinks
    case textSharing
    case fileUpload
    case tags
    case usage
    case settings

    var id: String { rawValue }

    var title: String {
        switch self {
        case .shortLinks: String(localized: "Short Links")
        case .textSharing: String(localized: "Text Sharing")
        case .fileUpload: String(localized: "File Upload")
        case .tags: String(localized: "Tags")
        case .usage: String(localized: "Usage")
        case .settings: String(localized: "Settings")
        }
    }

    var icon: String {
        switch self {
        case .shortLinks: "link"
        case .textSharing: "doc.text"
        case .fileUpload: "arrow.up.doc"
        case .tags: "tag"
        case .usage: "chart.bar"
        case .settings: "gearshape"
        }
    }
}

struct ContentView: View {
    @State private var selectedItem: SidebarItem? = .shortLinks
    @State private var hasAPIKey = KeychainService.getAPIKey() != nil

    var body: some View {
        if hasAPIKey {
            mainContent
        } else {
            OnboardingView(hasAPIKey: $hasAPIKey)
        }
    }

    @ViewBuilder
    private var mainContent: some View {
        #if os(macOS)
        NavigationSplitView {
            List(SidebarItem.allCases, selection: $selectedItem) { item in
                Label(item.title, systemImage: item.icon)
                    .tag(item)
            }
            .listStyle(.sidebar)
            .navigationSplitViewColumnWidth(min: 180, ideal: 200, max: 280)
        } detail: {
            detailView(for: selectedItem)
        }
        .frame(minWidth: 700, minHeight: 450)
        #else
        TabView {
            Tab(String(localized: "Short Links"), systemImage: "link") {
                NavigationStack {
                    ShortLinkListView()
                }
            }
            Tab(String(localized: "Text"), systemImage: "doc.text") {
                NavigationStack {
                    TextShareListView()
                }
            }
            Tab(String(localized: "Upload"), systemImage: "arrow.up.doc") {
                NavigationStack {
                    FileUploadView()
                }
            }
            Tab(String(localized: "More"), systemImage: "ellipsis") {
                NavigationStack {
                    MoreView(hasAPIKey: $hasAPIKey)
                }
            }
        }
        #endif
    }

    @ViewBuilder
    private func detailView(for item: SidebarItem?) -> some View {
        switch item {
        case .shortLinks:
            ShortLinkListView()
        case .textSharing:
            TextShareListView()
        case .fileUpload:
            FileUploadView()
        case .tags:
            TagsView()
        case .usage:
            UsageView()
        case .settings:
            SettingsView(hasAPIKey: $hasAPIKey)
        case nil:
            Text(String(localized: "Select an item from the sidebar"))
                .foregroundStyle(.secondary)
        }
    }
}

// MARK: - Onboarding View

struct OnboardingView: View {
    @Binding var hasAPIKey: Bool
    @State private var apiKey = ""
    @State private var baseURL = UserDefaults.standard.string(forKey: Constants.baseURLKey) ?? Constants.defaultBaseURL
    @State private var isValidating = false
    @State private var errorMessage: String?
    @State private var showSuccess = false

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Spacer(minLength: 40)

                Image(systemName: "link.circle.fill")
                    .font(.system(size: 64))
                    .foregroundStyle(Color.accentColor)

                Text("S.EE")
                    .font(.largeTitle.bold())

                Text(String(localized: "URL Shortener, Text Sharing & File Hosting"))
                    .font(.headline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)

                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(String(localized: "Base URL"))
                            .font(.subheadline.weight(.medium))
                        TextField("https://s.ee/api/v1/", text: $baseURL)
                            .textFieldStyle(.roundedBorder)
                            #if os(iOS)
                            .keyboardType(.URL)
                            .textInputAutocapitalization(.never)
                            #endif
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text(String(localized: "API Key"))
                            .font(.subheadline.weight(.medium))
                        HStack {
                            SecureField(String(localized: "Enter your API key"), text: $apiKey)
                                .textFieldStyle(.roundedBorder)
                            Button(String(localized: "Paste")) {
                                if let clipboard = ClipboardService.getString() {
                                    apiKey = clipboard
                                }
                            }
                        }
                    }

                    if let errorMessage {
                        Text(errorMessage)
                            .font(.caption)
                            .foregroundStyle(.red)
                    }

                    if showSuccess {
                        Label(String(localized: "API key verified successfully!"), systemImage: "checkmark.circle.fill")
                            .font(.caption)
                            .foregroundStyle(.green)
                    }
                }
                .frame(maxWidth: 400)

                Button(action: validate) {
                    if isValidating {
                        ProgressView()
                            .controlSize(.small)
                    } else {
                        Text(String(localized: "Verify & Continue"))
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(apiKey.isEmpty || isValidating)
                .keyboardShortcut(.defaultAction)

                Spacer(minLength: 40)
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 24)
        }
        #if os(macOS)
        .frame(minWidth: 500, minHeight: 400)
        #endif
    }

    private func validate() {
        isValidating = true
        errorMessage = nil
        showSuccess = false

        // Save base URL
        UserDefaults.standard.set(baseURL, forKey: Constants.baseURLKey)
        // Temporarily save API key for validation
        KeychainService.setAPIKey(apiKey)

        Task {
            do {
                let _ = try await APIClient.shared.validateAPIKey()
                showSuccess = true
                try? await Task.sleep(for: .seconds(0.5))
                hasAPIKey = true
            } catch {
                KeychainService.setAPIKey(nil)
                errorMessage = error.localizedDescription
            }
            isValidating = false
        }
    }
}

// MARK: - iOS More View

#if os(iOS)
struct MoreView: View {
    @Binding var hasAPIKey: Bool

    var body: some View {
        List {
            NavigationLink {
                TagsView()
            } label: {
                Label(String(localized: "Tags"), systemImage: "tag")
            }

            NavigationLink {
                UsageView()
            } label: {
                Label(String(localized: "Usage"), systemImage: "chart.bar")
            }

            NavigationLink {
                SettingsView(hasAPIKey: $hasAPIKey)
            } label: {
                Label(String(localized: "Settings"), systemImage: "gearshape")
            }
        }
        .navigationTitle(String(localized: "More"))
    }
}
#endif
