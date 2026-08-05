package com.colisa.podplay.features.podcast_details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colisa.podplay.R
import com.colisa.podplay.core.ui.components.AppError
import com.colisa.podplay.core.ui.components.AppLoading
import com.colisa.podplay.core.ui.components.ExpandableText
import com.colisa.podplay.core.ui.components.PodcastArtwork

@Composable
fun PodcastDetailsRoute(
  onBackClick: () -> Unit,
  viewModel: PodcastDetailsViewModel,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
  PodcastDetailsScreen(
    uiState = uiState,
    isRefreshing = isRefreshing,
    onBackClick = onBackClick,
    onRefresh = viewModel::onRefresh,
    onToggleSubscribe = viewModel::onToggleSubscribe,
    onPlayEpisode = viewModel::onPlayEpisode,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailsScreen(
  uiState: PodcastDetailsUiState,
  isRefreshing: Boolean,
  onBackClick: () -> Unit,
  onRefresh: () -> Unit,
  onToggleSubscribe: () -> Unit,
  onPlayEpisode: (EpisodeUi) -> Unit,
) {
  val podcast = (uiState as? PodcastDetailsUiState.Success)?.podcast

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = podcast?.title ?: stringResource(R.string.podcast),
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
        actions = {
          if (podcast != null) {
            IconButton(onClick = onToggleSubscribe) {
              Icon(
                imageVector = if (podcast.subscribed) {
                  Icons.Filled.Bookmark
                } else {
                  Icons.Outlined.BookmarkBorder
                },
                contentDescription = stringResource(
                  if (podcast.subscribed) R.string.unsubscribe else R.string.subscribe
                ),
              )
            }
          }
        },
      )
    },
  ) { padding ->
    when (uiState) {
      PodcastDetailsUiState.Loading -> AppLoading(Modifier.padding(padding))

      is PodcastDetailsUiState.Error -> AppError(
        message = uiState.message,
        onRetry = onRefresh,
        modifier = Modifier.padding(padding),
      )

      is PodcastDetailsUiState.Success -> PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
      ) {
        EpisodeList(
          podcast = uiState.podcast,
          onPlayEpisode = onPlayEpisode,
        )
      }
    }
  }
}

@Composable
private fun EpisodeList(
  podcast: PodcastDetailsUi,
  onPlayEpisode: (EpisodeUi) -> Unit,
) {
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    item {
      PodcastHeader(podcast)
      HorizontalDivider()
    }

    if (podcast.episodes.isEmpty()) {
      item {
        Text(
          text = stringResource(R.string.no_episodes),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(24.dp),
        )
      }
    }

    items(items = podcast.episodes, key = { it.guid }) { episode ->
      EpisodeRow(episode = episode, onClick = { onPlayEpisode(episode) })
      HorizontalDivider()
    }
  }
}

@Composable
private fun PodcastHeader(podcast: PodcastDetailsUi) {
  Column(modifier = Modifier.padding(16.dp)) {
    Row(verticalAlignment = Alignment.Top) {
      PodcastArtwork(
        imageUrl = podcast.imageUrlLarge.ifBlank { podcast.imageUrl },
        thumbnailUrl = podcast.imageUrl,
        size = 96.dp,
      )
      Spacer(Modifier.padding(horizontal = 8.dp))
      Text(
        text = podcast.title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.weight(1f),
      )
    }
    if (podcast.description.isNotBlank()) {
      Spacer(Modifier.height(12.dp))
      ExpandableText(text = podcast.description)
    }
  }
}

@Composable
private fun EpisodeRow(episode: EpisodeUi, onClick: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    Text(
      text = episode.title,
      style = MaterialTheme.typography.bodyLarge,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(4.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = episode.releaseDate,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = episode.duration,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
