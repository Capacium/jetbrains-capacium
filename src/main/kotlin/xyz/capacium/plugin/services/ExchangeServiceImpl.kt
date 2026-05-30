package xyz.capacium.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.doubleOrNull

private val LOG = logger<ExchangeServiceImpl>()

// ---------------------------------------------------------------------------
// Data models
// ---------------------------------------------------------------------------

data class CapabilityListing(
    val canonicalName: String,
    val kind: String,
    val shortDescription: String,
    val trustState: String,
    val qualityScore: Double?,
    val installCount: Int,
    val githubStars: Int,
    val version: String,
    val tags: List<String> = emptyList(),
    val mcpToolCount: Int = 0,
)

data class SearchResults(
    val results: List<CapabilityListing>,
    val total: Int,
)

// ---------------------------------------------------------------------------
// Service interface
// ---------------------------------------------------------------------------

interface ExchangeService {
    fun search(query: String, kind: String? = null, limit: Int = 30): SearchResults
    fun getCapability(owner: String, name: String): CapabilityListing?
    var exchangeUrl: String
}

// ---------------------------------------------------------------------------
// Implementation
// ---------------------------------------------------------------------------

@Service(Service.Level.APP)
class ExchangeServiceImpl : ExchangeService {

    override var exchangeUrl: String = "https://api.capacium.xyz"

    private val json = Json { ignoreUnknownKeys = true }

    override fun search(query: String, kind: String?, limit: Int): SearchResults {
        val params = buildString {
            append("q=").append(URL(query).toURI().rawPath.ifBlank { query })
            if (kind != null) append("&kind=").append(kind)
            append("&limit=").append(limit)
        }
        val url = "$exchangeUrl/v2/search?${encodeQuery(query, kind, limit)}"
        return try {
            val body = httpGet(url) ?: return SearchResults(emptyList(), 0)
            val obj = json.parseToJsonElement(body).jsonObject
            val items = (obj["results"] ?: obj["items"])?.jsonArray ?: return SearchResults(emptyList(), 0)
            val total = obj["total"]?.jsonPrimitive?.intOrNull ?: items.size
            SearchResults(
                results = items.map { el -> parseListing(el.jsonObject) },
                total = total,
            )
        } catch (e: Exception) {
            LOG.warn("Exchange search failed: ${e.message}")
            SearchResults(emptyList(), 0)
        }
    }

    override fun getCapability(owner: String, name: String): CapabilityListing? {
        return try {
            val body = httpGet("$exchangeUrl/v2/capabilities/$owner/$name") ?: return null
            parseListing(json.parseToJsonElement(body).jsonObject)
        } catch (e: Exception) {
            LOG.warn("Exchange getCapability failed: ${e.message}")
            null
        }
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────

    private fun httpGet(url: String): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "jetbrains-capacium/0.1.0")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            LOG.debug("HTTP error for $url: ${e.message}")
            null
        }
    }

    private fun encodeQuery(query: String, kind: String?, limit: Int): String {
        val sb = StringBuilder()
        sb.append("q=").append(java.net.URLEncoder.encode(query, "UTF-8"))
        if (kind != null) sb.append("&kind=").append(java.net.URLEncoder.encode(kind, "UTF-8"))
        sb.append("&limit=").append(limit)
        return sb.toString()
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private fun parseListing(obj: JsonObject): CapabilityListing {
        val mcpTools = obj["mcp_tools"]?.jsonArray?.size ?: 0
        return CapabilityListing(
            canonicalName = obj["canonical_name"]?.jsonPrimitive?.content ?: "",
            kind = obj["kind"]?.jsonPrimitive?.content ?: "skill",
            shortDescription = obj["short_description"]?.jsonPrimitive?.content ?: "",
            trustState = obj["trust_state"]?.jsonPrimitive?.content ?: "discovered",
            qualityScore = obj["quality_score"]?.jsonPrimitive?.doubleOrNull,
            installCount = obj["install_count"]?.jsonPrimitive?.intOrNull ?: 0,
            githubStars = obj["github_stars"]?.jsonPrimitive?.intOrNull ?: 0,
            version = obj["version"]?.jsonPrimitive?.content ?: "0.0.0",
            tags = obj["tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            mcpToolCount = mcpTools,
        )
    }

    companion object {
        @JvmStatic
        fun getInstance(): ExchangeService = service<ExchangeService>()
    }
}
