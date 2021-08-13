package com.colisa.podplay.models

import java.util.*

data class Episode(
    var guid: String = "",
    var title: String = "",
    var description: String = "",
    var mediaUrl: String = "",
    var type: String = "",
    var releaseDate: Date = Date(),
    var duration: String = ""
)