import Foundation
import os

// MARK: - HTTP Method

enum HTTPMethod: String, Sendable {
    case get = "GET"
    case post = "POST"
    case put = "PUT"
    case delete = "DELETE"
}

// MARK: - API Error

enum APIError: LocalizedError, Sendable {
    case unauthorized
    case badRequest(String)
    case serverError
    case networkError(String)
    case decodingError(String)
    case invalidBaseURL
    case noAPIKey
    case unexpectedStatusCode(Int, String?)

    var errorDescription: String? {
        switch self {
        case .unauthorized:
            String(localized: "Unauthorized. Please check your API key.")
        case .badRequest(let message):
            message
        case .serverError:
            String(localized: "Server error. Please try again later.")
        case .networkError(let message):
            message
        case .decodingError(let message):
            String(localized: "Failed to parse response: \(message)")
        case .invalidBaseURL:
            String(localized: "Invalid Base URL. Please check your settings.")
        case .noAPIKey:
            String(localized: "No API key configured. Please add your API key in Settings.")
        case .unexpectedStatusCode(let code, let message):
            message ?? String(localized: "Unexpected response (code: \(code))")
        }
    }
}

// MARK: - Endpoint

enum Endpoint: Sendable {
    case getDomains
    case createShortURL(CreateShortURLRequest)
    case updateShortURL(UpdateShortURLRequest)
    case deleteShortURL(DeleteShortURLRequest)
    case getLinkVisitStat(domain: String, slug: String, period: String)
    case getTextDomains
    case createText(CreateTextRequest)
    case updateText(UpdateTextRequest)
    case deleteText(DeleteTextRequest)
    case getFileDomains
    case uploadFile
    case deleteFile(hash: String)
    case getTags
    case getUsage

    var path: String {
        switch self {
        case .getDomains: "domains"
        case .createShortURL, .updateShortURL, .deleteShortURL: "shorten"
        case .getLinkVisitStat: "link/visit-stat"
        case .getTextDomains: "text/domains"
        case .createText, .updateText, .deleteText: "text"
        case .getFileDomains: "file/domains"
        case .uploadFile: "file/upload"
        case .deleteFile(let hash): "file/delete/\(hash)"
        case .getTags: "tags"
        case .getUsage: "usage"
        }
    }

    var method: HTTPMethod {
        switch self {
        case .getDomains, .getLinkVisitStat, .getTextDomains, .getFileDomains,
             .deleteFile, .getTags, .getUsage:
            .get
        case .createShortURL, .createText, .uploadFile:
            .post
        case .updateShortURL, .updateText:
            .put
        case .deleteShortURL, .deleteText:
            .delete
        }
    }

    var body: (any Encodable & Sendable)? {
        switch self {
        case .createShortURL(let req): req
        case .updateShortURL(let req): req
        case .deleteShortURL(let req): req
        case .createText(let req): req
        case .updateText(let req): req
        case .deleteText(let req): req
        default: nil
        }
    }

    var queryItems: [URLQueryItem]? {
        switch self {
        case .getLinkVisitStat(let domain, let slug, let period):
            [
                URLQueryItem(name: "domain", value: domain),
                URLQueryItem(name: "slug", value: slug),
                URLQueryItem(name: "period", value: period),
            ]
        default:
            nil
        }
    }

    var requiresAuth: Bool { true }
}

// MARK: - API Client

