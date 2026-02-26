import SwiftUI

struct UsageView: View {
    @State private var viewModel = UsageViewModel()

    var body: some View {
        Group {
            if !viewModel.isAvailable {
                EmptyStateView(
                    icon: "chart.bar",
                    title: String(localized: "Usage Not Available"),
                    message: String(localized: "Usage statistics are not available on this server.")
                )
            } else if let usage = viewModel.usage {
                ScrollView {
                    VStack(spacing: 16) {
                        // Storage summary
                        StorageCard(
                            fileCount: usage.fileCount,
                            storageUsageMb: usage.storageUsageMb,
                            storageUsageLimitMb: usage.storageUsageLimitMb
                        )

                        // Rate limit cards
                        LazyVGrid(columns: [
                            GridItem(.flexible()),
                            GridItem(.flexible()),
                        ], spacing: 16) {
                            UsageCard(
                                title: String(localized: "API Calls"),
                                icon: "server.rack",
                                dayCount: usage.apiCountDay,
                                dayLimit: usage.apiCountDayLimit,
                                monthCount: usage.apiCountMonth,
                                monthLimit: usage.apiCountMonthLimit
                            )

                            UsageCard(
                                title: String(localized: "Links"),
                                icon: "link",
                                dayCount: usage.linkCountDay,
                                dayLimit: usage.linkCountDayLimit,
                                monthCount: usage.linkCountMonth,
                                monthLimit: usage.linkCountMonthLimit
                            )

                            UsageCard(
                                title: String(localized: "Text Shares"),
                                icon: "doc.text",
                                dayCount: usage.textCountDay,
                                dayLimit: usage.textCountDayLimit,
                                monthCount: usage.textCountMonth,
                                monthLimit: usage.textCountMonthLimit
                            )

                            UsageCard(
                                title: String(localized: "Uploads"),
                                icon: "arrow.up.doc",
                                dayCount: usage.uploadCountDay,
                                dayLimit: usage.uploadCountDayLimit,
                                monthCount: usage.uploadCountMonth,
                                monthLimit: usage.uploadCountMonthLimit
                            )

                            UsageCard(
                                title: String(localized: "QR Codes"),
                                icon: "qrcode",
                                dayCount: usage.qrcodeCountDay,
                                dayLimit: usage.qrcodeCountDayLimit,
                                monthCount: usage.qrcodeCountMonth,
                                monthLimit: usage.qrcodeCountMonthLimit
                            )
                        }
                    }
                    .padding()
                }
            } else {
                ProgressView()
            }
        }
        .navigationTitle(String(localized: "Usage"))
        .loadingOverlay(viewModel.isLoading)
        .toolbar {
            #if os(macOS)
            ToolbarItem(placement: .automatic) {
                Button(action: { Task { await viewModel.loadUsage() } }) {
                    Label(String(localized: "Refresh"), systemImage: "arrow.clockwise")
                }
            }
            #endif
        }
        #if os(iOS)
        .refreshable {
            await viewModel.loadUsage()
        }
        #endif
        .task {
            await viewModel.loadUsage()
        }
    }
}

// MARK: - Storage Card

struct StorageCard: View {
    let fileCount: Int
    let storageUsageMb: String
    let storageUsageLimitMb: String

    private var isUnlimited: Bool {
        guard let limit = Double(storageUsageLimitMb) else { return false }
        return limit < 0
    }

    private var storageProgress: Double {
        guard let usage = Double(storageUsageMb),
              let limit = Double(storageUsageLimitMb),
              limit > 0 else { return 0 }
        return usage / limit
    }

    private func formatMb(_ value: String) -> String {
        guard let mb = Double(value) else { return value }
        if mb >= 1024 {
            return String(format: "%.1f GB", mb / 1024)
        }
        return String(format: "%.1f MB", mb)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(String(localized: "Storage"), systemImage: "externaldrive")
                .font(.headline)

            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(String(localized: "Files"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("\(fileCount)")
                        .font(.title2.weight(.semibold).monospacedDigit())
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 4) {
                    Text(String(localized: "Used"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    if isUnlimited {
                        Text("\(formatMb(storageUsageMb)) / \(String(localized: "Unlimited"))")
                            .font(.subheadline.weight(.medium).monospacedDigit())
                    } else {
                        Text("\(formatMb(storageUsageMb)) / \(formatMb(storageUsageLimitMb))")
                            .font(.subheadline.weight(.medium).monospacedDigit())
                    }
                }
            }

            if !isUnlimited {
                ProgressView(value: storageProgress)
                    .tint(storageProgress > 0.9 ? .red : storageProgress > 0.7 ? .orange : .accentColor)
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Usage Card

struct UsageCard: View {
    let title: String
    let icon: String
    let dayCount: Int
    let dayLimit: Int
    let monthCount: Int
    let monthLimit: Int

    private var isUnlimitedDay: Bool { dayLimit < 0 }
    private var isUnlimitedMonth: Bool { monthLimit < 0 }

    private var dayProgress: Double {
        guard dayLimit > 0 else { return 0 }
        return Double(dayCount) / Double(dayLimit)
    }

    private var monthProgress: Double {
        guard monthLimit > 0 else { return 0 }
        return Double(monthCount) / Double(monthLimit)
    }

    private func limitText(_ count: Int, _ limit: Int) -> String {
        if limit < 0 {
            return "\(count) / \(String(localized: "Unlimited"))"
        }
        return "\(count) / \(limit)"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(title, systemImage: icon)
                .font(.headline)

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(String(localized: "Today"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text(limitText(dayCount, dayLimit))
                        .font(.caption.monospacedDigit())
                }
                if !isUnlimitedDay {
                    ProgressView(value: dayProgress)
                        .tint(dayProgress > 0.9 ? .red : dayProgress > 0.7 ? .orange : .accentColor)
                }
            }

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(String(localized: "This Month"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text(limitText(monthCount, monthLimit))
                        .font(.caption.monospacedDigit())
                }
                if !isUnlimitedMonth {
                    ProgressView(value: monthProgress)
                        .tint(monthProgress > 0.9 ? .red : monthProgress > 0.7 ? .orange : .accentColor)
                }
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
    }
}
