package com.colisa.podplay.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.colisa.podplay.R
import com.colisa.podplay.core.player.PlayerUiState

private val ProgressHeight = 3.dp

@Composable
fun MiniPlayer(
  state: PlayerUiState,
  onPlayPause: () -> Unit,
  onOpen: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = state.episode != null,
    enter = slideInVertically { it },
    exit = slideOutVertically { it },
  ) {
    val episode = state.episode ?: return@AnimatedVisibility
    val target = if (state.durationMs > 0) {
      (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
      0f
    }
    val progress by animateFloatAsState(targetValue = target, label = "miniPlayerProgress")

    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      shape = MaterialTheme.shapes.large,
      shadowElevation = 3.dp,
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
      Column {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clickable(onClick = onOpen)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 8.dp),
        ) {
          PodcastArtwork(imageUrl = episode.artUrl, size = 48.dp)
          Column(
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 12.dp),
          ) {
            Text(
              text = episode.title,
              style = MaterialTheme.typography.titleSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = if (state.isBuffering) {
                stringResource(R.string.buffering)
              } else {
                episode.podcastTitle
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          FilledTonalIconButton(onClick = onPlayPause) {
            Icon(
              imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = stringResource(
                if (state.isPlaying) R.string.pause else R.string.play
              ),
            )
          }
        }

        // A plain track hugging the bottom edge. LinearProgressIndicator draws a stop
        // indicator dot and an end gap, which read as artefacts at this size.
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(ProgressHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(progress)
              .height(ProgressHeight)
              .background(MaterialTheme.colorScheme.primary),
          )
        }
      }
    }
  }
}
