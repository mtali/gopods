package com.colisa.podplay.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.colisa.podplay.core.common.utils.DateUtils
import com.colisa.podplay.core.models.Podcast

/** Search result row. Larger artwork than a stock ListItem, and no divider. */
@Composable
fun PodcastRow(
  podcast: Podcast,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    PodcastArtwork(imageUrl = podcast.imageUrl, size = 72.dp)
    Spacer(Modifier.padding(horizontal = 8.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = podcast.feedTitle,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = DateUtils.formatRelativeDate(podcast.lastUpdated),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** Library grid cell: artwork leads, title underneath. */
@Composable
fun PodcastGridItem(
  podcast: Podcast,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clickable(onClick = onClick)
      .padding(8.dp),
  ) {
    PodcastArtwork(
      imageUrl = podcast.imageUrl600.ifBlank { podcast.imageUrl },
      thumbnailUrl = podcast.imageUrl,
      modifier = Modifier.fillMaxWidth(),
      fillWidth = true,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = podcast.feedTitle,
      style = MaterialTheme.typography.titleSmall,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = DateUtils.formatRelativeDate(podcast.lastUpdated),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
