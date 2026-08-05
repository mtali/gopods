package com.colisa.podplay.features.now_playing

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
  viewModel: NowPlayingViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  NowPlayingScreen(
    uiState = uiState,
    onBackClick = onBackClick,
    onPlayPause = viewModel::onPlayPause,
    onSeekBack = viewModel::onSeekBack,
    onSeekForward = viewModel::onSeekForward,
    onSeekTo = viewModel::onSeekTo,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
  uiState: PlayerUiState,
  onBackClick: () -> Unit,
  onPlayPause: () -> Unit,
  onSeekBack: () -> Unit,
  onSeekForward: () -> Unit,
  onSeekTo: (Long) -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.now_playing)) },
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Surface(
        shape = MaterialTheme.shapes.large,
        shadowElevation = 12.dp,
      ) {
        PodcastArtwork(
          imageUrl = episode?.artUrl600?.ifBlank { episode.artUrl },
          thumbnailUrl = episode?.artUrl,
          size = 280.dp,
        )
      }

      Spacer(Modifier.height(40.dp))

      Text(
        text = episode?.title.orEmpty(),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = if (uiState.isBuffering) {
          stringResource(R.string.buffering)
        } else {
          episode?.podcastTitle.orEmpty()
        },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
      )

      Spacer(Modifier.height(32.dp))

      PositionSlider(
        positionMs = uiState.positionMs,
        durationMs = uiState.durationMs,
        onSeekTo = onSeekTo,
      )

      Spacer(Modifier.height(24.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        FilledTonalIconButton(
          onClick = onSeekBack,
          modifier = Modifier.size(56.dp),
        ) {
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
        FilledTonalIconButton(
          onClick = onSeekForward,
          modifier = Modifier.size(56.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.FastForward,
            contentDescription = stringResource(R.string.content_fast_forward),
            modifier = Modifier.size(28.dp),
          )
        }
      }
    }
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
      Text(
        text = DateUtils.formatElapsedTime(durationMs / 1000),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
