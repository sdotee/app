package s.how.see.util

import s.how.see.data.local.db.entity.UploadedFileEntity

enum class LinkDisplayType(val label: String) {
    DIRECT_LINK("Direct Link"),
    SHARE_PAGE("Share Page"),
    BBCODE("BBCode"),
    BBCODE_WITH_LINK("BBCode w/ Link"),
    BBCODE_DIRECT_LINK("BBCode w/ Direct Link"),
    HTML("HTML"),
    HTML_WITH_LINK("HTML w/ Link"),
    HTML_DIRECT_LINK("HTML w/ Direct Link"),
    MARKDOWN("Markdown");

    fun formatted(file: UploadedFileEntity): String {
        val url = file.url
        val pageUrl = file.page ?: url
        val filename = file.filename
        val fileType = LinkFormatter.fileType(filename)

        return when (this) {
            DIRECT_LINK -> url
            SHARE_PAGE -> pageUrl
            BBCODE -> LinkFormatter.bbcode(url, filename, fileType)
            BBCODE_WITH_LINK -> LinkFormatter.bbcodeWithLink(url, pageUrl, filename, fileType)
            BBCODE_DIRECT_LINK -> LinkFormatter.bbcodeDirectLink(url, filename, fileType)
            HTML -> LinkFormatter.html(url, filename, fileType)
            HTML_WITH_LINK -> LinkFormatter.htmlWithLink(url, pageUrl, filename, fileType)
            HTML_DIRECT_LINK -> LinkFormatter.htmlDirectLink(url, filename, fileType)
            MARKDOWN -> LinkFormatter.markdown(url, filename, fileType)
        }
    }

    companion object {
        fun fromString(value: String): LinkDisplayType {
            return entries.find { it.name == value } ?: DIRECT_LINK
        }
    }
}

object LinkFormatter {

    enum class FileType { IMAGE, AUDIO, VIDEO, OTHER }

    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "avif", "ico", "tiff",
    )
    private val audioExtensions = setOf(
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma",
    )
    private val videoExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp",
    )

    fun fileType(filename: String): FileType {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return when {
            ext in imageExtensions -> FileType.IMAGE
            ext in audioExtensions -> FileType.AUDIO
            ext in videoExtensions -> FileType.VIDEO
            else -> FileType.OTHER
        }
    }

    // BBCode - plain tags
    fun bbcode(url: String, filename: String, type: FileType): String = when (type) {
        FileType.IMAGE -> "[img]$url[/img]"
        FileType.AUDIO -> "[audio]$url[/audio]"
        FileType.VIDEO -> "[video]$url[/video]"
        FileType.OTHER -> "[url=$url]$filename[/url]"
    }

    // BBCode - wrapped in share page link
    fun bbcodeWithLink(url: String, pageUrl: String, filename: String, type: FileType): String = when (type) {
        FileType.IMAGE -> "[url=$pageUrl][img]$url[/img][/url]"
        FileType.AUDIO -> "[audio]$url[/audio]"
        FileType.VIDEO -> "[video]$url[/video]"
        FileType.OTHER -> "[url=$pageUrl]$filename[/url]"
    }

    // BBCode - wrapped in direct URL link
    fun bbcodeDirectLink(url: String, filename: String, type: FileType): String = when (type) {
        FileType.IMAGE -> "[url=$url][img]$url[/img][/url]"
        FileType.AUDIO -> "[audio]$url[/audio]"
        FileType.VIDEO -> "[video]$url[/video]"
        FileType.OTHER -> "[url=$url]$filename[/url]"
    }

    // HTML - plain tags
    fun html(url: String, filename: String, type: FileType): String = when (type) {
        FileType.IMAGE -> """<img src="$url" alt="$filename" />"""
        FileType.AUDIO -> """<audio src="$url" controls>$filename</audio>"""
        FileType.VIDEO -> """<video src="$url" controls>$filename</video>"""
        FileType.OTHER -> """<a href="$url">$filename</a>"""
    }

    // HTML - wrapped in share page link
    fun htmlWithLink(url: String, pageUrl: String, filename: String, type: FileType): String = when (type) {
        FileType.IMAGE -> """<a href="$pageUrl"><img src="$url" alt="$filename" /></a>"""
        FileType.AUDIO -> """<a href="$pageUrl"><audio src="$url" controls>$filename</audio></a>"""
        FileType.VIDEO -> """<a href="$pageUrl"><video src="$url" controls>$filename</video></a>"""
        FileType.OTHER -> """<a href="$pageUrl">$filename</a>"""
    }

    // HTML - wrapped in direct URL link
    fun htmlDirectLink(url: String, filename: String, type: FileType): String = when (type) {
        FileType.IMAGE -> """<a href="$url"><img src="$url" alt="$filename" /></a>"""
        FileType.AUDIO -> """<a href="$url"><audio src="$url" controls>$filename</audio></a>"""
        FileType.VIDEO -> """<a href="$url"><video src="$url" controls>$filename</video></a>"""
        FileType.OTHER -> """<a href="$url">$filename</a>"""
    }

    // Markdown
    fun markdown(url: String, filename: String, type: FileType): String = when (type) {
        FileType.IMAGE -> "![$filename]($url)"
        else -> "[$filename]($url)"
    }
}
