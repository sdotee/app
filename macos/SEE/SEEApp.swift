import SwiftUI
import SwiftData

@main
struct SEEApp: App {
    var sharedModelContainer: ModelContainer = {
        let schema = Schema([
            ShortLink.self,
            TextShare.self,
            UploadedFile.self,
        ])
        let modelConfiguration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
        do {
            return try ModelContainer(for: schema, configurations: [modelConfiguration])
        } catch {
            fatalError("Could not create ModelContainer: \(error)")
        }
    }()

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .modelContainer(sharedModelContainer)
        #if os(macOS)
        .windowToolbarStyle(.unified)
        .defaultSize(width: 900, height: 600)
        .commands {
            CommandGroup(after: .newItem) {
                Button(String(localized: "New Short Link")) {
                    NotificationCenter.default.post(name: .createShortLink, object: nil)
                }
                .keyboardShortcut("n", modifiers: [.command])

                Button(String(localized: "New Text Share")) {
                    NotificationCenter.default.post(name: .createTextShare, object: nil)
                }
                .keyboardShortcut("n", modifiers: [.command, .shift])
            }
        }
        #endif

        #if os(macOS)
        Settings {
            SettingsView()
                .modelContainer(sharedModelContainer)
        }

        MenuBarExtra("S.EE", systemImage: "link") {
            MenuBarView()
                .modelContainer(sharedModelContainer)
        }
        .menuBarExtraStyle(.window)
        #endif
    }
}

// MARK: - Notification Names

extension Notification.Name {
    static let createShortLink = Notification.Name("createShortLink")
    static let createTextShare = Notification.Name("createTextShare")
}
