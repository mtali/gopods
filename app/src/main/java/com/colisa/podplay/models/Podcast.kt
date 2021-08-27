package com.colisa.podplay.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Podcast(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    var feedUrl: String = "",
    var feedTitle: String = "",
    var feedDescription: String = "",
    var imageUrl: String = "",
    var lastUpdated: Date = Date(),
    @Ignore var episodes: List<Episode> = listOf()
)