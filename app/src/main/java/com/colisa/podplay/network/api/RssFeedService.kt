package com.colisa.podplay.network.api

import com.colisa.podplay.network.models.RssFeedResponse
import com.colisa.podplay.util.DateUtils
import okhttp3.*
import org.w3c.dom.Node
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.coroutines.suspendCoroutine


class RssFeedService : FeedService {
    override suspend fun getFeed(feedUrl: String): RssFeedResponse = suspendCoroutine { c ->
        val client = OkHttpClient()
        val request = Request.Builder().url(feedUrl).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                c.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful.not()) {
                        c.resumeWith(Result.failure(IOException("Unexpected code ${response.code()}")))
                    } else {
                        val body = response.body()
                        if (body != null) {
                            val dbFactory = DocumentBuilderFactory.newInstance()
                            val dBuilder = dbFactory.newDocumentBuilder()
                            val doc = dBuilder.parse(body.byteStream())
                            val feed = RssFeedResponse()
                            domToRssFeedResponse(doc, feed)
                            c.resumeWith(Result.success(feed))
                        } else {
                            c.resumeWith(Result.failure(IOException("Empty response")))
                        }
                    }
                }
            }
        })
    }

    private fun domToRssFeedResponse(node: Node, rssFeedResponse: RssFeedResponse) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            val nodeName = node.nodeName
            val parentName = node.parentNode.nodeName
            val grannyName = node.parentNode?.parentNode?.nodeName ?: ""
            if (parentName == "item" && grannyName == "channel") {
                val currentEpisode = rssFeedResponse.episodes?.last()
                currentEpisode?.let {
                    when (nodeName) {
                        "title" -> it.title = node.textContent
                        "description" -> it.description = node.textContent
                        "itunes:duration" -> it.duration = node.textContent
                        "guid" -> it.guid = node.textContent
                        "pubDate" -> it.pubDate = node.textContent
                        "link" -> it.link = node.textContent
                        "enclosure" -> {
                            it.url = node.attributes.getNamedItem("url").textContent
                            it.type = node.attributes.getNamedItem("type")?.textContent
                        }
                    }
                }
            }
            if (parentName == "channel") {
                when (nodeName) {
                    "title" -> rssFeedResponse.title = node.textContent
                    "description" -> rssFeedResponse.description = node.textContent
                    "itunes:summary" -> rssFeedResponse.summary = node.textContent
                    "item" -> rssFeedResponse.episodes?.add(RssFeedResponse.EpisodeResponse())
                    "pubDate" -> rssFeedResponse.lastUpdated =
                        DateUtils.xmlDateToDate(node.textContent)
                }
            }
        }
        val nodes = node.childNodes
        for (i in 0 until nodes.length) {
            val childNode = nodes.item(i)
            domToRssFeedResponse(childNode, rssFeedResponse)
        }
    }


}


interface FeedService {
    suspend fun getFeed(feedUrl: String): RssFeedResponse

    companion object {
        val instance: RssFeedService by lazy { RssFeedService() }
    }
}

