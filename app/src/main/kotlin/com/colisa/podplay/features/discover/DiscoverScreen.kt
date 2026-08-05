package com.colisa.podplay.features.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colisa.podplay.R
import com.colisa.podplay.core.models.Podcast
import com.colisa.podplay.core.ui.components.AppError
import com.colisa.podplay.core.ui.components.AppLoading
import com.colisa.podplay.core.ui.components.AppMessage
import com.colisa.podplay.core.ui.components.AppOffline
import com.colisa.podplay.core.ui.components.PodcastRow

@Composable
fun DiscoverRoute(
  onPodcastClick: (feedUrl: String) -> Unit,
  viewModel: DiscoverViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
  DiscoverScreen(
    query = viewModel.query,
    uiState = uiState,
    recentSearches = recentSearches,
    onQueryChange = viewModel::onQueryChange,
    onSearch = viewModel::onSearch,
    onSearchRecent = viewModel::onSearchRecent,
    onClearQuery = viewModel::onClearQuery,
    onRetry = viewModel::onRetry,
    onPodcastClick = onPodcastClick,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
  query: String,
  uiState: DiscoverUiState,
  recentSearches: List<String>,
  onQueryChange: (String) -> Unit,
  onSearch: () -> Unit,
  onSearchRecent: (String) -> Unit,
  onClearQuery: () -> Unit,
  onRetry: () -> Unit,
  onPodcastClick: (feedUrl: String) -> Unit,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }

  Scaffold(
    topBar = {
      // Docked rather than full screen: this screen keeps a bottom bar and a mini
      // player, which a full screen search surface would leave stranded.
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 4.dp),
      ) {
        DockedSearchBar(
          expanded = expanded,
          onExpandedChange = { expanded = it },
          inputField = {
            SearchBarDefaults.InputField(
              query = query,
              onQueryChange = onQueryChange,
              onSearch = {
                onSearch()
                expanded = false
              },
              expanded = expanded,
              onExpandedChange = { expanded = it },
              placeholder = { Text(stringResource(R.string.search_podcasts)) },
              leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
              },
              trailingIcon = {
                if (query.isNotEmpty()) {
                  IconButton(onClick = onClearQuery) {
                    Icon(
                      imageVector = Icons.Filled.Close,
                      contentDescription = stringResource(R.string.clear_search),
                    )
                  }
                }
              },
            )
          },
        ) {
          // Expanded surface: previous searches, newest first.
          RecentSearches(
            terms = recentSearches,
            onSelect = { term ->
              onSearchRecent(term)
              expanded = false
            },
          )
        }
      }
    },
  ) { padding ->
    when (uiState) {
      DiscoverUiState.Idle -> AppMessage(
        icon = Icons.Outlined.Search,
        title = stringResource(R.string.discover_empty_title),
        message = stringResource(R.string.discover_empty_message),
        modifier = Modifier.padding(padding),
      )

      DiscoverUiState.Loading -> AppLoading(Modifier.padding(padding))

      DiscoverUiState.Offline -> AppOffline(
        modifier = Modifier.padding(padding),
        onRetry = onRetry,
      )

      DiscoverUiState.NoResults -> AppMessage(
        icon = Icons.Outlined.Search,
        title = stringResource(R.string.discover_no_results),
        modifier = Modifier.padding(padding),
      )

      is DiscoverUiState.Error -> AppError(
        message = uiState.message,
        onRetry = onRetry,
        modifier = Modifier.padding(padding),
      )

      is DiscoverUiState.Success -> SearchResults(
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
private fun RecentSearches(
  terms: List<String>,
  onSelect: (String) -> Unit,
) {
  if (terms.isEmpty()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.discover_empty_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    return
  }

  LazyColumn {
    item {
      Text(
        text = stringResource(R.string.recent_searches),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
      )
    }
    items(items = terms, key = { it }) { term ->
      ListItem(
        modifier = Modifier.clickable { onSelect(term) },
        headlineContent = { Text(term) },
        leadingContent = {
          Icon(imageVector = Icons.Outlined.History, contentDescription = null)
        },
        // Transparent so the rows read as part of the search bar's card.
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
      )
    }
  }
}

@Composable
private fun SearchResults(
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
    }
  }
}
