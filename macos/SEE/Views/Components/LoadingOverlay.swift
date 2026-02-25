import SwiftUI

struct LoadingOverlay: ViewModifier {
    let isLoading: Bool

    func body(content: Content) -> some View {
        content
            .overlay {
                if isLoading {
                    ZStack {
                        Color.black.opacity(0.1)
                        ProgressView()
                            .controlSize(.large)
                    }
                }
            }
            .allowsHitTesting(!isLoading)
    }
}

extension View {
    func loadingOverlay(_ isLoading: Bool) -> some View {
        modifier(LoadingOverlay(isLoading: isLoading))
    }
}

// MARK: - Toast

struct ToastModifier: ViewModifier {
    @Binding var message: String?
    var isError: Bool = false

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if let message {
                    HStack(spacing: 8) {
                        Image(systemName: isError ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
                            .foregroundStyle(isError ? .red : .green)
                        Text(message)
                            .font(.subheadline)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 10))
                    .shadow(color: .black.opacity(0.1), radius: 8, y: 4)
                    #if os(iOS)
                    .padding(.bottom, 60)
                    #else
                    .padding(.bottom, 20)
                    #endif
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onAppear {
                        Task { @MainActor in
                            try? await Task.sleep(for: .seconds(2.5))
                            withAnimation {
                                self.message = nil
                            }
                        }
                    }
                }
            }
            .animation(.easeInOut, value: message)
    }
}

extension View {
    func toast(message: Binding<String?>, isError: Bool = false) -> some View {
        modifier(ToastModifier(message: message, isError: isError))
    }
}
