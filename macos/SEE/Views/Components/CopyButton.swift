import SwiftUI

struct CopyButton: View {
    let text: String
    var font: Font = .caption2
    @State private var copied = false

    var body: some View {
        Button(action: copy) {
            Image(systemName: copied ? "checkmark" : "doc.on.doc")
                .font(font)
                .foregroundStyle(copied ? .green : .secondary)
                .contentTransition(.symbolEffect(.replace))
        }
        .buttonStyle(.borderless)
        .help(copied ? String(localized: "Copied!") : String(localized: "Copy link"))
    }

    private func copy() {
        ClipboardService.copy(text)
        copied = true
        Task {
            try? await Task.sleep(for: .seconds(1.5))
            withAnimation { copied = false }
        }
    }
}
