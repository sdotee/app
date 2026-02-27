package s.how.see.util

import android.util.Patterns

object UrlValidator {
    fun isValidUrl(url: String): Boolean {
        return url.isNotBlank() && Patterns.WEB_URL.matcher(url).matches()
    }
}
