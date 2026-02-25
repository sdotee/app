import Foundation
import SwiftData

@Model
final class UploadedFile {
    var fileID: Int
    var filename: String
    var storename: String
    var size: Int64
    var width: Int?
    var height: Int?
    var url: String
    var page: String
    var path: String
    var deleteHash: String
    var deleteURL: String
    var createdAt: Date

    init(
        fileID: Int,
        filename: String,
        storename: String,
        size: Int64,
        width: Int? = nil,
        height: Int? = nil,
        url: String,
        page: String,
        path: String,
        deleteHash: String,
        deleteURL: String,
        createdAt: Date = .now
    ) {
        self.fileID = fileID
        self.filename = filename
        self.storename = storename
        self.size = size
        self.width = width
        self.height = height
        self.url = url
        self.page = page
        self.path = path
        self.deleteHash = deleteHash
        self.deleteURL = deleteURL
        self.createdAt = createdAt
    }

    var isImage: Bool {
        let ext = (filename as NSString).pathExtension.lowercased()
        return ["jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic"].contains(ext)
    }
}
