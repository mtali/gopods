package com.colisa.podplay.features.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colisa.podplay.R
import com.colisa.podplay.core.models.Podcast
import com.colisa.podplay.core.ui.components.AppError
import com.colisa.podplay.core.ui.components.AppLoading
import com.colisa.podplay.core.ui.components.EmptySubscriptions
import com.colisa.podplay.core.ui.components.PodcastRow

@Composable
fun LibraryRoute(
  onPodcastClick: (feedUrl: String) -> Unit,
  onDiscoverClick: () -> Unit,
  onSettingsClick: () -> Unit,
  viewModel: LibraryViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  LibraryScreen(
    uiState = uiState,
    onPodcastClick = onPodcastClick,
    onDiscoverClick = onDiscoverClick,
    onSettingsClick = onSettingsClick,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
  uiState: LibraryUiState,
  onPodcastClick: (feedUrl: String) -> Unit,
  onDiscoverClick: () -> Unit,
  onSettingsClick: () -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.library)) },
        actions = {
          IconButton(onClick = onSettingsClick) {
            Icon(
              imageVector = Icons.Outlined.Settings,
              contentDescription = stringResource(R.string.settings),
            )
          }
        },
      )
    },
  ) { padding ->
    when (uiState) {
      LibraryUiState.Loading -> AppLoading(Modifier.padding(padding))

      LibraryUiState.Empty -> EmptySubscriptions(
        modifier = Modifier.padding(padding),
        onDiscover = onDiscoverClick,
      )

      is LibraryUiState.Error -> AppError(
        message = uiState.message,
        modifier = Modifier.padding(padding),
      )

      is LibraryUiState.Success -> PodcastList(
        podcasts = uiState.podcasts,
        onPodcastClick = onPodcastClick,
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
      )
    }
  }
}

@Composable
private fun PodcastList(
  podcasts: List<Podcast>,
  onPodcastClick: (feedUrl: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(modifier = modifier) {
    items(items = podcasts, key = { it.feedUrl }) { podcast ->
      PodcastRow(
        podcast = podcast,
        onClick = { onPodcastClick(podcast.feedUrl) },
        modifier = Modifier.animateItem(),
      )
      HorizontalDivider()
    }
  }
}