actor APIClient {
    static let shared = APIClient()

    private let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        return encoder
    }()

    private let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        return decoder
    }()

    private func resolveBaseURL() throws -> URL {
        let stored = _readBaseURL()
        guard let url = URL(string: stored) else {
            throw APIError.invalidBaseURL
        }
        return url
    }

    // Use nonisolated to read from UserDefaults without actor isolation issues
    private nonisolated func _readBaseURL() -> String {
        UserDefaults.standard.string(forKey: Constants.baseURLKey) ?? Constants.defaultBaseURL
    }

    private func apiKey() throws -> String {
        guard let key = KeychainService.getAPIKey(), !key.isEmpty else {
            throw APIError.noAPIKey
        }
        return key
    }

    // MARK: - Generic Request

    func request<T: Decodable>(_ endpoint: Endpoint) async throws -> APIResponse<T> {
        let base = try resolveBaseURL()
        guard var components = URLComponents(url: base.appendingPathComponent(endpoint.path), resolvingAgainstBaseURL: true) else {
            throw APIError.invalidBaseURL
        }

        if let queryItems = endpoint.queryItems {
            components.queryItems = queryItems
        }

        guard let url = components.url else {
            throw APIError.invalidBaseURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = endpoint.method.rawValue

        if endpoint.requiresAuth {
            let key = try apiKey()
            request.setValue(key, forHTTPHeaderField: "Authorization")
        }

        if let body = endpoint.body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try encoder.encode(body)
        }

        let (data, response) = try await performRequest(request)
        return try handleResponse(data: data, response: response)
    }

    // MARK: - Request (ignore response body)

    /// Perform a request and only check the HTTP status code, without parsing the response body.
    func requestNoBody(_ endpoint: Endpoint) async throws {
        let base = try resolveBaseURL()
        guard var components = URLComponents(url: base.appendingPathComponent(endpoint.path), resolvingAgainstBaseURL: true) else {
            throw APIError.invalidBaseURL
        }

        if let queryItems = endpoint.queryItems {
            components.queryItems = queryItems
        }

        guard let url = components.url else {
            throw APIError.invalidBaseURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = endpoint.method.rawValue

        if endpoint.requiresAuth {
            let key = try apiKey()
            request.setValue(key, forHTTPHeaderField: "Authorization")
        }

        if let body = endpoint.body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try encoder.encode(body)
        }

        let (data, response) = try await performRequest(request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.networkError(String(localized: "Invalid response"))
        }
        if httpResponse.statusCode == 401 {
            throw APIError.unauthorized
        }

        // Try to parse API-level error from response body
        if let apiError = try? decoder.decode(APIResponse<EmptyData>.self, from: data) {
            if apiError.code == 401 {
                throw APIError.unauthorized
            }
            if apiError.code != 200 {
                throw APIError.unexpectedStatusCode(apiError.code, apiError.message)
            }
            return // API says 200, success
        }

        // Fallback: check HTTP status
        if httpResponse.statusCode >= 400 {
            throw APIError.unexpectedStatusCode(httpResponse.statusCode, nil)
        }
    }

    // MARK: - File Upload with Progress

    func uploadFile(
        _ fileData: Data,
        filename: String,
        domain: String?,
        progress: @Sendable @escaping (Double) -> Void
    ) async throws -> UploadFileResponse {
        let base = try resolveBaseURL()
        let url = base.appendingPathComponent("file/upload")

        let boundary = UUID().uuidString
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(try apiKey(), forHTTPHeaderField: "Authorization")
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()

        // File field
        body.append("--\(boundary)\r\n")
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"\(filename)\"\r\n")
        body.append("Content-Type: application/octet-stream\r\n\r\n")
        body.append(fileData)
        body.append("\r\n")

        // Domain field (if provided)
        if let domain {
            body.append("--\(boundary)\r\n")
            body.append("Content-Disposition: form-data; name=\"domain\"\r\n\r\n")
            body.append(domain)
            body.append("\r\n")
        }

        body.append("--\(boundary)--\r\n")

        let delegate = UploadProgressDelegate(progressHandler: progress)
        let session = URLSession(configuration: .default, delegate: delegate, delegateQueue: nil)
        defer { session.invalidateAndCancel() }

        let (data, response) = try await session.upload(for: request, from: body)
        let apiResponse: APIResponse<UploadFileResponse> = try handleResponse(data: data, response: response)
        guard let fileResponse = apiResponse.data else {
            throw APIError.decodingError(String(localized: "Missing response data"))
        }
        return fileResponse
    }

    // MARK: - Validation (test API key by calling GET domains)

    func validateAPIKey() async throws -> [String] {
        let response: APIResponse<DomainsResponse> = try await self.request(.getDomains)
        return response.data?.domains ?? []
    }

    // MARK: - Private

    private func performRequest(_ request: URLRequest) async throws -> (Data, URLResponse) {
        do {
            return try await URLSession.shared.data(for: request)
        } catch {
            throw APIError.networkError(error.localizedDescription)
        }
    }

    private func handleResponse<T: Decodable>(data: Data, response: URLResponse) throws -> APIResponse<T> {
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.networkError(String(localized: "Invalid response"))
        }

        if httpResponse.statusCode == 401 {
            throw APIError.unauthorized
        }

        do {
            let apiResponse = try decoder.decode(APIResponse<T>.self, from: data)

            switch apiResponse.code {
            case 200:
                return apiResponse
            case 400:
                throw APIError.badRequest(apiResponse.message ?? String(localized: "Bad request"))
            case 401:
                throw APIError.unauthorized
            case 500:
                throw APIError.serverError
            default:
                throw APIError.unexpectedStatusCode(apiResponse.code, apiResponse.message)
            }
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.decodingError(error.localizedDescription)
        }
    }
}

// MARK: - Upload Progress Delegate

final class UploadProgressDelegate: NSObject, URLSessionTaskDelegate, URLSessionDataDelegate, Sendable {
    private let progressHandler: @Sendable (Double) -> Void
    private let lastReportedProgress = OSAllocatedUnfairLock(initialState: 0.0)

    init(progressHandler: @Sendable @escaping (Double) -> Void) {
        self.progressHandler = progressHandler
    }

    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        didSendBodyData bytesSent: Int64,
        totalBytesSent: Int64,
        totalBytesExpectedToSend: Int64
    ) {
        let progress = Double(totalBytesSent) / Double(totalBytesExpectedToSend)
        // Throttle updates: only report when progress changes by >= 1% or reaches 100%
        let shouldReport = lastReportedProgress.withLock { last -> Bool in
            if progress >= 1.0 || progress - last >= 0.01 {
                last = progress
                return true
            }
            return false
        }
        if shouldReport {
            progressHandler(progress)
        }
    }
}

// MARK: - Data Helper

private extension Data {
    mutating func append(_ string: String) {
        if let data = string.data(using: .utf8) {
            append(data)
        }
    }
}
