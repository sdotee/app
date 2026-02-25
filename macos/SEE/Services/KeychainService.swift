import Foundation
import Security

// All methods are nonisolated to allow access from any actor context
struct KeychainService: Sendable {
    private static let service = Constants.keychainServiceName

    nonisolated static func save(key: String, value: String) throws {
        guard let data = value.data(using: .utf8) else { return }

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]

        // Delete existing item first
        SecItemDelete(query as CFDictionary)

        var addQuery = query
        addQuery[kSecValueData as String] = data
        addQuery[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlocked

        let status = SecItemAdd(addQuery as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw KeychainError.saveFailed(status)
        }
    }

    nonisolated static func load(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess, let data = result as? Data else {
            return nil
        }
        return String(data: data, encoding: .utf8)
    }

    nonisolated static func delete(key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
        SecItemDelete(query as CFDictionary)
    }

    // Convenience for API Key
    nonisolated static func getAPIKey() -> String? {
        load(key: Constants.keychainAPIKeyAccount)
    }

    nonisolated static func setAPIKey(_ value: String?) {
        if let value {
            try? save(key: Constants.keychainAPIKeyAccount, value: value)
        } else {
            delete(key: Constants.keychainAPIKeyAccount)
        }
    }
}

enum KeychainError: LocalizedError, Sendable {
    case saveFailed(OSStatus)

    var errorDescription: String? {
        switch self {
        case .saveFailed(let status):
            String(localized: "Failed to save to Keychain (status: \(status))")
        }
    }
}
