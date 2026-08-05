package com.colisa.podplay.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.colisa.podplay.core.common.utils.DateUtils
import com.colisa.podplay.core.models.Podcast

@Composable
fun PodcastRow(
  podcast: Podcast,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ListItem(
    modifier = modifier.clickable(onClick = onClick),
    headlineContent = {
      Text(
        text = podcast.feedTitle,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    },
    supportingContent = {
      Text(
        text = DateUtils.dateToShortDate(podcast.lastUpdated),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    leadingContent = {
      PodcastArtwork(
        imageUrl = podcast.imageUrl,
        size = 56.dp,
        modifier = Modifier.padding(vertical = 4.dp),
      )
    },
  )
}
