package com.colisa.podplay.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.colisa.podplay.R
import com.colisa.podplay.core.ui.theme.GoPodsTheme

@Composable
fun AppLoading(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CircularProgressIndicator()
  }
}

fun LazyListScope.appLoading(modifier: Modifier = Modifier) {
  item { AppLoading(modifier = modifier) }
}

@Composable
fun AppMessage(
  icon: ImageVector,
  title: String,
  modifier: Modifier = Modifier,
  message: String? = null,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(48.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      textAlign = TextAlign.Center,
    )
    if (message != null) {
      Spacer(Modifier.height(4.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
    if (actionLabel != null && onAction != null) {
      Spacer(Modifier.height(16.dp))
      OutlinedButton(onClick = onAction) { Text(actionLabel) }
    }
  }
}

@Composable
fun AppError(
  message: String?,
  modifier: Modifier = Modifier,
  onRetry: (() -> Unit)? = null,
) {
  AppMessage(
    icon = Icons.Outlined.CloudOff,
    title = stringResource(R.string.error_generic_title),
    message = message ?: stringResource(R.string.generic_error),
    actionLabel = onRetry?.let { stringResource(R.string.action_retry) },
    onAction = onRetry,
    modifier = modifier,
  )
}

@Composable
fun EmptySubscriptions(modifier: Modifier = Modifier, onDiscover: () -> Unit) {
  AppMessage(
    icon = Icons.Outlined.Search,
    title = stringResource(R.string.no_podcasts_message),
    actionLabel = stringResource(R.string.no_podcasts_action),
    onAction = onDiscover,
    modifier = modifier,
  )
}

@Preview
@Composable
private fun AppMessagePreview() {
  GoPodsTheme {
    AppMessage(
      icon = Icons.Outlined.CloudOff,
      title = "Something went wrong",
      message = "Check your connection and try again",
      actionLabel = "Retry",
      onAction = {},
    )
  }
}
