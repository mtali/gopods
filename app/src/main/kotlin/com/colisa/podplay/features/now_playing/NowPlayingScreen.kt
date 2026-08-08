package com.colisa.podplay.features.now_playing

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colisa.podplay.R
import com.colisa.podplay.core.player.PlayerUiState
import com.colisa.podplay.core.ui.components.PodcastArtwork

@Composable
fun NowPlayingRoute(
  onBackClick: () -> Unit,
  onPodcastClick: (String) -> Unit,
  viewModel: NowPlayingViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  NowPlayingScreen(
    uiState = uiState,
    onBackClick = onBackClick,
    onPodcastClick = onPodcastClick,
    onPlayPause = viewModel::onPlayPause,
    onSeekBack = viewModel::onSeekBack,
    onSeekForward = viewModel::onSeekForward,
    onSeekTo = viewModel::onSeekTo,
    onCycleSpeed = viewModel::onCycleSpeed,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
  uiState: PlayerUiState,
  onBackClick: () -> Unit,
  onPodcastClick: (String) -> Unit,
  onPlayPause: () -> Unit,
  onSeekBack: () -> Unit,
  onSeekForward: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onCycleSpeed: () -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.now_playing),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.back),
            )
          }
        },
      )
    },
  ) { padding ->
    val episode = uiState.episode
    // Scrollable, so the notes below the controls are reachable.
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.height(16.dp))

      val podcastTitle = episode?.podcastTitle.orEmpty()
      val feedUrl = episode?.feedUrl.orEmpty()
      // Artwork and title open the podcast too, not just the name below them.
      val openPodcast = Modifier.clickable(enabled = feedUrl.isNotBlank()) {
        onPodcastClick(feedUrl)
      }

      Surface(
        shape = MaterialTheme.shapes.large,
        shadowElevation = 12.dp,
        modifier = openPodcast,
      ) {
        PodcastArtwork(
          imageUrl = episode?.artUrl600?.ifBlank { episode.artUrl },
          thumbnailUrl = episode?.artUrl,
          size = 260.dp,
        )
      }

      Spacer(Modifier.height(28.dp))

      Text(
        text = episode?.title.orEmpty(),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = openPodcast,
      )

      if (podcastTitle.isNotBlank()) {
        // An episode stored before the feed url was tracked has nowhere to go.
        if (feedUrl.isBlank()) {
          Spacer(Modifier.height(8.dp))
          Text(
            text = podcastTitle,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        } else {
          TextButton(onClick = { onPodcastClick(feedUrl) }) {
            Text(
              text = podcastTitle,
              style = MaterialTheme.typography.titleSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      if (uiState.isBuffering) {
        Spacer(Modifier.height(8.dp))
        Text(
          text = stringResource(R.string.buffering),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Spacer(Modifier.height(24.dp))

      PositionSlider(
        positionMs = uiState.positionMs,
        durationMs = uiState.durationMs,
        onSeekTo = onSeekTo,
      )

      Spacer(Modifier.height(12.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        FilledTonalIconButton(onClick = onSeekBack, modifier = Modifier.size(56.dp)) {
          Icon(
            imageVector = Icons.Filled.FastRewind,
            contentDescription = stringResource(R.string.content_fast_rewind),
            modifier = Modifier.size(28.dp),
          )
        }
        FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(84.dp)) {
          Icon(
            imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = stringResource(
              if (uiState.isPlaying) R.string.pause else R.string.play
            ),
            modifier = Modifier.size(40.dp),
          )
        }
        FilledTonalIconButton(onClick = onSeekForward, modifier = Modifier.size(56.dp)) {
          Icon(
            imageVector = Icons.Filled.FastForward,
            contentDescription = stringResource(R.string.content_fast_forward),
            modifier = Modifier.size(28.dp),
          )
        }
      }

      Spacer(Modifier.height(4.dp))

      TextButton(onClick = onCycleSpeed) {
        Text(
          text = stringResource(
            R.string.playback_speed,
            uiState.speed.toString().removeSuffix(".0"),
          ),
          style = MaterialTheme.typography.labelLarge,
        )
      }

      if (!episode?.description.isNullOrBlank()) {
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        ShowNotes(description = episode.description)
      }

      Spacer(Modifier.height(32.dp))
    }
  }
}

@Composable
private fun ShowNotes(description: String) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 20.dp),
  ) {
    Text(
      text = stringResource(R.string.show_notes),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun PositionSlider(
  positionMs: Long,
  durationMs: Long,
  onSeekTo: (Long) -> Unit,
) {
  // While dragging, the thumb follows the finger instead of the player position.
  var dragValue by remember { mutableStateOf<Float?>(null) }
  val max = durationMs.coerceAtLeast(1L).toFloat()
  val value = dragValue ?: positionMs.coerceIn(0, durationMs.coerceAtLeast(0)).toFloat()
  val remaining = ((durationMs - value.toLong()).coerceAtLeast(0)) / 1000

  Column(modifier = Modifier.fillMaxWidth()) {
    Slider(
      value = value.coerceIn(0f, max),
      valueRange = 0f..max,
      onValueChange = { dragValue = it },
      onValueChangeFinished = {
        dragValue?.let { onSeekTo(it.toLong()) }
        dragValue = null
      },
      enabled = durationMs > 0,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = DateUtils.formatElapsedTime(value.toLong() / 1000),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      // Time left is what you want to know mid episode, not the total.
      Text(
        text = stringResource(R.string.time_remaining, DateUtils.formatElapsedTime(remaining)),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
