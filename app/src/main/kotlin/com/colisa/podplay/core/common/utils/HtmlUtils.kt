package com.colisa.podplay.core.common.utils

import android.text.Html
import android.text.Spanned

object HtmlUtils {

  /** Strips images and newlines out of feed markup before rendering it as text. */
  fun htmlToSpannable(data: String?): Spanned {
    val html = (data ?: "").trim()
    val cleaned = html
      .replace("\n".toRegex(), "")
      .replace("(<(/)img>)|(<img.+?>)".toRegex(), "")
    return Html.fromHtml(cleaned, Html.FROM_HTML_MODE_LEGACY)
  }

  fun htmlToText(data: String?): String = htmlToSpannable(data).toString()
}
