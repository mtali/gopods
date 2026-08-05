package com.colisa.podplay.features.discover

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colisa.podplay.R
import com.colisa.podplay.core.models.Podcast
import com.colisa.podplay.core.ui.components.AppError
import com.colisa.podplay.core.ui.components.AppLoading
import com.colisa.podplay.core.ui.components.AppMessage
import com.colisa.podplay.core.ui.components.PodcastRow
import androidx.compose.runtime.remember

@Composable
fun DiscoverRoute(
  onPodcastClick: (feedUrl: String) -> Unit,
  viewModel: DiscoverViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  DiscoverScreen(
    query = viewModel.query,
    uiState = uiState,
    onQueryChange = viewModel::onQueryChange,
    onSearch = viewModel::onSearch,
    onClearQuery = viewModel::onClearQuery,
    onRetry = viewModel::onRetry,
    onPodcastClick = onPodcastClick,
  )
}

@Composable
fun DiscoverScreen(
  query: String,
  uiState: DiscoverUiState,
  onQueryChange: (String) -> Unit,
  onSearch: () -> Unit,
  onClearQuery: () -> Unit,
  onRetry: () -> Unit,
  onPodcastClick: (feedUrl: String) -> Unit,
) {
  val keyboard = LocalSoftwareKeyboardController.current
  val focusRequester = remember { FocusRequester() }

  Scaffold(
    topBar = {
      OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .focusRequester(focusRequester),
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
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
          onSearch = {
            onSearch()
            keyboard?.hide()
          },
        ),
      )
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
      HorizontalDivider()
    }
  }
}
