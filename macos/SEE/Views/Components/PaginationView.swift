import SwiftUI

struct PaginationView: View {
    let currentPage: Int
    let totalPages: Int
    let onPageChange: (Int) -> Void

    var body: some View {
        if totalPages > 1 {
            HStack(spacing: 12) {
                Button(action: { onPageChange(currentPage - 1) }) {
                    Image(systemName: "chevron.left")
                }
                .disabled(currentPage <= 1)

                Text(String(localized: "\(currentPage) / \(totalPages)"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()

                Button(action: { onPageChange(currentPage + 1) }) {
                    Image(systemName: "chevron.right")
                }
                .disabled(currentPage >= totalPages)
            }
            .buttonStyle(.borderless)
        }
    }
}

/// Helper to compute paginated slices.
enum Pagination {
    static let pageSize = 50

    static func totalPages(for count: Int) -> Int {
        max(1, Int(ceil(Double(count) / Double(pageSize))))
    }

    static func page<T>(_ items: [T], page: Int) -> [T] {
        let start = (page - 1) * pageSize
        let end = min(start + pageSize, items.count)
        guard start < items.count else { return [] }
        return Array(items[start..<end])
    }
}
