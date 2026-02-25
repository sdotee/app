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
                            title: String(localized: "QR Codes"),
                            icon: "qrcode",
                            dayCount: usage.qrcodeCountDay,
                            dayLimit: usage.qrcodeCountDayLimit,
                            monthCount: usage.qrcodeCountMonth,
                            monthLimit: usage.qrcodeCountMonthLimit
                        )
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

struct UsageCard: View {
    let title: String
    let icon: String
    let dayCount: Int
    let dayLimit: Int
    let monthCount: Int
    let monthLimit: Int

    private var dayProgress: Double {
        guard dayLimit > 0 else { return 0 }
        return Double(dayCount) / Double(dayLimit)
    }

    private var monthProgress: Double {
        guard monthLimit > 0 else { return 0 }
        return Double(monthCount) / Double(monthLimit)
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
                    Text("\(dayCount) / \(dayLimit)")
                        .font(.caption.monospacedDigit())
                }
                ProgressView(value: dayProgress)
                    .tint(dayProgress > 0.9 ? .red : dayProgress > 0.7 ? .orange : .accentColor)
            }

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(String(localized: "This Month"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text("\(monthCount) / \(monthLimit)")
                        .font(.caption.monospacedDigit())
                }
                ProgressView(value: monthProgress)
                    .tint(monthProgress > 0.9 ? .red : monthProgress > 0.7 ? .orange : .accentColor)
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
    }
}
