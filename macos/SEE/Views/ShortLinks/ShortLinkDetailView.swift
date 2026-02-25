import SwiftUI

struct ShortLinkDetailView: View {
    let link: ShortLink

    var body: some View {
        List {
            Section {
                LabeledContent(String(localized: "Short URL")) {
                    HStack {
                        Text(link.shortURL)
                            .foregroundStyle(Color.accentColor)
                        Button(action: { ClipboardService.copy(link.shortURL) }) {
                            Image(systemName: "doc.on.doc")
                        }
                        .buttonStyle(.borderless)
                    }
                }

                LabeledContent(String(localized: "Target URL")) {
                    HStack {
                        Text(link.targetURL)
                            .lineLimit(2)
                        Button(action: { ClipboardService.copy(link.targetURL) }) {
                            Image(systemName: "doc.on.doc")
                        }
                        .buttonStyle(.borderless)
                    }
                }

                if !link.title.isEmpty {
                    LabeledContent(String(localized: "Title"), value: link.title)
                }

                LabeledContent(String(localized: "Domain"), value: link.domain)
                LabeledContent(String(localized: "Slug"), value: link.slug)

                if let customSlug = link.customSlug {
                    LabeledContent(String(localized: "Custom Slug"), value: customSlug)
                }
            }

            Section {
                LabeledContent(String(localized: "Created"), value: link.createdAt.shortFormatted)

                if link.hasPassword {
                    Label(String(localized: "Password Protected"), systemImage: "lock.fill")
                }

                if let expireAt = link.expireAt {
                    LabeledContent(String(localized: "Expires"), value: expireAt.shortFormatted)
                }
            }
        }
        .navigationTitle(link.shortURL)
    }
}
