package s.how.see.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response
import s.how.see.data.local.preferences.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = secureStorage.getApiKey()
        val request = if (apiKey != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", apiKey)
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
