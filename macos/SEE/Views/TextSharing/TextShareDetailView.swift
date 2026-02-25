import SwiftUI

struct TextShareDetailView: View {
    let share: TextShare

    var body: some View {
        List {
            Section {
                LabeledContent(String(localized: "Title"), value: share.title)

                LabeledContent(String(localized: "Link")) {
                    HStack {
                        Text(share.shortURL)
                            .foregroundStyle(Color.accentColor)
                        Button(action: { ClipboardService.copy(share.shortURL) }) {
                            Image(systemName: "doc.on.doc")
                        }
                        .buttonStyle(.borderless)
                    }
                }

                LabeledContent(String(localized: "Type")) {
                    Text(TextType(rawValue: share.textType)?.displayName ?? share.textType)
                }

                LabeledContent(String(localized: "Domain"), value: share.domain)
                LabeledContent(String(localized: "Created"), value: share.createdAt.shortFormatted)
            }

            Section {
                Text(share.content)
                    .font(share.textType == TextType.sourceCode.rawValue
                          ? .system(.body, design: .monospaced) : .body)
                    .textSelection(.enabled)
            } header: {
                Text(String(localized: "Content"))
            }
        }
        .navigationTitle(share.title)
    }
}
