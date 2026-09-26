package com.aravind.wavve.data.piped

import android.util.Log
import com.aravind.wavve.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the current list of live Piped API instances.
 *
 * On refresh(), fetches the up-to-date instance list from Piped's own instances
 * endpoint. If that fails (no network, endpoint down, malformed response), falls
 * back to whatever list is already cached — or Constants.PIPED_INSTANCES on first
 * ever launch. This mirrors what Piped's own frontend does, since hardcoded
 * instance lists go stale as instances shut down or get blocked.
 */
@Singleton
class PipedInstanceProvider @Inject constructor() {

    @Volatile
    private var cachedInstances: List<String> = Constants.PIPED_INSTANCES

    // Small dedicated client for this one-off call — avoids depending on the
    // app's main OkHttpClient (which carries the failover interceptor that
    // uses THIS class, so pulling that client in here would be circular).
    private val bootstrapClient = OkHttpClient.Builder().build()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(Constants.PIPED_INSTANCES_LIST_URL)
                .build()

            bootstrapClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext
                val body = response.body?.string() ?: return@withContext

                val json = JSONArray(body)
                val urls = mutableListOf<String>()
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    val apiUrl = obj.optString("api_url").trimEnd('/')
                    if (apiUrl.isNotBlank()) urls.add(apiUrl)
                }

                if (urls.isNotEmpty()) {
                    cachedInstances = urls
                    Log.d("PipedInstanceProvider", "Refreshed: ${urls.size} live instances")
                }
            }
        } catch (e: Exception) {
            Log.w("PipedInstanceProvider", "Instance list refresh failed, using cached/fallback list", e)
            // cachedInstances stays as-is (previous successful fetch, or Constants fallback)
        }
    }

    fun getInstances(): List<String> = cachedInstances
}