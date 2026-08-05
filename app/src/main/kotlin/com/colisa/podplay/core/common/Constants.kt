package com.colisa.podplay.core.common

import kotlinx.serialization.json.Json

object JsonProvider {
  val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    explicitNulls = false
  }
}

const val DATABASE_NAME = "GoDatabase"

const val ITUNES_BASE_URL = "https://itunes.apple.com/"
