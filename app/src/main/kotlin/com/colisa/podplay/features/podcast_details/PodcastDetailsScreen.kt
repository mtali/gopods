package com.colisa.podplay.features.podcast_details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colisa.podplay.R
import com.colisa.podplay.core.ui.components.AppError
import com.colisa.podplay.core.ui.components.AppLoading
import com.colisa.podplay.core.ui.components.AppOffline
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
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        // The hero below carries the title, so the bar only names it once scrolled.
        title = {
          Text(
            text = podcast?.title.orEmpty(),
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
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
      )
    },
  ) { padding ->
    when (uiState) {
      PodcastDetailsUiState.Loading -> AppLoading(Modifier.padding(padding))

      PodcastDetailsUiState.Offline -> AppOffline(
        modifier = Modifier.padding(padding),
        onRetry = onRefresh,
      )

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
          loadingEpisodes = uiState.loadingEpisodes,
          onToggleSubscribe = onToggleSubscribe,
          onPlayEpisode = onPlayEpisode,
        )
      }
    }
  }
}

@Composable
private fun EpisodeList(
  podcast: PodcastDetailsUi,
  loadingEpisodes: Boolean,
  onToggleSubscribe: () -> Unit,
  onPlayEpisode: (EpisodeUi) -> Unit,
) {
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    item {
      PodcastHeader(podcast = podcast, onToggleSubscribe = onToggleSubscribe)
    }

    if (podcast.episodes.isNotEmpty()) {
      item {
        Text(
          text = stringResource(R.string.episode_count, podcast.episodes.size),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        )
      }
    } else if (loadingEpisodes) {
      item { AppLoading() }
    } else {
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
    }
  }
}

@Composable
private fun PodcastHeader(podcast: PodcastDetailsUi, onToggleSubscribe: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    PodcastArtwork(
      imageUrl = podcast.imageUrlLarge.ifBlank { podcast.imageUrl },
      thumbnailUrl = podcast.imageUrl,
      size = 168.dp,
    )
    Spacer(Modifier.height(16.dp))
    Text(
      text = podcast.title,
      style = MaterialTheme.typography.headlineSmall,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))

    // Subscribing is the main action here, so it gets a button rather than an icon
    // hidden in the app bar.
    if (podcast.subscribed) {
      OutlinedButton(onClick = onToggleSubscribe) {
        Icon(imageVector = Icons.Filled.Check, contentDescription = null)
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(stringResource(R.string.subscribed))
      }
    } else {
      Button(onClick = onToggleSubscribe) {
        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(stringResource(R.string.subscribe))
      }
    }

    if (podcast.description.isNotBlank()) {
      Spacer(Modifier.height(16.dp))
      ExpandableText(text = podcast.description)
    }
    Spacer(Modifier.height(8.dp))
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
    // Date first as an overline, so scanning down the list reads as a timeline.
    Text(
      text = episode.releaseDate.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(4.dp))
    Text(
      text = episode.title,
      style = MaterialTheme.typography.titleSmall,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    if (episode.description.isNotBlank()) {
      Spacer(Modifier.height(4.dp))
      Text(
        text = episode.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(Modifier.height(10.dp))
    // Play and length together: one obvious target that also says how long it is.
    FilledTonalButton(
      onClick = onClick,
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
      modifier = Modifier.height(34.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.PlayArrow,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      if (episode.duration.isNotBlank()) {
        Spacer(Modifier.width(6.dp))
        Text(text = episode.duration, style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}
