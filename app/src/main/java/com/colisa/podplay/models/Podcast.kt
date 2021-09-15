package com.colisa.podplay.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(indices = [Index(value = ["collectionId"], unique = true)])
data class Podcast(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    var collectionId: Long = 0,
    var feedUrl: String = "",
    var feedTitle: String = "",
    var feedDescription: String = "",
    var imageUrl: String = "",
    var imageUrl600: String = "",
    var lastUpdated: Date = Date(),
    var subscribed: Boolean = false,
    @Ignore var episodes: List<Episode> = listOf(),
)