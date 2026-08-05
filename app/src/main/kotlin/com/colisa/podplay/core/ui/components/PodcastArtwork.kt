package com.colisa.podplay.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.colisa.podplay.R

/**
 * Draws a tinted placeholder underneath so a slow or failed load leaves a tile rather
 * than a blank gap. [thumbnailUrl] lets a cached small image show while a larger one
 * is fetched.
 */
@Composable
fun PodcastArtwork(
  imageUrl: String?,
  modifier: Modifier = Modifier,
  size: Dp = 56.dp,
  thumbnailUrl: String? = null,
) {
  val context = LocalContext.current
  Box(
    modifier = modifier
      .size(size)
      .clip(MaterialTheme.shapes.small)
      .background(MaterialTheme.colorScheme.surfaceVariant),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = Icons.Filled.Podcasts,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(size / 2),
    )
    AsyncImage(
      model = ImageRequest.Builder(context)
        .data(imageUrl)
        .crossfade(true)
        .apply { thumbnailUrl?.let { placeholderMemoryCacheKey(it) } }
        .build(),
      contentDescription = stringResource(R.string.podcast_image),
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
  }
}
