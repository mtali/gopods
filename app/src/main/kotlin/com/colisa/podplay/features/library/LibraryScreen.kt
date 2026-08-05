package com.colisa.podplay.features.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colisa.podplay.R
import com.colisa.podplay.core.models.Podcast
import com.colisa.podplay.core.ui.components.AppError
import com.colisa.podplay.core.ui.components.AppLoading
import com.colisa.podplay.core.ui.components.EmptySubscriptions
import com.colisa.podplay.core.ui.components.PodcastGridItem

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

      is LibraryUiState.Success -> PodcastGrid(
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
private fun PodcastGrid(
  podcasts: List<Podcast>,
  onPodcastClick: (feedUrl: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  // Adaptive so a tablet or landscape gets more columns for free.
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 156.dp),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    modifier = modifier,
  ) {
    items(items = podcasts, key = { it.feedUrl }) { podcast ->
      PodcastGridItem(
        podcast = podcast,
        onClick = { onPodcastClick(podcast.feedUrl) },
        modifier = Modifier.animateItem(),
      )
    }
  }
}
