@file:Suppress("MemberVisibilityCanBePrivate")

package com.colisa.podplay

object GoConstants {
    // Fragment tags
    const val DETAILS_FRAGMENT_TAG = "DETAILS_FRAGMENT"
    const val ERROR_FRAGMENT_TAG = "ERROR_FRAGMENT"

    // Active fragments
    const val PODCASTS_TAB = "PODCASTS_TAB"
    const val SEARCH_TAB = "SEARCH_TAB"
    const val SETTINGS_TAB = "SETTINGS_TAB"
    const val EPISODES_TAB = "EPISODES_TAB"

    val DEFAULT_ACTIVE_FRAGMENTS =
        listOf(PODCASTS_TAB, SEARCH_TAB, EPISODES_TAB, SETTINGS_TAB)


    // Notification
    const val NOTIFICATION_CHANNEL_ID = "CHANNEL_ID_GO"
    const val NOTIFICATION_INTENT_REQUEST_CODE = 100
    const val NOTIFICATION_ID = 101
    const val FAST_FORWARD_ACTION = "FAST_FORWARD_GO"
    const val PREV_ACTION = "PREV_GO"
    const val PLAY_PAUSE_ACTION = "PLAY_PAUSE_GO"
    const val NEXT_ACTION = "NEXT_GO"
    const val REWIND_ACTION = "REWIND_GO"
    const val REPEAT_ACTION = "REPEAT_GO"
    const val CLOSE_ACTION = "CLOSE_GO"
}