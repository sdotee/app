import SwiftUI

struct DomainPicker: View {
    let title: String
    @Binding var selection: String
    let domains: [String]

    var body: some View {
        Picker(title, selection: $selection) {
            ForEach(domains, id: \.self) { domain in
                Text(domain).tag(domain)
            }
        }
    }
}
