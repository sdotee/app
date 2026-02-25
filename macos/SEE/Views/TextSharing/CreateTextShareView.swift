import SwiftUI
import SwiftData

struct CreateTextShareView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @Bindable var viewModel: TextShareViewModel
    var editingShare: TextShare?

    private var isEditing: Bool { editingShare != nil }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField(String(localized: "Untitled"), text: $viewModel.title)
                        .textFieldStyle(.roundedBorder)

                    Picker(String(localized: "Type"), selection: $viewModel.textType) {
                        ForEach(TextType.allCases) { type in
                            Text(type.displayName).tag(type)
                        }
                    }
                    .pickerStyle(.segmented)

                    if !viewModel.domains.isEmpty {
                        DomainPicker(
                            title: String(localized: "Domain"),
                            selection: $viewModel.selectedDomain,
                            domains: viewModel.domains
                        )
                    }

                    TextField(String(localized: "Custom Slug (optional)"), text: $viewModel.customSlug)
                        .textFieldStyle(.roundedBorder)
                        .disabled(isEditing)
                } header: {
                    Text(String(localized: "Details"))
                }

                Section {
                    TextEditor(text: $viewModel.content)
                        .font(viewModel.textType == .sourceCode ? .system(.body, design: .monospaced) : .body)
                        .frame(minHeight: 200)
                } header: {
                    Text(String(localized: "Content"))
                }

                if !isEditing {
                    Section {
                        SecureField(String(localized: "Password (optional)"), text: $viewModel.password)
                            .textFieldStyle(.roundedBorder)

                        Toggle(String(localized: "Set Expiration"), isOn: $viewModel.enableExpiry)
                        if viewModel.enableExpiry {
                            DatePicker(
                                String(localized: "Expire At"),
                                selection: Binding(
                                    get: { viewModel.expireAt ?? Date().addingTimeInterval(86400) },
                                    set: { viewModel.expireAt = $0 }
                                ),
                                in: Date()...,
                                displayedComponents: [.date, .hourAndMinute]
                            )
                        }
                    } header: {
                        Text(String(localized: "Options"))
                    }

                    if !viewModel.tags.isEmpty {
                        Section {
                            TagSelector(
                                tags: viewModel.tags,
                                selectedTagIDs: $viewModel.selectedTagIDs,
                                maxSelection: 5
                            )
                        } header: {
                            Text(String(localized: "Tags"))
                        }
                    }
                }
            }
            .formStyle(.grouped)
            .navigationTitle(isEditing ? String(localized: "Edit Text Share") : String(localized: "New Text Share"))
            #if os(macOS)
            .frame(minWidth: 500, minHeight: 500)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "Cancel")) { dismiss() }
                        .keyboardShortcut(.cancelAction)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isEditing ? String(localized: "Save") : String(localized: "Create")) {
                        Task {
                            let success: Bool
                            if let share = editingShare {
                                success = await viewModel.updateTextShare(share, context: modelContext)
                            } else {
                                success = await viewModel.createTextShare(context: modelContext)
                            }
                            if success { dismiss() }
                        }
                    }
                    .disabled(viewModel.content.isEmpty || viewModel.isLoading)
                    .keyboardShortcut(.defaultAction)
                }
            }
            .loadingOverlay(viewModel.isLoading)
            .toast(message: $viewModel.successMessage)
            .toast(message: $viewModel.errorMessage, isError: true)
        }
        .task {
            if isEditing, let share = editingShare {
                viewModel.populateForm(from: share)
            }
            await viewModel.loadDomains()
            await viewModel.loadTags()
        }
    }
}
