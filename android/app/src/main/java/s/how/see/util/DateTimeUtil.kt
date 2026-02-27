package s.how.see.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtil {
    private val displayFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())

    fun formatTimestamp(timestamp: Long): String {
        return displayFormat.format(Date(timestamp))
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }
}
