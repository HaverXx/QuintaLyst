package com.quintalyst.shared

import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.models.response.SearchResponse
import com.metrolist.innertubex.models.YouTubeClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** iOS-facing facade over the exact InnerTubeX version used by the Android app. */
class QuintalystMusicBridge {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val httpClient = HttpClient(Darwin) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(this@QuintalystMusicBridge.json)
        }
    }
    private val innerTube = InnerTube(httpClient)
    private val configStore = RemotePlayerConfigStore(httpClient, PlayerConfigRepository.disabled())
    private val cipherService = YouTubeCipherService(httpClient, configStore)
    private val extractor = InnerTubeExtractor(
        configParser = YtConfigParserImpl(httpClient, innerTube, configStore),
        cipherService = cipherService,
        innerTube = innerTube,
    )

    @OptIn(ExperimentalSerializationApi::class)
    @Throws(Exception::class)
    suspend fun searchJson(query: String): String {
        val response = innerTube.search(YouTubeClient.WEB_REMIX, query).body<SearchResponse>()
        return json.encodeToString(response)
    }

    @Throws(Exception::class)
    suspend fun libraryJson(): String {
        val response = innerTube.browse(YouTubeClient.WEB_REMIX, browseId = "FEmusic_library_landing")
            .body<com.metrolist.innertubex.models.response.BrowseResponse>()
        return json.encodeToString(response)
    }

    @Throws(Exception::class)
    suspend fun homeJson(): String {
        val response = innerTube.browse(YouTubeClient.WEB_REMIX, browseId = "FEmusic_home")
            .body<com.metrolist.innertubex.models.response.BrowseResponse>()
        return json.encodeToString(response)
    }

    @Throws(Exception::class)
    suspend fun audioStreamJson(videoId: String): String {
        val stream = requireNotNull(
            extractor.extract(videoId, ContentHints(wantVideo = false), audioQuality = AudioQuality.HIGH),
        ) { "No playable audio stream was returned." }
        require(stream.sabrBootstrap == null) { "This audio stream uses a transport not supported by AVPlayer." }
        val url = requireNotNull(stream.audioUrl) { "The stream did not contain a direct audio URL." }
        val headers = buildJsonObject { stream.headers.forEach { (name, value) -> put(name, value) } }
        return buildJsonObject {
            put("url", url)
            put("mimeType", stream.mimeType.orEmpty())
            put("headers", headers)
        }.toString()
    }    fun setSession(cookie: String?, visitorData: String?, dataSyncId: String?, authUser: String) {
        innerTube.replaceSession(
            cookie = cookie,
            visitorData = visitorData,
            dataSyncId = dataSyncId,
            authUser = authUser,
            useLoginForBrowse = true,
        )
    }

    fun clearSession() {
        innerTube.clearSessionIfMatches(innerTube.sessionSnapshot())
    }

    fun close() {
        innerTube.close()
        httpClient.close()
    }
}





