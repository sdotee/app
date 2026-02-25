import SwiftUI

struct LinkRowView: View {
    let shortURL: String
    let title: String
    let subtitle: String?
    let badge: String?
    let date: Date
    let onCopy: () -> Void
    let onEdit: (() -> Void)?
    let onDelete: () -> Void
    var extraMenuItems: (() -> AnyView)?

    init(
        shortURL: String,
        title: String,
        subtitle: String? = nil,
        badge: String? = nil,
        date: Date,
        onCopy: @escaping () -> Void,
        onEdit: (() -> Void)? = nil,
        onDelete: @escaping () -> Void,
        extraMenuItems: (() -> AnyView)? = nil
    ) {
        self.shortURL = shortURL
        self.title = title
        self.subtitle = subtitle
        self.badge = badge
        self.date = date
        self.onCopy = onCopy
        self.onEdit = onEdit
        self.onDelete = onDelete
        self.extraMenuItems = extraMenuItems
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            // Top row: link + copy + badges
            HStack(alignment: .center, spacing: 6) {
                if let url = URL(string: shortURL) {
                    Link(shortURL, destination: url)
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(1)
                } else {
                    Text(shortURL)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color.accentColor)
                        .lineLimit(1)
                }

                CopyButton(text: shortURL, font: .caption)

                Spacer()

                if let badge {
                    Text(badge)
                        .font(.caption2)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.secondary.opacity(0.12), in: Capsule())
                }

                Text(date.relativeFormatted)
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }

            // Title
            if !title.isEmpty {
                Text(title)
                    .font(.body)
                    .lineLimit(1)
            }

            // Subtitle (target URL or content preview)
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
        }
        .padding(.vertical, 4)
        .contextMenu {
            Button(String(localized: "Copy Link")) { onCopy() }

            if let onEdit {
                Button(String(localized: "Edit...")) { onEdit() }
            }

            if let extraMenuItems {
                extraMenuItems()
            }

            Divider()

            Button(String(localized: "Delete"), role: .destructive) { onDelete() }
        }
    }
}
