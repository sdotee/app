import SwiftUI

struct TagSelector: View {
    let tags: [Tag]
    @Binding var selectedTagIDs: [Int]
    var maxSelection: Int?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(String(localized: "Tags"))
                .font(.subheadline.weight(.medium))

            if tags.isEmpty {
                Text(String(localized: "No tags available"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } else {
                FlowLayout(spacing: 6) {
                    ForEach(tags) { tag in
                        TagChip(
                            tag: tag,
                            isSelected: selectedTagIDs.contains(tag.id),
                            action: { toggle(tag) }
                        )
                    }
                }
            }

            if let maxSelection {
                Text(String(localized: "\(selectedTagIDs.count)/\(maxSelection) selected"))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func toggle(_ tag: Tag) {
        if selectedTagIDs.contains(tag.id) {
            selectedTagIDs.removeAll { $0 == tag.id }
        } else {
            if let maxSelection, selectedTagIDs.count >= maxSelection {
                return
            }
            selectedTagIDs.append(tag.id)
        }
    }
}

struct TagChip: View {
    let tag: Tag
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(tag.name)
                .font(.caption)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(isSelected ? Color.accentColor.opacity(0.15) : Color.secondary.opacity(0.1))
                .foregroundStyle(isSelected ? Color.accentColor : .primary)
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .strokeBorder(isSelected ? Color.accentColor : .clear, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Flow Layout

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = layout(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = layout(proposal: proposal, subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y), proposal: .unspecified)
        }
    }

    private func layout(proposal: ProposedViewSize, subviews: Subviews) -> LayoutResult {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var currentX: CGFloat = 0
        var currentY: CGFloat = 0
        var lineHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if currentX + size.width > maxWidth, currentX > 0 {
                currentX = 0
                currentY += lineHeight + spacing
                lineHeight = 0
            }
            positions.append(CGPoint(x: currentX, y: currentY))
            lineHeight = max(lineHeight, size.height)
            currentX += size.width + spacing
        }

        return LayoutResult(
            positions: positions,
            size: CGSize(width: maxWidth, height: currentY + lineHeight)
        )
    }

    private struct LayoutResult {
        var positions: [CGPoint]
        var size: CGSize
    }
}
