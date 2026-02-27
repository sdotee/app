package s.how.see.data.remote

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import s.how.see.data.local.preferences.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val appPreferences: AppPreferences,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val currentBaseUrl = runBlocking { appPreferences.baseUrl.first() }
            .toHttpUrlOrNull() ?: return chain.proceed(originalRequest)

        val originalUrl = originalRequest.url

        // Get the placeholder base URL path segments (e.g., ["api", "v1"])
        val placeholderBase = "https://s.ee/api/v1/".toHttpUrlOrNull()!!
        val placeholderSegments = placeholderBase.pathSegments.filter { it.isNotEmpty() }

        // Get the original request path segments
        val originalSegments = originalUrl.pathSegments.filter { it.isNotEmpty() }

        // Remove placeholder prefix to get the relative path
        val relativeSegments = if (originalSegments.size >= placeholderSegments.size &&
            originalSegments.take(placeholderSegments.size) == placeholderSegments
        ) {
            originalSegments.drop(placeholderSegments.size)
        } else {
            originalSegments
        }

        // Build new URL with the dynamic base URL + relative path
        val baseSegments = currentBaseUrl.pathSegments.filter { it.isNotEmpty() }
        val allSegments = baseSegments + relativeSegments

        val newUrlBuilder = originalUrl.newBuilder()
            .scheme(currentBaseUrl.scheme)
            .host(currentBaseUrl.host)
            .port(currentBaseUrl.port)

        // Clear existing path and set new segments
        // encodedPath replaces the entire path
        val newPath = "/" + allSegments.joinToString("/")
        newUrlBuilder.encodedPath(newPath)

        // Preserve query parameters
        val newUrl = newUrlBuilder.build()

        return chain.proceed(
            originalRequest.newBuilder().url(newUrl).build()
        )
    }
}
